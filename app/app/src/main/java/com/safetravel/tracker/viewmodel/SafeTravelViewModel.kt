package com.safetravel.tracker.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.safetravel.tracker.data.local.AppDatabase
import com.safetravel.tracker.data.local.entities.toOffline
import com.safetravel.tracker.data.repository.SafeTravelRepository
import com.safetravel.tracker.supabase.*
import com.safetravel.tracker.util.AddressUtils
import com.safetravel.tracker.util.DateTimeUtils
import com.safetravel.tracker.util.LocationHelper
import com.safetravel.tracker.util.AdaptiveLocationFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

data class EmergencyContact(
    val id: String,
    val name: String,
    val relation: String,
    val phone: String
)

data class GeocodeSuggestion(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class HeadsUpAlert(
    val title: String,
    val message: String,
    val tripId: String,
    val trackingCode: String
)

// --- ENHANCED SAFE TRAVEL VIEWMODEL ---
class SafeTravelViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repository and Local Database
    private val database = AppDatabase.getDatabase(application)
    private val repository = SafeTravelRepository(database.safeTravelDao())

    // Current Session status
    val userProfile = MutableStateFlow<SupabaseProfile?>(null)
    val activeTrip = MutableStateFlow<SupabaseTrip?>(null)
    val pastTrips = MutableStateFlow<List<SupabaseTrip>>(emptyList())
    val sosRecords = MutableStateFlow<List<SupabaseSosRecord>>(emptyList())
    val pointsLogs = MutableStateFlow<List<SupabasePointsLog>>(emptyList())
    val homepageBanners = MutableStateFlow<List<SupabaseHomepageBanner>>(emptyList())
    val safetyNews = MutableStateFlow<List<SupabaseSafetyNews>>(emptyList())
    val policeStations = MutableStateFlow<List<SupabasePoliceStation>>(emptyList())
    val fireStations = MutableStateFlow<List<SupabaseFireStation>>(emptyList())
    val hospitals = MutableStateFlow<List<SupabaseHospital>>(emptyList())
    val bloodBanks = MutableStateFlow<List<SupabaseBloodBank>>(emptyList())
    val hotlines = MutableStateFlow<List<SupabaseHotline>>(emptyList())
    val safetyTips = MutableStateFlow<List<SupabaseSafetyTip>>(emptyList())

    // Guardian Watch Logic
    val incomingTripShares = MutableStateFlow<List<SupabaseTripShare>>(emptyList())
    val selectedSharedTrip = MutableStateFlow<SupabaseTrip?>(null)
    val activeHeadsUpAlert = MutableStateFlow<HeadsUpAlert?>(null)

    // Emergency Search Logic
    val emergencySearchQuery = MutableStateFlow("")

    // Search and Autocomplete
    val searchSuggestions = MutableStateFlow<List<GeocodeSuggestion>>(emptyList())
    val isSearching = MutableStateFlow(false)

    // Telemetry Tracking
    val activeRoutePath = MutableStateFlow<List<Map<String, Double>>>(emptyList())

    // UI Persistence States (Tracking Tab)
    val travelOriginInput = MutableStateFlow("Locating...")
    val travelDestinationInput = MutableStateFlow("")
    val travelTransportMode = MutableStateFlow(2) // Default: Car
    val travelVehiclePlate = MutableStateFlow("")
    val travelVehicleDescription = MutableStateFlow("")

    // UI Configuration & Status
    val isSupabaseConnected = MutableStateFlow(SupabaseManager.isConfigured)
    val isLoading = MutableStateFlow(false)
    val isImageUploading = MutableStateFlow(false)
    val isAudioUploading = MutableStateFlow(false)
    val safetyAudioLogs = MutableStateFlow<List<com.safetravel.tracker.supabase.SupabaseSafetyAudioLog>>(emptyList())
    val actionFeedbackMessage = MutableStateFlow<String?>(null)

    val vehiclePhotoUrl = MutableStateFlow<String?>(null)
    val isVehiclePhotoUploading = MutableStateFlow(false)

    fun uploadProfileImage(context: Context, uri: android.net.Uri) {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            isImageUploading.value = true
            val uploadResult = com.safetravel.tracker.util.CloudinaryImageUploader.uploadProfileImage(context, uri, profile.id)
            val url = uploadResult?.secureUrl ?: com.safetravel.tracker.util.ImageUploadManager.uploadImage(context, uri)
            if (url != null) {
                updateProfileFields(profile.id, mapOf("avatar_url" to url))
                actionFeedbackMessage.value = "Profile image updated successfully."
            } else {
                actionFeedbackMessage.value = "Failed to upload image. Please try again."
            }
            isImageUploading.value = false
        }
    }

    fun uploadVehiclePhoto(context: Context, uri: android.net.Uri) {
        val profile = userProfile.value
        val userId = profile?.id ?: com.safetravel.tracker.supabase.SupabaseManager.currentUserId ?: "anonymous"
        viewModelScope.launch {
            isVehiclePhotoUploading.value = true
            val result = com.safetravel.tracker.util.CloudinaryImageUploader.uploadTripVehiclePhoto(context, uri, userId)
            if (result != null && result.secureUrl.isNotBlank()) {
                vehiclePhotoUrl.value = result.secureUrl
                actionFeedbackMessage.value = "Vehicle photo attached successfully."
            } else {
                actionFeedbackMessage.value = "Failed to upload vehicle photo. Please check internet connection."
            }
            isVehiclePhotoUploading.value = false
        }
    }

    fun clearVehiclePhoto() {
        vehiclePhotoUrl.value = null
    }
    val currentLocationName = MutableStateFlow("Locating...")
    val currentDistrict = MutableStateFlow("Dhaka")
    var mapHasCenteredInitially = false

    fun updateCurrentLocationName(context: android.content.Context, lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val name = LocationHelper.getPlaceName(context, lat, lng)
                if (name.isNotBlank()) {
                    currentLocationName.value = name
                }
                val district = LocationHelper.getDistrictName(context, lat, lng)
                district?.let {
                    currentDistrict.value = it
                }
            } catch (e: Exception) {
                // Ignore gracefully
            }
        }
    }

    // Form Flows
    val emailInput = MutableStateFlow("")
    val passwordInput = MutableStateFlow("")
    val nameInput = MutableStateFlow("")
    val phoneInput = MutableStateFlow("")
    val isSignUpMode = MutableStateFlow(false)
    val isAuthLoading = MutableStateFlow(false)

    // Emergency Contacts (Stored client-side)
    val emergencyContacts = MutableStateFlow<List<EmergencyContact>>(
        listOf(
            EmergencyContact("c-1", "Spouse (Sarah Rivera)", "Spouse", "+1 555-0144"),
            EmergencyContact("c-2", "Local Dispatch Center", "Emergency Hotline", "911 / 112")
        )
    )
    val currentPosition = MutableStateFlow<Pair<Double, Double>?>(null) // Start as null to trigger locating state
    val currentSpeedKmh = MutableStateFlow(0)

    // --- VOYAGE ANALYTICS (Derived) ---
    val totalDistanceTraveled = MutableStateFlow(0.0)
    val safetySuccessRate = MutableStateFlow(100)
    val totalPointsAccrued = MutableStateFlow(0)

    fun calculateAnalytics() {
        val trips = pastTrips.value
        totalDistanceTraveled.value = trips.sumOf { it.totalDistance ?: 0.0 }
        
        if (trips.isNotEmpty()) {
            val successful = trips.count { it.status == "completed" }
            safetySuccessRate.value = (successful * 100) / trips.size
        } else {
            safetySuccessRate.value = 100
        }
        
        totalPointsAccrued.value = userProfile.value?.pointsBalance ?: 0
    }

    private val resolvedAddressCache = mutableMapOf<String, String>()

    suspend fun reverseGeocodeCoordinates(lat: Double, lng: Double): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (lat == 0.0 && lng == 0.0) return@withContext null

        val cacheKey = "%.4f,%.4f".format(java.util.Locale.US, lat, lng)
        resolvedAddressCache[cacheKey]?.let { return@withContext it }

        var resolvedName: String? = null

        // 1. First priority: High-accuracy Mapbox reverse geocoding API
        val token = mapboxToken.value
        if (token.isNotBlank()) {
            try {
                val urlString = "https://api.mapbox.com/geocoding/v5/mapbox.places/$lng,$lat.json?access_token=$token&country=BD&language=en&limit=1"
                val url = java.net.URL(urlString)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val features = json.optJSONArray("features")
                    if (features != null && features.length() > 0) {
                        val placeObj = features.getJSONObject(0)
                        val fullPlace = placeObj.optString("place_name", "")
                        if (fullPlace.isNotBlank()) {
                            resolvedName = fullPlace.removeSuffix(", Bangladesh").trim()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SafeTravel", "Mapbox reverse geocoding failed: ${e.message}")
            }
        }

        // 2. Fallback: Android Geocoder + OpenStreetMap Nominatim
        if (resolvedName.isNullOrBlank()) {
            try {
                val helperName = LocationHelper.getPlaceName(getApplication(), lat, lng)
                if (helperName.isNotBlank() && helperName != "Unknown Location" && helperName != "My Location") {
                    resolvedName = helperName
                }
            } catch (e: Exception) {
                Log.e("SafeTravel", "LocationHelper reverse geocoding failed: ${e.message}")
            }
        }

        if (!resolvedName.isNullOrBlank()) {
            resolvedAddressCache[cacheKey] = resolvedName
        }
        return@withContext resolvedName
    }

    fun resolveAndSetDestination(context: Context, lat: Double, lng: Double) {
        // Set immediate coordinates-backed tag so map pins and routing start instantly without lag
        travelDestinationInput.value = "Locating address... [$lng, $lat]"

        viewModelScope.launch {
            val resolvedName = reverseGeocodeCoordinates(lat, lng)
            val finalName = if (!resolvedName.isNullOrBlank()) resolvedName else "Location (${String.format(java.util.Locale.US, "%.4f, %.4f", lat, lng)})"

            travelDestinationInput.value = "$finalName [$lng, $lat]"
        }
    }

    suspend fun resolveTripAddresses(trip: SupabaseTrip): SupabaseTrip {
        val start = trip.startAddress ?: ""
        val end = trip.endAddress ?: ""

        val needsStartResolution = AddressUtils.isGenericOrPlaceholder(start) || start.contains("[")
        val newStart = if (needsStartResolution) {
            resolvedAddressCache[start] ?: run {
                val coords = if (trip.startCoords != null && trip.startCoords.contains(",")) {
                    val p = trip.startCoords.split(",")
                    val lat = p[0].toDoubleOrNull()
                    val lng = p.getOrNull(1)?.toDoubleOrNull()
                    if (lat != null && lng != null) Pair(lat, lng) else null
                } else getCoordsForAddress(start)
                if (coords != null) {
                    val name = reverseGeocodeCoordinates(coords.first, coords.second) ?: LocationHelper.getPlaceName(getApplication(), coords.first, coords.second)
                    if (name != "Unknown Location" && name.isNotBlank() && name != "My Location") {
                        resolvedAddressCache[start] = name
                        name
                    } else AddressUtils.formatDisplayAddress(start)
                } else AddressUtils.formatDisplayAddress(start)
            }
        } else AddressUtils.formatDisplayAddress(start)

        val needsEndResolution = AddressUtils.isGenericOrPlaceholder(end) || end.contains("[")
        val newEnd = if (needsEndResolution) {
            resolvedAddressCache[end] ?: run {
                val coords = if (trip.endCoords != null && trip.endCoords.contains(",")) {
                    val p = trip.endCoords.split(",")
                    val lat = p[0].toDoubleOrNull()
                    val lng = p.getOrNull(1)?.toDoubleOrNull()
                    if (lat != null && lng != null) Pair(lat, lng) else null
                } else getCoordsForAddress(end)
                if (coords != null) {
                    val name = reverseGeocodeCoordinates(coords.first, coords.second) ?: LocationHelper.getPlaceName(getApplication(), coords.first, coords.second)
                    if (name != "Unknown Location" && name.isNotBlank() && name != "My Location") {
                        resolvedAddressCache[end] = name
                        name
                    } else AddressUtils.formatDisplayAddress(end)
                } else AddressUtils.formatDisplayAddress(end)
            }
        } else AddressUtils.formatDisplayAddress(end)

        val resolvedTrip = trip.copy(startAddress = newStart, endAddress = newEnd)

        // If either address was newly resolved from a placeholder, persist it to database so it never needs resolving again!
        if ((needsStartResolution && newStart.isNotBlank() && !AddressUtils.isGenericOrPlaceholder(newStart)) ||
            (needsEndResolution && newEnd.isNotBlank() && !AddressUtils.isGenericOrPlaceholder(newEnd))) {
            val updates = mutableMapOf<String, Any>()
            if (needsStartResolution && newStart != start && !AddressUtils.isGenericOrPlaceholder(newStart)) updates["start_address"] = newStart
            if (needsEndResolution && newEnd != end && !AddressUtils.isGenericOrPlaceholder(newEnd)) updates["end_address"] = newEnd

            if (updates.isNotEmpty()) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        if (SupabaseManager.isConfigured && !trip.id.startsWith("T-")) {
                            SupabaseManager.dbService?.updateTrip("eq.${trip.id}", updates, "Bearer ${SupabaseManager.currentSessionToken}")
                        }
                        getApplication<Application>()?.let { ctx ->
                            AppDatabase.getDatabase(ctx).safeTravelDao().insertTrip(resolvedTrip.toOffline())
                        }
                        val mockIdx = SupabaseManager.mockTrips.indexOfFirst { it.id == trip.id }
                        if (mockIdx != -1) {
                            SupabaseManager.mockTrips[mockIdx] = resolvedTrip
                        }
                    } catch (e: Exception) {
                        Log.e("SafeTravelVM", "Failed to persist resolved trip addresses", e)
                    }
                }
            }
        }

        return resolvedTrip
    }

    // New Profile & Destination input states
    val profileBloodGroup = MutableStateFlow("O+")
    val profileNid = MutableStateFlow("4102910405102")
    val profilePresentAddress = MutableStateFlow("")
    val profilePermanentAddress = MutableStateFlow("Village- Bera, P.O.- Bera, Pabna")
    val profileEmergencyName = MutableStateFlow("Sarah Rivera")
    val profileEmergencyRelation = MutableStateFlow("Spouse")
    val profileEmergencyPhone = MutableStateFlow("+1 555-0144")
    val profileVehiclePlate = MutableStateFlow("Dhaka Metro-Ga-42-1200")
    val profileMedicalNotes = MutableStateFlow("Penicillin Allergy, Asthmatic")
    val profileAvatarIndex = MutableStateFlow(0)
    val emergencyMobileNumberToCall = MutableStateFlow("+1 555-0144")
    
    // Configurable Mapbox Token read dynamically from Supabase remote configs or cache
    val mapboxToken = MutableStateFlow(
        SupabaseManager.getCachedConfig("mapbox_access_token").ifBlank {
            if (com.safetravel.tracker.BuildConfig.MAPBOX_ACCESS_TOKEN.isNotBlank() && 
                com.safetravel.tracker.BuildConfig.MAPBOX_ACCESS_TOKEN != "dummy_token" &&
                !com.safetravel.tracker.BuildConfig.MAPBOX_ACCESS_TOKEN.contains("MAPBOX_ACCESS_TOKEN")) {
                com.safetravel.tracker.BuildConfig.MAPBOX_ACCESS_TOKEN
            } else {
                "pk.eyJ1Ijoic3V2b3NoZWlraCIsImEiOiJja3ZwYTAxM3cweHV2MndxaHpiYnA4b3g2In0.20edAsF-TecrWTjbYMNGqg"
            }
        }
    )

    fun configureSupabaseCustom(url: String, anonKey: String) {
        val hadMockSession = SupabaseManager.currentSessionToken != null && !SupabaseManager.currentSessionToken!!.startsWith("eyJ")
        SupabaseManager.saveCustomKeys(url, anonKey)
        isSupabaseConnected.value = SupabaseManager.isConfigured
        if (hadMockSession) {
            userProfile.value = null
        }
    }

    fun resetSupabaseConfig() {
        SupabaseManager.clearCustomKeys()
        isSupabaseConnected.value = SupabaseManager.isConfigured
    }

    fun getCoordsForAddress(address: String): Pair<Double, Double>? {
        val clean = address.lowercase()
        // 1. Check AddressUtils first for exact coordinates or [lng, lat] embedded patterns
        val extracted = AddressUtils.extractCoordinates(address)
        if (extracted != null && extracted.first != 0.0 && extracted.second != 0.0) {
            return extracted
        }
        return when {
            clean.contains("point") || clean.contains("map") || (clean.contains("[") && clean.contains("]")) -> {
                try {
                    val content = clean.substringAfter("[").substringBefore("]")
                    val parts = content.split(",")
                    val lng = parts[0].trim().toDouble()
                    val lat = parts[1].trim().toDouble()
                    Pair(lat, lng)
                } catch (e: Exception) {
                    currentPosition.value ?: lastFilteredLocation?.let { Pair(it.latitude, it.longitude) }
                }
            }
            else -> {
                try {
                    if (clean.contains(",")) {
                        val parts = clean.split(",")
                        val first = parts[0].trim().toDoubleOrNull()
                        val second = parts[1].trim().toDoubleOrNull()
                        if (first != null && second != null) {
                            // Heuristic: if first value > 50, it's likely Lng, so swap for (Lat, Lng)
                            if (first > 50.0) Pair(second, first) else Pair(first, second)
                        } else {
                            currentPosition.value ?: lastFilteredLocation?.let { Pair(it.latitude, it.longitude) }
                        }
                    } else currentPosition.value ?: lastFilteredLocation?.let { Pair(it.latitude, it.longitude) }
                } catch (e: Exception) {
                    currentPosition.value ?: lastFilteredLocation?.let { Pair(it.latitude, it.longitude) }
                }
            }
        }
    }

    fun geocodeAddress(query: String, onResult: (Double, Double, String) -> Unit) {
        if (query.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val token = mapboxToken.value
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val currentPos = currentPosition.value ?: lastFilteredLocation?.let { Pair(it.latitude, it.longitude) }
                val proximityParam = if (currentPos != null && currentPos.second != 0.0 && currentPos.first != 0.0) {
                    "&proximity=${currentPos.second},${currentPos.first}"
                } else ""
                val urlString = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encodedQuery.json?access_token=$token&country=BD&limit=5&types=poi,address,neighborhood,locality,place$proximityParam&autocomplete=true&language=en"
                val url = java.net.URL(urlString)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                if (conn.responseCode == 200) {
                    val stream = conn.inputStream
                    val response = stream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val features = json.optJSONArray("features")
                    if (features != null && features.length() > 0) {
                        val firstObj = features.getJSONObject(0)
                        val placeName = firstObj.optString("place_name", query)
                        val center = firstObj.optJSONArray("center")
                        if (center != null && center.length() >= 2) {
                            val lng = center.getDouble(0)
                            val lat = center.getDouble(1)
                            launch(kotlinx.coroutines.Dispatchers.Main) {
                                onResult(lat, lng, placeName)
                            }
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Geocoding", "Mapbox geocode address error: ${e.message}")
            }
            // Fallback to local coordinate matching dictionary if offline/api error
            val fallback = getCoordsForAddress(query)
            if (fallback != null) {
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(fallback.first, fallback.second, query)
                }
            }
        }
    }

    fun searchPlaces(query: String) {
        if (query.isBlank()) {
            searchSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isSearching.value = true
            try {
                val token = mapboxToken.value
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val currentPos = currentPosition.value ?: lastFilteredLocation?.let { Pair(it.latitude, it.longitude) }
                val proximityParam = if (currentPos != null && currentPos.second != 0.0 && currentPos.first != 0.0) {
                    "&proximity=${currentPos.second},${currentPos.first}"
                } else ""
                val urlString = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encodedQuery.json?access_token=$token&country=BD&limit=10&types=poi,address,neighborhood,locality,place$proximityParam&autocomplete=true&language=en"
                val url = java.net.URL(urlString)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                if (conn.responseCode == 200) {
                    val stream = conn.inputStream
                    val response = stream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val features = json.optJSONArray("features")
                    val results = mutableListOf<GeocodeSuggestion>()
                    if (features != null) {
                        for (i in 0 until features.length()) {
                            val obj = features.getJSONObject(i)
                            val placeName = obj.optString("place_name", "")
                            val center = obj.optJSONArray("center")
                            if (placeName.isNotBlank() && center != null && center.length() >= 2) {
                                val lng = center.getDouble(0)
                                val lat = center.getDouble(1)
                                results.add(GeocodeSuggestion(placeName, lat, lng))
                            }
                        }
                    }
                    searchSuggestions.value = results
                }
            } catch (e: Exception) {
                android.util.Log.e("Geocoding", "Error searching places: ${e.message}")
            } finally {
                isSearching.value = false
            }
        }
    }

    private var locationJob: Job? = null
    private var gpsListener: LocationListener? = null

    init {
        // Load initial mock logs for immediate visibility
        pointsLogs.value = SupabaseManager.mockPointsLogs
        sosRecords.value = SupabaseManager.mockSosRecords
        
        // Observe Repository Flows (Offline-First)
        viewModelScope.launch {
            repository.allBanners.collectLatest { list ->
                homepageBanners.value = list
}
        }
        
        viewModelScope.launch {
            repository.allNews.collectLatest { list ->
                safetyNews.value = list
}
        }

        // 1. Initial One-Time Load of Banners & News into Room DB
        loadInitialContent()

        // 2. Initial User Session Data (Trips, Points, Guardians)
        refreshData()

        // 3. Smart Low-Bandwidth Alert Polling (Every 30s, strictly for incoming trip shares/alerts)
        viewModelScope.launch {
            while (isActive) {
                delay(30000) // 30 seconds interval (reduces polling traffic by >95%)
                if (userProfile.value != null) {
                    refreshIncomingShares()
                }
            }
        }
    }

    fun submitAuthentication() {
        viewModelScope.launch {
            val email = emailInput.value.trim()
            val password = passwordInput.value
            val name = nameInput.value.trim()
            val phone = phoneInput.value.trim()

            if (email.isBlank() || password.isBlank()) {
                actionFeedbackMessage.value = "Error: Input email and password to proceed."
                return@launch
            }

            isAuthLoading.value = true
            actionFeedbackMessage.value = null

            try {
                if (isSignUpMode.value) {
                    // Sign Up Route
                    if (name.isBlank()) {
                        actionFeedbackMessage.value = "Error: Full name is required."
                        return@launch
                    }
                    if (password.length < 6) {
                        actionFeedbackMessage.value = "Error: Password must be at least 6 characters."
                        return@launch
                    }
                    val result = SupabaseManager.signUpWithEmail(email, name, phone, "user", password)
                    result.onSuccess { profile ->
                        userProfile.value = profile
                        actionFeedbackMessage.value = "Account created successfully!"
                        refreshData()
                    }.onFailure { exception ->
                        actionFeedbackMessage.value = exception.localizedMessage ?: "Sign Up failed. Please try again."
                    }
                } else {
                    // Sign In Route (Authenticates with Supabase base password or retrieves local sandbox mock profile)
                    val result = SupabaseManager.loginWithEmail(email, password)
                    result.onSuccess { profile ->
                        userProfile.value = profile
                        actionFeedbackMessage.value = "Signed in successfully!"
                        refreshData()
                    }.onFailure { exception ->
                        actionFeedbackMessage.value = exception.localizedMessage ?: "Invalid login credentials."
                    }
                }
            } finally {
                isAuthLoading.value = false
            }
        }
    }

    fun logout() {
        SupabaseManager.clearSession()
        userProfile.value = null
        activeTrip.value = null
        isAuthLoading.value = false
        actionFeedbackMessage.value = null
        emailInput.value = ""
        passwordInput.value = ""
        nameInput.value = ""
        phoneInput.value = ""
        isSignUpMode.value = false
        cancelLocationTracking()
    }

    fun checkForExistingSession() {
        val currentId = SupabaseManager.currentUserId
        android.util.Log.d("SafeTravelViewModel", "Checking saved session. currentUserId: $currentId")
        if (currentId != null) {
            // First immediately populate with cached profile to avoid blank screen flicker
            val cachedProfile = SupabaseManager.mockProfiles[currentId]
            if (cachedProfile != null && userProfile.value == null) {
                userProfile.value = cachedProfile
            }

            viewModelScope.launch {
                isLoading.value = true
                val result = SupabaseManager.fetchProfile(currentId)
                result.onSuccess { profile ->
                    userProfile.value = profile
                    android.util.Log.d("SafeTravelViewModel", "Session restored successfully: ${profile.fullName}")
                    refreshData()
                }.onFailure {
                    android.util.Log.e("SafeTravelViewModel", "Failed to restore session online: ${it.message}", it)
                    // If we have cached profile, keep it! Do not wipe userProfile to null
                    if (userProfile.value == null) {
                        val fallback = SupabaseManager.mockProfiles[currentId]
                        if (fallback != null) {
                            userProfile.value = fallback
                        }
                    }
                    refreshData() 
                }
                isLoading.value = false
            }
        } else {
            // If no session but we are in mock mode, load the default mock profile for testing
            if (!SupabaseManager.isConfigured) {
                val defaultMock = SupabaseManager.mockProfiles.values.firstOrNull()
                if (defaultMock != null) {
                    userProfile.value = defaultMock
                    SupabaseManager.currentUserId = defaultMock.id
    }
}
            refreshData()
        }
    }

    fun startTrip(
        start: String, 
        end: String, 
        battery: Int, 
        vehicle: String, 
        vehicleDesc: String, 
        modeInt: Int = 2, 
        sharedPhones: List<String> = emptyList(), 
        plannedRoute: List<Map<String, Double>> = emptyList(),
        estimatedDistanceKm: Double? = null,
        estimatedDurationMin: Double? = null,
        vehiclePhoto: String? = null
    ) {
        val profile = userProfile.value
        if (profile == null) {
            actionFeedbackMessage.value = "Session expired. Please log in again."
            return
        }

        val modeStr = when(modeInt) {
            0 -> "walking"
            1 -> "cycling"
            2 -> "driving"
            3 -> "bus"
            else -> "driving"
        }

        viewModelScope.launch {
            isLoading.value = true
            actionFeedbackMessage.value = null

            // 1. Resolve REAL user start GPS coordinates (STRICTLY NO MOCK DATA)
            var realStartCoords: Pair<Double, Double>? = AddressUtils.extractCoordinates(start)
            if (realStartCoords == null || realStartCoords.first == 0.0 || realStartCoords.second == 0.0) {
                realStartCoords = currentPosition.value 
                    ?: lastFilteredLocation?.let { Pair(it.latitude, it.longitude) }
            }
            if (realStartCoords == null || realStartCoords.first == 0.0 || realStartCoords.second == 0.0) {
                val immediateLoc = LocationHelper.getCurrentLocation(getApplication())
                if (immediateLoc != null && immediateLoc.latitude != 0.0 && immediateLoc.longitude != 0.0) {
                    realStartCoords = Pair(immediateLoc.latitude, immediateLoc.longitude)
                    currentPosition.value = realStartCoords
                }
            }

            if (realStartCoords == null || realStartCoords.first == 0.0 || realStartCoords.second == 0.0) {
                isLoading.value = false
                actionFeedbackMessage.value = "Waiting for real GPS signal... Please ensure Location is enabled."
                return@launch
            }

            val startCoordsPair = realStartCoords

            // 2. Resolve destination coordinates
            var realEndCoords: Pair<Double, Double>? = AddressUtils.extractCoordinates(end)
            if (realEndCoords == null || realEndCoords.first == 0.0 || realEndCoords.second == 0.0) {
                realEndCoords = getCoordsForAddress(end)
            }
            val endCoordsPair = realEndCoords ?: startCoordsPair

            val startCoordsStr = "${startCoordsPair.first},${startCoordsPair.second}"
            val endCoordsStr = "${endCoordsPair.first},${endCoordsPair.second}"
            val deviceModel = android.os.Build.MODEL

            var cleanStart = AddressUtils.cleanAddressForStorage(start)
            var cleanEnd = AddressUtils.cleanAddressForStorage(end)

            // Cross-check: guarantee destination address is a clean, real location before saving to DB
            if (AddressUtils.isGenericOrPlaceholder(cleanEnd) || cleanEnd.startsWith("Location (")) {
                val resolvedEnd = reverseGeocodeCoordinates(endCoordsPair.first, endCoordsPair.second)
                if (!resolvedEnd.isNullOrBlank()) {
                    cleanEnd = resolvedEnd
                    travelDestinationInput.value = "$resolvedEnd [${endCoordsPair.second}, ${endCoordsPair.first}]"
                }
            }

            // Cross-check: guarantee start address is also a real location
            if (AddressUtils.isGenericOrPlaceholder(cleanStart) || cleanStart.startsWith("Location (") || cleanStart == "Locating...") {
                val resolvedStart = reverseGeocodeCoordinates(startCoordsPair.first, startCoordsPair.second)
                if (!resolvedStart.isNullOrBlank()) {
                    cleanStart = resolvedStart
                    travelOriginInput.value = resolvedStart
                } else {
                    cleanStart = "Location (${String.format(java.util.Locale.US, "%.4f, %.4f", startCoordsPair.first, startCoordsPair.second)})"
                }
            }

            val result = SupabaseManager.startTrip(
                userId = profile.id,
                start = cleanStart,
                end = cleanEnd,
                mode = modeStr,
                battery = battery,
                vehicle = vehicle,
                vehicleDesc = vehicleDesc,
                startCoords = startCoordsStr,
                endCoords = endCoordsStr,
                deviceModel = deviceModel,
                notifiedGuardians = sharedPhones,
                plannedRouteLog = if (plannedRoute.isNotEmpty()) plannedRoute else null,
                estimatedDistanceKm = estimatedDistanceKm,
                estimatedDurationMin = estimatedDurationMin,
                vehiclePhotoUrl = vehiclePhoto ?: vehiclePhotoUrl.value
            )
            result.onSuccess { trip ->
                activeTrip.value = trip
                vehiclePhotoUrl.value = null
                val initialPath = if (startCoordsPair.first != 0.0 && startCoordsPair.second != 0.0) {
                    listOf(mapOf("lat" to startCoordsPair.first, "lng" to startCoordsPair.second))
                } else {
                    emptyList()
                }
                activeRoutePath.value = initialPath
                com.safetravel.tracker.util.ActiveTripTrailCache.saveTrail(getApplication(), trip.id, initialPath)
                adaptiveLocationFilter.reset()
                actionFeedbackMessage.value = "Trip started! Live tracking is active."

                // Connect persistent WebSocket to Supabase Realtime for zero-egress live streaming
                SupabaseRealtimeBroadcaster.connectTrip(trip.id)

                // Share with guardians if any selected
                if (sharedPhones.isNotEmpty()) {
                    shareTripWithGuardians(trip.id, sharedPhones)
                }

                refreshData()
            }.onFailure {
                android.util.Log.e("SafeTravel", "Failed to start trip", it)
                actionFeedbackMessage.value = "Error: ${it.localizedMessage}"
            }
            isLoading.value = false
        }
    }

    fun endTrip(battery: Int? = null, distance: Double? = null) {
        val trip = activeTrip.value ?: return
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            isLoading.value = true

            // Cross-check ending location: where did the user actually end the trip?
            val currentLoc = currentPosition.value ?: lastFilteredLocation?.let { Pair(it.latitude, it.longitude) }
            var finalEndAddress = trip.endAddress ?: ""
            var finalEndCoords = trip.endCoords ?: ""

            if (currentLoc != null && currentLoc.first != 0.0 && currentLoc.second != 0.0) {
                val currentLat = currentLoc.first
                val currentLng = currentLoc.second
                finalEndCoords = "$currentLat,$currentLng"

                val plannedCoords = AddressUtils.extractCoordinates(trip.endCoords ?: trip.endAddress)
                val isPlaceholder = AddressUtils.isGenericOrPlaceholder(finalEndAddress)
                var needsActualAddressUpdate = isPlaceholder

                if (plannedCoords != null && plannedCoords.first != 0.0 && plannedCoords.second != 0.0) {
                    val distanceResults = FloatArray(1)
                    android.location.Location.distanceBetween(
                        plannedCoords.first, plannedCoords.second,
                        currentLat, currentLng,
                        distanceResults
                    )
                    val distanceDiffMeters = distanceResults[0]
                    // If user finished journey > 200m away from planned destination, update address!
                    if (distanceDiffMeters > 200f) {
                        needsActualAddressUpdate = true
                        Log.d("SafeTravelVM", "Trip ended $distanceDiffMeters m away from planned destination. Updating end address.")
                    }
                } else {
                    needsActualAddressUpdate = true
                }

                if (needsActualAddressUpdate) {
                    val actualAddress = reverseGeocodeCoordinates(currentLat, currentLng)
                    if (!actualAddress.isNullOrBlank()) {
                        finalEndAddress = actualAddress
                    }
                }
            }

            val cleanFinalEnd = AddressUtils.cleanAddressForStorage(finalEndAddress)

            // Force save final route log
            if (SupabaseManager.isConfigured && activeRoutePath.value.isNotEmpty()) {
                try {
                    SupabaseManager.dbService?.updateTrip(
                        idQuery = "eq.${trip.id}",
                        updates = mapOf("route_path_log" to activeRoutePath.value),
                        userAuth = "Bearer ${SupabaseManager.currentSessionToken}"
                    )
                } catch (e: Exception) {}
            }
            
            // Calculate 100% accurate actual traveled distance from complete stitched breadcrumb log
            val context = getApplication<Application>()
            val cachedTrail = com.safetravel.tracker.util.ActiveTripTrailCache.loadTrail(context, trip.id)
            val fullTraveledList = com.safetravel.tracker.util.ActiveTripTrailCache.stitchTrail(
                inMemoryTrail = activeRoutePath.value,
                persistedTrail = trip.routePathLog,
                cachedTrail = cachedTrail
            )
            val calculatedActualKm = com.safetravel.tracker.util.TripTelemetryCalculator.calculateActualTraveledDistanceKm(fullTraveledList)
            val finalDistanceToSave = if (calculatedActualKm > 0.05) {
                calculatedActualKm
            } else {
                distance ?: trip.totalDistance ?: trip.estimatedDistanceKm ?: 0.0
            }

            val finalEndBattery = battery ?: com.safetravel.tracker.util.BatteryHelper.getCurrentBatteryLevel(context)
            val result = SupabaseManager.endTrip(
                tripId = trip.id,
                endBattery = finalEndBattery,
                totalDistance = finalDistanceToSave,
                finalEndAddress = cleanFinalEnd,
                finalEndCoords = finalEndCoords
            )
            result.onSuccess {
                com.safetravel.tracker.util.ActiveTripTrailCache.clearTrail(getApplication(), trip.id)
                activeRoutePath.value = emptyList()
                activeTrip.value = null
                cancelLocationTracking()
                stopSosSafetyCheckLoop()
                SupabaseRealtimeBroadcaster.disconnectTrip()

                // Reward standard or premium bonus points dynamically based on Admin Remote Configs
                val isEngineActive = SupabaseManager.getRemoteConfig("points_engine_enabled", "true") != "false"
                if (isEngineActive) {
                    val basePoints = SupabaseManager.getPointConfig("points_per_trip", 50)
                    val premiumPoints = SupabaseManager.getPointConfig("points_per_trip_premium", 100)
                    val baseReward = if (profile.isPremium == true) premiumPoints else basePoints

                    val pointsPerKm = SupabaseManager.getPointConfig("points_per_km", 0)
                    val distanceBonus = if (pointsPerKm > 0 && finalDistanceToSave > 0.1) {
                        (finalDistanceToSave * pointsPerKm).toInt()
                    } else 0

                    val totalReward = baseReward + distanceBonus
                    val newPoints = profile.pointsBalance + totalReward
                    val newCredits = profile.tripCredits + 1

                    SupabaseManager.updateProfileFields(
                        profile.id,
                        mapOf("points_balance" to newPoints, "trip_credits" to newCredits)
                    )

                    val bonusNote = if (distanceBonus > 0) " (includes $distanceBonus KM distance bonus)" else ""
                    SupabaseManager.logPoints(profile.id, totalReward, "Trip completed safely: $cleanFinalEnd$bonusNote")
                    actionFeedbackMessage.value = "Trip completed safely! +$totalReward points earned!"
                } else {
                    actionFeedbackMessage.value = "Trip completed safely!"
                }
                syncProfile()
                refreshData()
            }.onFailure {
                actionFeedbackMessage.value = "Error completing trip: ${it.localizedMessage}"
            }
            isLoading.value = false
        }
    }

    val showSosSafetyCheckWizard = mutableStateOf(false)
    private var sosSafetyCheckJob: kotlinx.coroutines.Job? = null
    var sosSafetyCheckCount = 0

    fun startSosSafetyCheckLoop(intervalMin: Int) {
        sosSafetyCheckJob?.cancel()
        sosSafetyCheckJob = viewModelScope.launch {
            while (isActive && activeTrip.value?.status == "sos") {
                delay((intervalMin * 60 * 1000L).coerceAtLeast(10000L))
                if (activeTrip.value?.status == "sos") {
                    showSosSafetyCheckWizard.value = true
                }
            }
        }
    }

    fun stopSosSafetyCheckLoop() {
        sosSafetyCheckJob?.cancel()
        sosSafetyCheckJob = null
        showSosSafetyCheckWizard.value = false
    }

    fun reportSosSafetyCheck(responseType: String) {
        val trip = activeTrip.value ?: return
        showSosSafetyCheckWizard.value = false
        sosSafetyCheckCount++
        viewModelScope.launch {
            val detailsText = when (responseType) {
                "safe" -> "User reported: I Am Okay"
                "danger" -> "User reported: Need Help (Danger)"
                else -> "Safety check auto-dismissed: No response from user within 15 seconds"
            }
            com.safetravel.tracker.util.SosEmergencyOrchestrator.logSosEvent(
                tripId = trip.id,
                actionType = "safety_check_response",
                details = detailsText,
                metadata = mapOf(
                    "check_index" to sosSafetyCheckCount,
                    "response_type" to responseType
                )
            )
            actionFeedbackMessage.value = when (responseType) {
                "safe" -> "Guardians updated: You reported safe."
                "danger" -> "CRITICAL: Guardians alerted you need urgent help!"
                else -> "Guardians alerted: No response received from safety check."
            }
        }
    }

    fun resolveSos() {
        val trip = activeTrip.value ?: return
        viewModelScope.launch {
            SupabaseManager.updateTripSafetyStatus(trip.id, "ongoing")
            activeTrip.value = trip.copy(status = "ongoing")
            stopSosSafetyCheckLoop()
            com.safetravel.tracker.util.SosEmergencyOrchestrator.logSosEvent(
                tripId = trip.id,
                actionType = "sos_resolved",
                details = "User cancelled SOS and marked themselves safe"
            )
            actionFeedbackMessage.value = "SOS resolved. Status restored to active journey."
            refreshData()
        }
    }

    fun triggerSos() {
        val trip = activeTrip.value ?: return
        viewModelScope.launch {
            // Immediate local red alert state
            activeTrip.value = trip.copy(status = "sos")
            actionFeedbackMessage.value = "RED ALERT: SOS signal dispatched to all guardians."

            val appCtx = getApplication<Application>()

            // 1. Dispatch WhatsApp distress messages to ALL guardians immediately
            appCtx?.let { ctx ->
                com.safetravel.tracker.util.SosEmergencyOrchestrator.dispatchSosToAllGuardians(
                    context = ctx,
                    trip = trip,
                    guardians = userGuardians.value,
                    currentAddress = trip.startAddress
                )
            }

            // 2. Start In-App Safety Check Loop
            val intervalMin = appCtx?.let { com.safetravel.tracker.util.SosSettingsManager.getSafetyCheckIntervalMin(it) } ?: 2
            startSosSafetyCheckLoop(intervalMin)

            // 3. Sync to Supabase in background
            try {
                SupabaseManager.triggerSos(trip.id)
                com.safetravel.tracker.util.SosEmergencyOrchestrator.logSosEvent(
                    tripId = trip.id,
                    actionType = "sos_triggered",
                    details = "Emergency SOS activated via hold trigger button",
                    metadata = mapOf(
                        "current_lat" to (trip.currentLat ?: 0.0),
                        "current_lng" to (trip.currentLng ?: 0.0),
                        "battery_level" to (trip.startBatteryLevel ?: 0)
                    )
                )
                refreshData()
            } catch (e: Exception) {
                Log.w("SafeTravelVM", "Supabase SOS background sync warning: ${e.message}")
            }
        }
    }

    fun togglePremium() {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            val nextState = profile.isPremium != true
            val result = SupabaseManager.updateProfileFields(profile.id, mapOf("is_premium" to nextState))
            result.onSuccess { updated ->
                userProfile.value = updated
                actionFeedbackMessage.value = if (nextState) "Premium Vault Enforcer Activated! 💎" else "Premium state configured off."
}
        }
    }

    fun updateProfileFields(userId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            val result = SupabaseManager.updateProfileFields(userId, updates)
            result.onSuccess { updated ->
                userProfile.value = updated
                actionFeedbackMessage.value = "Profile metrics synchronized. 🛡️"
}
            result.onFailure { exception ->
                actionFeedbackMessage.value = "Error: ${exception.localizedMessage}"
                android.util.Log.e("SafeTravelViewModel", "Update failed", exception)
}
        }
    }

    fun addEmergencyContact(name: String, relation: String, phone: String) {
        val newC = EmergencyContact(
            id = "C-" + UUID.randomUUID().toString().take(5).uppercase(),
            name = name,
            relation = relation,
            phone = phone
        )
        emergencyContacts.value = emergencyContacts.value + newC
    }

    fun deleteEmergencyContact(id: String) {
        emergencyContacts.value = emergencyContacts.value.filter { it.id != id }
    }

    // Guardian Network Logic
    val userGuardians = MutableStateFlow<List<Guardian>>(emptyList())

    fun saveGuardianToSupabase(guardian: Guardian) {
        viewModelScope.launch {
            val result = SupabaseManager.saveGuardian(guardian)
            result.onSuccess {
                refreshGuardians()
                actionFeedbackMessage.value = "Guardian Added Successfully! 🛡️"
}
        }
    }

    fun refreshGuardians() {
        val currentId = userProfile.value?.id ?: SupabaseManager.currentUserId ?: "00000000-0000-0000-0000-000000000001"
        viewModelScope.launch {
            SupabaseManager.fetchGuardians(currentId).onSuccess { list ->
                userGuardians.value = list
            }.onFailure {
                // If offline/error and list is empty, supply mock guardians
                if (userGuardians.value.isEmpty()) {
                    userGuardians.value = SupabaseManager.mockGuardians
                }
            }
        }
    }

    fun deleteGuardian(id: String) {
        viewModelScope.launch {
            SupabaseManager.deleteGuardian(id).onSuccess {
                refreshGuardians()
                actionFeedbackMessage.value = "Guardian record purged."
}
        }
    }

    fun shareTripWithGuardians(tripId: String, phones: List<String>) {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            SupabaseManager.shareTrip(tripId, profile.id, phones).onSuccess {
                actionFeedbackMessage.value = "Trip shared with selected guardians! 🛡️"
            }
        }
    }

    fun refreshIncomingShares() {
        val profile = userProfile.value ?: return
        val phone = profile.phoneNumber ?: ""
        val userId = profile.id
        viewModelScope.launch {
            SupabaseManager.fetchIncomingShares(phone, userId).onSuccess { list ->
                val oldActiveIds = incomingTripShares.value.filter { it.status == "active" }.map { it.tripId }.toSet()
                incomingTripShares.value = list
                
                // Alert if a new active share appears while the app is active
                if (oldActiveIds.isNotEmpty()) {
                    val newActive = list.filter { it.status == "active" && !oldActiveIds.contains(it.tripId) }
                    if (newActive.isNotEmpty()) {
                        val newest = newActive.first()
                        val sharerName = newest.sharedByProfile?.fullName ?: "Someone close to you"
                        activeHeadsUpAlert.value = HeadsUpAlert(
                            title = "🚨 New Trip Alert!",
                            message = "$sharerName has started a trip and shared their live tracking with you.",
                            tripId = newest.tripId,
                            trackingCode = newest.tripDetails?.trackingCode ?: ""
                        )
                    }
                }
            }
        }
    }

    fun observeSharedTrip(tripId: String) {
        // Immediate local lookup to prevent screen load latency
        val localTrip = incomingTripShares.value.find { it.tripId == tripId }?.tripDetails
            ?: pastTrips.value.find { it.id == tripId }
            ?: activeTrip.value?.takeIf { it.id == tripId }
            ?: SupabaseManager.mockTrips.find { it.id == tripId }

        if (localTrip != null) {
            selectedSharedTrip.value = localTrip
        }

        viewModelScope.launch {
            while (isActive) {
                SupabaseManager.fetchTripById(tripId).onSuccess { trip ->
                    selectedSharedTrip.value = trip
                }
                delay(4000) // Poll every 4 seconds for live tracking
            }
        }
    }

    fun loadInitialContent() {
        viewModelScope.launch {
            try {
                repository.refreshBanners()
                repository.refreshNews()
            } catch (e: Exception) {
                Log.e("SafeTravelVM", "Initial content load error: ${e.message}")
            }
        }
    }

    fun refreshData() {
        val profile = userProfile.value
        viewModelScope.launch {
            // Keep points and app remote configs fresh from Supabase REST API
            SupabaseManager.fetchRemoteConfigs()

            // Fetch past trips if logged in
            profile?.let {
                SupabaseManager.fetchUserTrips(it.id).onSuccess { trips ->
                    val ongoing = trips.find { it.status == "ongoing" || it.status == "sos" }
                    if (ongoing != null) {
                        activeTrip.value = ongoing
                        val context = getApplication<Application>()
                        val cachedTrail = com.safetravel.tracker.util.ActiveTripTrailCache.loadTrail(context, ongoing.id)
                        val fullTrail = com.safetravel.tracker.util.ActiveTripTrailCache.stitchTrail(
                            inMemoryTrail = activeRoutePath.value,
                            persistedTrail = ongoing.routePathLog,
                            cachedTrail = cachedTrail
                        )
                        if (fullTrail.isNotEmpty()) {
                            activeRoutePath.value = fullTrail
                            activeTrip.value = ongoing.copy(routePathLog = fullTrail)
                            com.safetravel.tracker.util.ActiveTripTrailCache.saveTrail(context, ongoing.id, fullTrail)
                        }
                        SupabaseRealtimeBroadcaster.connectTrip(ongoing.id)
                    }
                    pastTrips.value = trips.filter { it.status != "ongoing" && it.status != "sos" }
                    calculateAnalytics()
                }
            }

            // Refresh incoming shared trips for Guardian Alerts
            refreshIncomingShares()
            refreshGuardians()
            refreshSafetyAudioLogs()
            refreshRemoteConfigs()

            // Retrieve points log and sos signals from Supabase/Mock db
            val sosRes = SupabaseManager.getLiveSosRecords()
            sosRes.onSuccess { list ->
                sosRecords.value = list
            }

            val pLog = if (SupabaseManager.isConfigured) {
                emptyList()
            } else {
                SupabaseManager.mockPointsLogs
            }
            pointsLogs.value = pLog
        }
    }

    fun refreshPoliceStations(force: Boolean = false) {
        if (!force && policeStations.value.isNotEmpty()) return
        viewModelScope.launch {
            if (policeStations.value.isEmpty()) isLoading.value = true
            PoliceStationManager.fetchPoliceStations().onSuccess { list ->
                policeStations.value = list
            }.onFailure {
                Log.e("SafeTravelVM", "Failed to fetch police stations", it)
            }
            isLoading.value = false
        }
    }

    fun refreshFireStations(force: Boolean = false) {
        if (!force && fireStations.value.isNotEmpty()) return
        viewModelScope.launch {
            if (fireStations.value.isEmpty()) isLoading.value = true
            FireStationManager.fetchFireStations().onSuccess { list ->
                fireStations.value = list
            }.onFailure {
                Log.e("SafeTravelVM", "Failed to fetch fire stations", it)
            }
            isLoading.value = false
        }
    }

    fun refreshHospitals(force: Boolean = false) {
        if (!force && hospitals.value.isNotEmpty()) return
        viewModelScope.launch {
            if (hospitals.value.isEmpty()) isLoading.value = true
            HospitalManager.fetchHospitals().onSuccess { list ->
                hospitals.value = list
            }.onFailure {
                Log.e("SafeTravelVM", "Failed to fetch hospitals", it)
            }
            isLoading.value = false
        }
    }

    fun refreshBloodBanks(force: Boolean = false) {
        if (!force && bloodBanks.value.isNotEmpty()) return
        viewModelScope.launch {
            if (bloodBanks.value.isEmpty()) isLoading.value = true
            BloodBankManager.fetchBloodBanks().onSuccess { list ->
                bloodBanks.value = list
            }.onFailure {
                Log.e("SafeTravelVM", "Failed to fetch blood banks", it)
            }
            isLoading.value = false
        }
    }

    fun refreshHotlines(force: Boolean = false) {
        if (!force && hotlines.value.isNotEmpty()) return
        viewModelScope.launch {
            if (hotlines.value.isEmpty()) isLoading.value = true
            EmergencyManager.fetchHotlines().onSuccess { list ->
                hotlines.value = list
            }.onFailure {
                Log.e("SafeTravelVM", "Failed to fetch hotlines", it)
            }
            isLoading.value = false
        }
    }

    fun refreshSafetyTips(force: Boolean = false) {
        if (!force && safetyTips.value.isNotEmpty()) return
        val profile = userProfile.value
        viewModelScope.launch {
            if (safetyTips.value.isEmpty()) isLoading.value = true
            SafetyTipManager.fetchTipsWithStatus(profile?.id).onSuccess { list ->
                safetyTips.value = list
            }.onFailure {
                Log.e("SafeTravelVM", "Failed to fetch safety tips", it)
            }
            isLoading.value = false
        }
    }

    fun markTipAsRead(tipId: String) {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            SafetyTipManager.markAsRead(profile.id, tipId).onSuccess {
                // Update local state immediately for UI responsiveness
                val updatedList = safetyTips.value.map {
                    if (it.id == tipId) it.copy(isRead = true) else it
                }
                safetyTips.value = updatedList
                actionFeedbackMessage.value = "Tip marked as read! ✅"
            }
        }
    }

    fun prefetchImages(context: Context) {
        val urls = mutableListOf<String>()
        homepageBanners.value.forEach { it.imageUrl?.let { url -> urls.add(url) } }
        safetyNews.value.forEach { it.imageUrl?.let { url -> urls.add(url) } }

        urls.forEach { url ->
            val request = ImageRequest.Builder(context)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
            context.imageLoader.enqueue(request)
        }
    }

    fun refreshSafetyAudioLogs() {
        val profile = userProfile.value ?: return
        viewModelScope.launch {
            SupabaseManager.fetchSafetyAudioLogs(profile.id).onSuccess { list ->
                safetyAudioLogs.value = list
            }
        }
    }

    fun deleteSafetyAudioLog(audioLog: com.safetravel.tracker.supabase.SupabaseSafetyAudioLog, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            // 1. Instantly update UI state for zero-latency user feedback
            safetyAudioLogs.value = safetyAudioLogs.value.filter { it.id != audioLog.id }

            // 2. Soft-delete in Supabase (persisting evidence on server while marking as deleted for user)
            val res = SupabaseManager.softDeleteSafetyAudioLog(audioLog.id)

            // 3. Delete local physical file if it exists on the device to free phone storage
            try {
                val context = getApplication<Application>()
                if (audioLog.audioUrl.startsWith("file:")) {
                    val path = android.net.Uri.parse(audioLog.audioUrl).path
                    if (!path.isNullOrBlank()) {
                        val localFile = java.io.File(path)
                        if (localFile.exists()) localFile.delete()
                    }
                } else {
                    val recordDir = java.io.File(context.filesDir, "safety_recordings")
                    val localFileName = audioLog.cloudinaryPublicId?.substringAfterLast("/") ?: ""
                    if (localFileName.isNotBlank()) {
                        val candidates = recordDir.listFiles { _, name -> name.startsWith(localFileName) }
                        candidates?.forEach { it.delete() }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SafeTravelViewModel", "Local audio file delete error: ${e.message}")
            }

            onComplete?.invoke(res.isSuccess)
        }
    }

    fun refreshRemoteConfigs() {
        viewModelScope.launch {
            SupabaseManager.fetchRemoteConfigs().onSuccess { configs ->
                configs["mapbox_access_token"]?.let { token ->
                    if (token.isNotBlank() && token != mapboxToken.value) {
                        mapboxToken.value = token
                    }
                }
            }
        }
    }

    fun uploadAndSaveAudio(
        file: java.io.File,
        tripId: String? = null,
        durationSec: Int = 0,
        sourceTrigger: String = "manual_toolkit"
    ) {
        val profile = userProfile.value
        viewModelScope.launch {
            isAudioUploading.value = true
            try {
                // 1. Resolve current GPS location for evidence metadata
                val currentPos = currentPosition.value ?: lastFilteredLocation?.let { Pair(it.latitude, it.longitude) }
                val coordsStr = if (currentPos != null && currentPos.first != 0.0) "${currentPos.first},${currentPos.second}" else null
                val address = if (currentPos != null && currentPos.first != 0.0) {
                    reverseGeocodeCoordinates(currentPos.first, currentPos.second) ?: currentLocationName.value
                } else {
                    currentLocationName.value
                }

                // 2. Upload high-compressed audio file to Cloudinary with rich evidence metadata (Zero Supabase Egress)
                val currentUid = profile?.id ?: SupabaseManager.currentUserId
                val context = getApplication<Application>()
                val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as? android.os.BatteryManager
                val battery = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
                val devModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()

                val uploadResult = com.safetravel.tracker.util.CloudinaryAudioUploader.uploadAudio(
                    file = file,
                    userId = currentUid,
                    tripId = tripId,
                    recordedAddress = address,
                    recordedCoords = coordsStr,
                    sourceTrigger = sourceTrigger,
                    batteryLevel = battery,
                    deviceModel = devModel
                )

                if (uploadResult != null && uploadResult.secureUrl.isNotBlank()) {
                    // 3. Save entry to Supabase safety_audio_logs
                    val saveRes = SupabaseManager.createSafetyAudioLog(
                        userId = currentUid,
                        tripId = tripId,
                        audioUrl = uploadResult.secureUrl,
                        cloudinaryPublicId = uploadResult.publicId,
                        durationSec = durationSec.coerceAtLeast(1),
                        fileSizeBytes = uploadResult.fileSizeBytes,
                        recordedAddress = address,
                        recordedCoords = coordsStr,
                        sourceTrigger = sourceTrigger
                    )

                    // 4. If this audio is bound to an active trip, update travel_activities.audio_clip_url
                    if (!tripId.isNullOrBlank() && SupabaseManager.isConfigured) {
                        try {
                            SupabaseManager.dbService?.updateTrip(
                                idQuery = "eq.$tripId",
                                updates = mapOf("audio_clip_url" to uploadResult.secureUrl),
                                userAuth = "Bearer ${SupabaseManager.currentSessionToken}"
                            )
                        } catch (e: Exception) {
                            Log.w("SafeTravelVM", "Failed to update trip audio_clip_url", e)
                        }
                    }

                    actionFeedbackMessage.value = "🎙️ Audio Blackbox secured & uploaded to vault!"
                    refreshSafetyAudioLogs()
                } else {
                    actionFeedbackMessage.value = "Audio saved locally on device."
                }
            } catch (e: Exception) {
                Log.e("SafeTravelVM", "Failed to upload and save audio", e)
                actionFeedbackMessage.value = "Audio saved locally on device."
            } finally {
                isAudioUploading.value = false
            }
        }
    }

    fun syncProfile() {
        val current = userProfile.value ?: return
        viewModelScope.launch {
            SupabaseManager.fetchProfile(current.id).onSuccess { synced ->
                userProfile.value = synced
            }
        }
    }

    private var lastDbCheckpointTimeMs: Long = 0L

    fun updateLiveCoordinatesInSupabase(lat: Double, lng: Double) {
        val currentTrip = activeTrip.value ?: return
        viewModelScope.launch {
            // 1. Update local path log with seamless historical stitching (zero data loss across app kills)
            val newPoint = mapOf("lat" to lat, "lng" to lng)
            val context = getApplication<Application>()
            val cachedTrail = com.safetravel.tracker.util.ActiveTripTrailCache.loadTrail(context, currentTrip.id)
            val updatedPath = com.safetravel.tracker.util.ActiveTripTrailCache.stitchTrail(
                inMemoryTrail = activeRoutePath.value,
                persistedTrail = currentTrip.routePathLog,
                cachedTrail = cachedTrail,
                newPoint = newPoint
            )
            val currentBat = com.safetravel.tracker.util.BatteryHelper.getCurrentBatteryLevel(context)
            activeRoutePath.value = updatedPath
            activeTrip.value = currentTrip.copy(
                routePathLog = updatedPath,
                endBatteryLevel = currentBat
            )
            com.safetravel.tracker.util.ActiveTripTrailCache.saveTrail(context, currentTrip.id, updatedPath)

            // 2. High-performance, 0ms Persistent WebSocket Broadcast (Near-zero egress, zero DB load)
            val speed = currentSpeedKmh.value
            val heading = lastFilteredLocation?.bearing ?: 0f
            SupabaseRealtimeBroadcaster.sendLocation(
                lat = lat,
                lng = lng,
                speedKmh = speed,
                heading = heading,
                battery = currentBat
            )

            // 3. Live Database Update with complete route_path_log history and current battery
            if (SupabaseManager.isConfigured) {
                try {
                    val authHeader = "Bearer ${SupabaseManager.currentSessionToken}"
                    val updates = mutableMapOf<String, Any>(
                        "current_lat" to lat,
                        "current_lng" to lng,
                        "route_path_log" to updatedPath,
                        "end_battery_level" to currentBat
                    )
                    
                    val response = SupabaseManager.dbService?.updateTrip(
                        idQuery = "eq.${currentTrip.id}",
                        updates = updates,
                        userAuth = authHeader
                    )
                    if (response?.isSuccessful == true && !response.body().isNullOrEmpty()) {
                        android.util.Log.d("Telemetry", "Supabase live coords, route_path_log, and battery updated ($currentBat%). Points: ${updatedPath.size}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Telemetry", "Failed to upload live coords to Supabase", e)
                }
            } else {
                // Mock update
                val mockIndex = SupabaseManager.mockTrips.indexOfFirst { it.id == currentTrip.id }
                if (mockIndex != -1) {
                    SupabaseManager.mockTrips[mockIndex] = SupabaseManager.mockTrips[mockIndex].copy(
                        routePathLog = updatedPath,
                        endBatteryLevel = currentBat
                    )
                }
            }
        }
    }

    fun onManualLocationUpdate(lat: Double, lng: Double) {
        currentPosition.value = Pair(lat, lng)
        
        // Auto-sync origin input if it's still in initial state
        if (travelOriginInput.value == "Locating..." || travelOriginInput.value == "Unknown Location") {
            viewModelScope.launch {
                val name = LocationHelper.getPlaceName(getApplication(), lat, lng)
                if (name.isNotBlank() && name != "Unknown Location") {
                    travelOriginInput.value = name
                }
            }
        }

        // Only upload telemetry if there is an active trip
        if (activeTrip.value != null) {
            updateLiveCoordinatesInSupabase(lat, lng)
        }
    }

    fun submitFeedback(rating: Int, type: String, subject: String, message: String, onSuccess: () -> Unit) {
        val profile = userProfile.value
        viewModelScope.launch {
            isLoading.value = true
            val feedback = SupabaseFeedback(
                userId = profile?.id,
                rating = rating,
                feedbackType = type,
                subject = subject,
                message = message
            )
            val result = SupabaseManager.submitFeedback(feedback)
            result.onSuccess {
                actionFeedbackMessage.value = "Feedback shared with engineering! 🚀"
                onSuccess()
}.onFailure {
                actionFeedbackMessage.value = "Failed to send feedback: ${it.localizedMessage}"
}
            isLoading.value = false
        }
    }

    private val adaptiveLocationFilter = AdaptiveLocationFilter()
    private var lastFilteredLocation: Location? = null
    private var locationUpdatesJob: Job? = null

    fun onFilteredLocationReceived(location: Location) {
        val rawLat = location.latitude
        val rawLng = location.longitude
        if (rawLat == 0.0 && rawLng == 0.0) return

        // 0. Determine active transport mode (Walk: 0, Cycle: 1, Car: 2, Bus: 3)
        val mode = activeTrip.value?.let { trip ->
            when (trip.transportMode?.lowercase()) {
                "walk", "walking", "0" -> AdaptiveLocationFilter.MODE_WALK
                "cycle", "bike", "bicycling", "1" -> AdaptiveLocationFilter.MODE_CYCLE
                "bus", "transit", "3" -> AdaptiveLocationFilter.MODE_BUS
                else -> AdaptiveLocationFilter.MODE_CAR
            }
        } ?: travelTransportMode.value

        // 1. Pass through Adaptive Kalman & Deadband Filter (Smooths trajectory without snapping to road)
        val smoothed = adaptiveLocationFilter.filterLocation(location, mode) ?: return

        val lat = smoothed.latitude
        val lng = smoothed.longitude

        // 2. Update Live Speedometer
        val speedKmh = if (smoothed.hasSpeed() && smoothed.speed >= 0.6f) {
            (smoothed.speed * 3.6f).toInt().coerceAtLeast(0)
        } else {
            0
        }
        currentSpeedKmh.value = speedKmh

        lastFilteredLocation = smoothed
        onManualLocationUpdate(lat, lng)
        updateCurrentLocationName(getApplication(), lat, lng)
        Log.d("SafeTravelVM", "Adaptive Filtered Location: $lat, $lng (mode=$mode, accuracy=${if (smoothed.hasAccuracy()) smoothed.accuracy else "N/A"}m)")
    }

    fun requestImmediateLocation() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                    onFilteredLocationReceived(loc)
                    Log.d("SafeTravelVM", "Immediate Fused Location acquired: ${loc.latitude}, ${loc.longitude}")
                }
            } catch (e: Exception) {
                Log.e("SafeTravelVM", "Failed immediate location fetch", e)
            }
        }
    }

    fun startContinuousLocationUpdates() {
        requestImmediateLocation()
        if (locationUpdatesJob != null && locationUpdatesJob?.isActive == true) return // Already active

        val context = getApplication<Application>()
        locationUpdatesJob = viewModelScope.launch {
            try {
                // Primary: Google Play Services Fused Location with hardware sensor fusion & deadband filtering
                LocationHelper.getLocationUpdates(context, intervalMs = 3000).collect { loc ->
                    onFilteredLocationReceived(loc)
                }
            } catch (e: Exception) {
                Log.e("SafeTravelVM", "Fused location stream error, falling back to pure GPS", e)
                // Fallback only if FusedClient fails: use pure GPS (NEVER raw cell tower network provider)
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            onFilteredLocationReceived(location)
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    gpsListener = listener
                    try {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 1.0f, listener)
                    } catch (sec: SecurityException) {
                        Log.e("SafeTravelVM", "GPS permission denied", sec)
                    }
                }
            }
        }
    }

    fun stopContinuousLocationUpdates() {
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        gpsListener?.let {
            val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.removeUpdates(it)
            gpsListener = null
        }
        adaptiveLocationFilter.reset()
    }

    fun startLocationTracking(startLat: Double, startLng: Double, destLat: Double, destLng: Double) {
        // simulation logic removed in favor of real GPS
    }

    private fun cancelLocationTracking() {
        locationJob?.cancel()
        locationJob = null
        currentPosition.value = null
        adaptiveLocationFilter.reset()
    }

    override fun onCleared() {
        super.onCleared()
        stopContinuousLocationUpdates()
        locationJob?.cancel()
    }
}
