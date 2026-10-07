package com.safetravel.tracker.supabase

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import com.safetravel.tracker.data.local.AppDatabase
import com.safetravel.tracker.data.local.entities.toOffline
import com.safetravel.tracker.data.local.entities.toSupabase

object SupabaseManager {
    private const val TAG = "SupabaseManager"

    // Session Events Flow for Global Monitoring
    val sessionEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)

    // Authentication State
    var currentSessionToken: String? = null
    var currentRefreshToken: String? = null
    var currentUserId: String? = null

    // Proxy properties for backward compatibility
    val isConfigured: Boolean get() = SupabaseClient.isConfigured
    val authService: SupabaseAuthService? get() = SupabaseClient.authService
    val dbService: SupabaseDbService? get() = SupabaseClient.dbService

    val currentAuthHeader: String
        get() {
            val token = currentSessionToken
            return if (!token.isNullOrBlank() && token.startsWith("eyJ")) {
                "Bearer $token"
            } else {
                "Bearer ${SupabaseClient.supabaseAnonKey}"
            }
        }

    private var appContext: android.content.Context? = null

    fun initSession(context: android.content.Context) {
        appContext = context.applicationContext
        val sharedPrefs = context.getSharedPreferences("safetravel_session", android.content.Context.MODE_PRIVATE)
        SupabaseClient.sharedPrefs = sharedPrefs
        currentSessionToken = sharedPrefs.getString("session_token", null)
        currentRefreshToken = sharedPrefs.getString("refresh_token", null)
        currentUserId = sharedPrefs.getString("user_id", null)
        
        // If connected to live Supabase but session token is a sandbox mock token, clear the stale mock session
        if (isConfigured && currentSessionToken != null && !currentSessionToken!!.startsWith("eyJ")) {
            currentSessionToken = null
            currentRefreshToken = null
            currentUserId = null
            sharedPrefs.edit().apply {
                remove("session_token")
                remove("refresh_token")
                remove("user_id")
                apply()
            }
            ProfileSessionManager.clear(sharedPrefs)
        }

        SupabaseClient.rebuildClient()

        val restoredProfile = ProfileSessionManager.loadProfile(sharedPrefs, currentUserId)
        if (restoredProfile != null) {
            mockProfiles[restoredProfile.id] = restoredProfile
            if (currentUserId == null) currentUserId = restoredProfile.id
        }

        // Load persisted guardians
        val guardiansJson = sharedPrefs.getString("mock_guardians_json", null)
        if (guardiansJson != null) {
            try {
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, Guardian::class.java)
                val list = SupabaseClient.moshi.adapter<List<Guardian>>(listType).fromJson(guardiansJson)
                if (list != null) {
                    mockGuardians.clear()
                    mockGuardians.addAll(list)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse saved guardians", e)
            }
        }
    }

    fun saveSession(token: String, userId: String, profile: SupabaseProfile? = null, refreshToken: String? = null) {
        currentSessionToken = token
        currentUserId = userId
        if (refreshToken != null) currentRefreshToken = refreshToken
        
        SupabaseClient.sharedPrefs?.let { prefs ->
            prefs.edit().apply {
                putString("session_token", token)
                putString("user_id", userId)
                refreshToken?.let { putString("refresh_token", it) }
                apply()
            }
            if (profile != null) {
                ProfileSessionManager.saveProfile(prefs, profile)
            }
        }
        profile?.let { mockProfiles[it.id] = it }

        // Sync FCM Token if available
        val savedFcmToken = SupabaseClient.sharedPrefs?.getString("fcm_token", null)
        if (savedFcmToken != null && isConfigured) {
            syncFcmToken(userId, savedFcmToken)
        }
    }

    fun syncFcmToken(userId: String, token: String) {
        if (!isConfigured) return
        val authHeader = SupabaseClient.getValidAuthHeader()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val dbService = SupabaseClient.dbService ?: return@launch
                val payload = mapOf(
                    "user_id" to userId,
                    "fcm_token" to token,
                    "updated_at" to Calendar.getInstance().apply { timeZone = TimeZone.getTimeZone("UTC") }.time.toString()
                )
                val response = dbService.upsertUserDevice(payload, authHeader)
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM Token synced to Supabase successfully.")
                } else {
                    Log.e(TAG, "FCM Token sync failed: ${response.code()} ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing FCM Token to Supabase", e)
            }
        }
    }

    fun clearSession() {
        currentSessionToken = null
        currentRefreshToken = null
        currentUserId = null
        SupabaseClient.sharedPrefs?.let { prefs ->
            prefs.edit().apply {
                remove("session_token")
                remove("refresh_token")
                remove("user_id")
                apply()
            }
            ProfileSessionManager.clear(prefs)
        }
    }

    fun saveCustomKeys(url: String, key: String) {
        // If current session was a mock sandbox session, clear it so fresh live login occurs
        if (currentSessionToken != null && !currentSessionToken!!.startsWith("eyJ")) {
            clearSession()
        }
        SupabaseClient.sharedPrefs?.edit()?.apply {
            putString("supabase_url_override", url.trim())
            putString("supabase_anon_key_override", key.trim())
            apply()
        }
        SupabaseClient.rebuildClient()
    }

    fun clearCustomKeys() {
        SupabaseClient.sharedPrefs?.edit()?.apply {
            remove("supabase_url_override")
            remove("supabase_anon_key_override")
            apply()
        }
        SupabaseClient.rebuildClient()
    }

    // --- MOCK DATABASE (Simulated Sandbox) ---
    var mockProfiles = mutableMapOf<String, SupabaseProfile>()
    var mockGuardians = mutableListOf<Guardian>()
    var mockTrips = mutableListOf<SupabaseTrip>()
    var mockSosRecords = mutableListOf<SupabaseSosRecord>()
    var mockPointsLogs = mutableListOf<SupabasePointsLog>()
    var mockHomepageBanners = mutableListOf<SupabaseHomepageBanner>()
    var mockSafetyNews = mutableListOf<SupabaseSafetyNews>()
    var mockTripShares = mutableListOf<SupabaseTripShare>()

    init {
        val defaultUserId = "usr-" + UUID.randomUUID().toString().take(6)
        mockProfiles[defaultUserId] = SupabaseProfile(
            id = defaultUserId,
            fullName = "Alex Rivera",
            phoneNumber = "+1 (555) 019-9231",
            isPremium = true,
            tripCredits = 5,
            pointsBalance = 350,
            role = "user"
        )
    }

    // Helper to parse Supabase Auth error JSON into clean human-friendly messages
    fun parseAuthErrorMessage(rawError: String?, defaultMsg: String): String {
        if (rawError.isNullOrBlank()) return defaultMsg
        val lower = rawError.lowercase()
        return when {
            lower.contains("invalid_credentials") || lower.contains("invalid login credentials") ->
                "Invalid email or password. Please check your credentials and try again."
            lower.contains("user_already_exists") || lower.contains("already registered") ->
                "An account with this email already exists. Please log in instead."
            lower.contains("password should be at least") || lower.contains("weak_password") ->
                "Password is too short. It must be at least 6 characters."
            lower.contains("invalid email") || lower.contains("valid email") ->
                "Please enter a valid email address."
            lower.contains("rate limit") || lower.contains("too many requests") ->
                "Too many attempts. Please wait a moment and try again."
            lower.contains("network") || lower.contains("unable to resolve host") || lower.contains("failed to connect") ->
                "Network connection error. Please check your internet."
            else -> {
                // Try extracting 'msg' or 'message' from JSON if available
                try {
                    val msgRegex = Regex("\"(?:msg|message|error_description)\"\\s*:\\s*\"([^\"]+)\"")
                    val match = msgRegex.find(rawError)
                    match?.groups?.get(1)?.value ?: defaultMsg
                } catch (e: Exception) {
                    defaultMsg
                }
            }
        }
    }

    // --- AUTHENTICATION LOGIC ---
    suspend fun signUpWithEmail(email: String, name: String, phone: String, role: String, passwordEntered: String = "SafeTravelSecret123"): Result<SupabaseProfile> {
        if (isConfigured) {
            try {
                val signupBody = SignupRequest(email.trim(), passwordEntered, SignupData(name, phone, role))
                val response = authService?.signUp(signupBody)
                if (response?.isSuccessful == true && response.body() != null) {
                    val body = response.body()!!
                    val userId = body.user?.id ?: ("usr-" + UUID.randomUUID().toString().take(6))
                    val profileObj = SupabaseProfile(id = userId, fullName = name, phoneNumber = phone, role = role)
                    saveSession(body.accessToken ?: "", userId, profileObj, body.refreshToken)
                    try {
                        val createResp = dbService?.createProfile(profileObj, "Bearer ${body.accessToken}")
                        if (createResp?.isSuccessful != true) {
                            val errorBody = createResp?.errorBody()?.string() ?: "No error details"
                            Log.e(TAG, "Profiles insert failed in signup: $errorBody")
                            return Result.failure(Exception("Failed to create user profile row in database: $errorBody"))
                        } else {
                            Log.d(TAG, "Profiles insert succeeded in signup")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Profiles insert exception in signup: ${e.message}", e)
                        return Result.failure(Exception("Database connection failed while creating user profile: ${e.message}"))
                    }
                    return Result.success(profileObj)
                }
                val cleanError = parseAuthErrorMessage(response?.errorBody()?.string(), "Signup failed. Please try again.")
                return Result.failure(Exception(cleanError))
            } catch (e: Exception) { 
                val cleanError = parseAuthErrorMessage(e.message, "Network or server connection failed.")
                return Result.failure(Exception(cleanError)) 
            }
        } else {
            val newUserId = "usr-" + UUID.randomUUID().toString().take(6)
            val profile = SupabaseProfile(id = newUserId, fullName = name, phoneNumber = phone, role = role, pointsBalance = 50)
            saveSession("sim-token", newUserId, profile)
            return Result.success(profile)
        }
    }

    suspend fun loginWithEmail(email: String, passwordEntered: String = "SafeTravelSecret123"): Result<SupabaseProfile> {
        val emailClean = email.trim()
        if (isConfigured) {
            try {
                val response = authService?.login(LoginRequest(emailClean, passwordEntered))
                if (response?.isSuccessful == true && response.body() != null) {
                    val authData = response.body()!!
                    val userId = authData.user?.id ?: return Result.failure(Exception("User ID missing"))
                    val authHeader = "Bearer ${authData.accessToken}"
                    val pResp = dbService?.getProfile("eq.$userId", authHeader)
                    if (pResp?.isSuccessful == true && !pResp.body().isNullOrEmpty()) {
                        val profile = pResp.body()!!.first()
                        saveSession(authData.accessToken!!, userId, profile, authData.refreshToken)
                        return Result.success(profile)
                    }
                    // Profile row is missing (e.g. from a past failed signup). Let's AUTO-HEAL by inserting it right now!
                    val defProfile = SupabaseProfile(id = userId, fullName = emailClean.substringBefore("@"), phoneNumber = "")
                    try {
                        val healResp = dbService?.createProfile(defProfile, authHeader)
                        if (healResp?.isSuccessful == true && !healResp.body().isNullOrEmpty()) {
                            val healedProfile = healResp.body()!!.first()
                            saveSession(authData.accessToken!!, userId, healedProfile, authData.refreshToken)
                            return Result.success(healedProfile)
                        } else {
                            val healErr = healResp?.errorBody()?.string() ?: "Unknown auto-heal error"
                            Log.e(TAG, "Profile Auto-healing failed on login: $healErr")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Profile Auto-healing exception on login: ${e.message}", e)
                    }
                    saveSession(authData.accessToken!!, userId, defProfile, authData.refreshToken)
                    return Result.success(defProfile)
                }
                val cleanError = parseAuthErrorMessage(response?.errorBody()?.string(), "Invalid login credentials.")
                return Result.failure(Exception(cleanError))
            } catch (e: Exception) { 
                val cleanError = parseAuthErrorMessage(e.message, "Network or server connection failed.")
                return Result.failure(Exception(cleanError)) 
            }
        } else {
            val existing = mockProfiles.values.firstOrNull { it.fullName.lowercase() == emailClean.substringBefore("@").lowercase() }
            if (existing != null) {
                saveSession("sim-token", existing.id, existing)
                return Result.success(existing)
            }
            val newId = "usr-" + UUID.randomUUID().toString().take(6)
            val p = SupabaseProfile(id = newId, fullName = emailClean.substringBefore("@"), phoneNumber = "+1 555-0100", pointsBalance = 100)
            saveSession("sim-token", newId, p)
            return Result.success(p)
        }
    }

    // --- DATABASE & PROFILE LOGIC ---
    suspend fun fetchProfile(userId: String): Result<SupabaseProfile> {
        if (isConfigured) {
            try {
                val authHeader = SupabaseClient.getValidAuthHeader()
                val response = dbService?.getProfile("eq.$userId", authHeader)
                if (response?.isSuccessful == true && !response.body().isNullOrEmpty()) {
                    val fetchedProfile = response.body()!!.first()
                    mockProfiles[userId] = fetchedProfile
                    SupabaseClient.sharedPrefs?.let { prefs ->
                        ProfileSessionManager.saveProfile(prefs, fetchedProfile)
                    }
                    return Result.success(fetchedProfile)
                }
                if (response?.isSuccessful == true && response.body().isNullOrEmpty()) {
                    // Profile row is missing in the database - let's auto-heal it!
                    val defProfile = SupabaseProfile(id = userId, fullName = "Safe Travel User", phoneNumber = "")
                    try {
                        val healResp = dbService?.createProfile(defProfile, authHeader)
                        if (healResp?.isSuccessful == true && !healResp.body().isNullOrEmpty()) {
                            val healed = healResp.body()!!.first()
                            mockProfiles[userId] = healed
                            SupabaseClient.sharedPrefs?.let { prefs ->
                                ProfileSessionManager.saveProfile(prefs, healed)
                            }
                            return Result.success(healed)
                        }
                    } catch (healEx: Exception) {
                        Log.e(TAG, "Auto-healing fetchProfile creation failed: ${healEx.message}", healEx)
                    }
                    return Result.success(defProfile)
                }
                val code = response?.code()
                val errBody = response?.errorBody()?.string() ?: "No error body"
                Log.w(TAG, "Profile fetch API returned ($code): $errBody. Falling back to cached profile.")
                val localFallback = mockProfiles[userId] ?: SupabaseClient.sharedPrefs?.let { ProfileSessionManager.loadProfile(it, userId) }
                if (localFallback != null) {
                    return Result.success(localFallback)
                }
                return Result.failure(Exception("Profile fetch failed ($code): $errBody"))
            } catch (e: Exception) { 
                Log.e(TAG, "Exception during fetchProfile", e)
                val localFallback = mockProfiles[userId] ?: SupabaseClient.sharedPrefs?.let { ProfileSessionManager.loadProfile(it, userId) }
                if (localFallback != null) {
                    return Result.success(localFallback)
                }
                return Result.failure(e) 
            }
        }
        return mockProfiles[userId]?.let { Result.success(it) } ?: Result.failure(Exception("Local Profile Not Initialized"))
    }

    suspend fun updateProfileFields(userId: String, updates: Map<String, Any>): Result<SupabaseProfile> {
        if (isConfigured) {
            try {
                val authHeader = SupabaseClient.getValidAuthHeader()
                val response = dbService?.updateProfile("eq.$userId", updates, authHeader)
                if (response?.isSuccessful == true && !response.body().isNullOrEmpty()) {
                    val p = response.body()!!.first()
                    saveSession(currentSessionToken ?: "", userId, p)
                    return Result.success(p)
                } else if (response != null && !response.isSuccessful) {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown Supabase Error"
                    Log.e(TAG, "Supabase updateProfile error: $errorMsg")
                    return Result.failure(Exception("Database error: $errorMsg"))
                }
            } catch (e: Exception) { 
                Log.e(TAG, "Exception updating profile in Supabase", e)
                return Result.failure(e) 
            }
        }
        
        val oldP = mockProfiles[userId] ?: return Result.failure(Exception("No profile loaded"))
        var updated = oldP
        
        // Manual mapping of all fields for Mock mode support
        updates.forEach { (key, value) ->
            updated = when (key) {
                "full_name" -> updated.copy(fullName = value as String)
                "phone_number" -> updated.copy(phoneNumber = value as String)
                "is_premium" -> updated.copy(isPremium = value as Boolean)
                "points_balance" -> updated.copy(pointsBalance = value as Int)
                "trip_credits" -> updated.copy(tripCredits = value as Int)
                "dob" -> updated.copy(dob = value as String)
                "gender" -> updated.copy(gender = value as String)
                "nid_passport" -> updated.copy(nidPassport = value as String)
                "occupation" -> updated.copy(occupation = value as String)
                "present_address" -> updated.copy(presentAddress = value as String)
                "permanent_address" -> updated.copy(permanentAddress = value as String)
                "blood_group" -> updated.copy(bloodGroup = value as String)
                "allergies" -> updated.copy(allergies = value as String)
                "chronic_conditions" -> updated.copy(chronicConditions = value as String)
                "current_medications" -> updated.copy(currentMedications = value as String)
                "height_weight" -> updated.copy(heightWeight = value as String)
                "special_needs" -> updated.copy(specialNeeds = value as String)
                else -> updated
            }
        }

        saveSession(currentSessionToken ?: "sim-token", userId, updated)
        return Result.success(updated)
    }

    private fun getIsoInstantString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    // --- TRIP MANAGEMENT ---
    suspend fun startTrip(
        userId: String, 
        start: String, 
        end: String, 
        mode: String, 
        battery: Int, 
        vehicle: String, 
        vehicleDesc: String? = null, 
        startCoords: String? = null, 
        endCoords: String? = null, 
        deviceModel: String? = null, 
        notifiedGuardians: List<String> = emptyList(), 
        plannedRouteLog: List<Map<String, Double>>? = null,
        estimatedDistanceKm: Double? = null,
        estimatedDurationMin: Double? = null,
        vehiclePhotoUrl: String? = null
    ): Result<SupabaseTrip> {
        val trackingCode = "ST-" + UUID.randomUUID().toString().take(6).uppercase()
        val isMockUser = userId == "00000000-0000-0000-0000-000000000001" || userId.startsWith("usr-")
        val nowIso = getIsoInstantString()
        if (isConfigured && !isMockUser) {
            try {
                val body = mutableMapOf<String, Any>(
                    "user_id" to userId, 
                    "start_address" to start, 
                    "end_address" to end, 
                    "safety_status" to "ongoing", 
                    "transport_mode" to mode, 
                    "start_battery_level" to battery, 
                    "end_battery_level" to battery,
                    "vehicle_plate_number" to vehicle,
                    "tracking_code" to trackingCode
                )
                if (notifiedGuardians.isNotEmpty()) {
                    body["notified_guardians"] = notifiedGuardians
                }
                if (!plannedRouteLog.isNullOrEmpty()) {
                    body["planned_route_log"] = plannedRouteLog
                }
                estimatedDistanceKm?.let {
                    body["estimated_distance_km"] = it
                    body["total_distance"] = it // Store initial planned distance
                }
                estimatedDurationMin?.let {
                    body["estimated_duration_min"] = it
                }
                vehicleDesc?.let { body["vehicle_description"] = it }
                vehiclePhotoUrl?.let { body["vehicle_photo_url"] = it }
                startCoords?.let { coordsStr ->
                    body["start_coords"] = coordsStr
                    val parts = coordsStr.split(",").mapNotNull { it.trim().toDoubleOrNull() }
                    if (parts.size >= 2) {
                        val lat = parts[0]
                        val lng = parts[1]
                        body["current_lat"] = lat
                        body["current_lng"] = lng
                        body["route_path_log"] = listOf(mapOf("lat" to lat, "lng" to lng))
                    }
                }
                endCoords?.let { body["end_coords"] = it }
                deviceModel?.let { body["device_model"] = it }
                val response = dbService?.createTrip(body, "Bearer ${currentSessionToken ?: SupabaseClient.supabaseAnonKey}")
                if (response?.isSuccessful == true && !response.body().isNullOrEmpty()) {
                    val remoteTrip = response.body()!!.first()
                    appContext?.let { ctx ->
                        try { AppDatabase.getDatabase(ctx).safeTravelDao().insertTrip(remoteTrip.toOffline()) } catch (e: Exception) {}
                    }
                    return Result.success(remoteTrip)
                }
                val code = response?.code()
                val errBody = response?.errorBody()?.string() ?: "No error body"
                android.util.Log.e("SupabaseManager", "Remote startTrip failed. HTTP Code: $code, Error: $errBody")
                return Result.failure(Exception("Unable to start journey on server. Please check your internet connection."))
            } catch (e: Exception) { 
                android.util.Log.e("SupabaseManager", "Remote startTrip exception: ${e.localizedMessage}", e)
                return Result.failure(e)
            }
        }
        val t = SupabaseTrip(
            id = "T-" + UUID.randomUUID().toString().take(5).uppercase(),
            userId = userId,
            startAddress = start,
            endAddress = end,
            startCoords = startCoords,
            endCoords = endCoords,
            transportMode = mode,
            startBatteryLevel = battery,
            endBatteryLevel = battery,
            deviceModel = deviceModel ?: "Simulated Device",
            status = "ongoing",
            createdAt = nowIso,
            vehiclePlateNumber = vehicle,
            vehicleDescription = vehicleDesc,
            routePathLog = emptyList(),
            plannedRouteLog = plannedRouteLog,
            totalDistance = estimatedDistanceKm,
            estimatedDistanceKm = estimatedDistanceKm,
            estimatedDurationMin = estimatedDurationMin,
            trackingCode = trackingCode,
            notifiedGuardians = notifiedGuardians
        )
        mockTrips.add(t)
        appContext?.let { ctx ->
            try { AppDatabase.getDatabase(ctx).safeTravelDao().insertTrip(t.toOffline()) } catch (e: Exception) {}
        }
        return Result.success(t)
    }

    suspend fun endTrip(
        tripId: String, 
        endBattery: Int? = null, 
        totalDistance: Double? = null,
        finalEndAddress: String? = null,
        finalEndCoords: String? = null
    ): Result<SupabaseTrip> {
        val now = getIsoInstantString()
        val isMockTrip = tripId.startsWith("T-") || mockTrips.any { it.id == tripId }
        if (isConfigured && !isMockTrip) {
            try {
                val updates = mutableMapOf<String, Any>(
                    "safety_status" to "completed",
                    "end_time" to now
                )
                endBattery?.let { updates["end_battery_level"] = it }
                totalDistance?.let { updates["total_distance"] = it }
                finalEndAddress?.let { updates["end_address"] = it }
                finalEndCoords?.let { updates["end_coords"] = it }
                val resp = dbService?.updateTrip("eq.$tripId", updates, "Bearer $currentSessionToken")
                if (resp?.isSuccessful == true && !resp.body().isNullOrEmpty()) {
                    val updatedTrip = resp.body()!!.first()
                    appContext?.let { ctx ->
                        try { AppDatabase.getDatabase(ctx).safeTravelDao().insertTrip(updatedTrip.toOffline()) } catch (e: Exception) {}
                    }
                    return Result.success(updatedTrip)
                }
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                android.util.Log.e("SupabaseManager", "Remote endTrip failed. HTTP Code: $code, Error: $errBody")
                return Result.failure(Exception("Unable to complete journey on server. Please check your connection."))
            } catch (e: Exception) { 
                android.util.Log.e("SupabaseManager", "Remote endTrip exception: ${e.localizedMessage}", e)
                return Result.failure(e)
            }
        }
        val idx = mockTrips.indexOfFirst { it.id == tripId }
        if (idx != -1) {
            val existing = mockTrips[idx]
            val u = existing.copy(
                status = "completed", 
                endBatteryLevel = endBattery, 
                endTime = now, 
                totalDistance = totalDistance,
                endAddress = finalEndAddress ?: existing.endAddress,
                endCoords = finalEndCoords ?: existing.endCoords
            )
            mockTrips[idx] = u
            appContext?.let { ctx ->
                try { AppDatabase.getDatabase(ctx).safeTravelDao().insertTrip(u.toOffline()) } catch (e: Exception) {}
            }
            return Result.success(u)
        }
        // Fallback for untracked active trips to update smoothly
        val fallbackTrip = SupabaseTrip(
            id = tripId,
            userId = currentUserId ?: "00000000-0000-0000-0000-000000000001",
            status = "completed",
            endBatteryLevel = endBattery,
            endTime = now,
            totalDistance = totalDistance,
            endAddress = finalEndAddress,
            endCoords = finalEndCoords
        )
        appContext?.let { ctx ->
            try { AppDatabase.getDatabase(ctx).safeTravelDao().insertTrip(fallbackTrip.toOffline()) } catch (e: Exception) {}
        }
        return Result.success(fallbackTrip)
    }

    suspend fun triggerSos(tripId: String): Result<SupabaseSosRecord> {
        val isMockTrip = tripId.startsWith("T-") || mockTrips.any { it.id == tripId }
        val nowIso = getIsoInstantString()
        if (isConfigured && !isMockTrip) {
            try {
                val auth = currentAuthHeader
                dbService?.updateTrip(
                    "eq.$tripId", 
                    mapOf(
                        "safety_status" to "sos",
                        "sos_triggered_at" to nowIso
                    ), 
                    auth
                )
                val resp = dbService?.createSosRecord(mapOf("trip_id" to tripId, "status" to "active"), auth)
                if (resp?.isSuccessful == true && !resp.body().isNullOrEmpty()) {
                    return Result.success(resp.body()!!.first())
                }
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: ""
                android.util.Log.w("SupabaseManager", "createSosRecord warning ($code): $errBody")
                return Result.success(
                    SupabaseSosRecord(
                        id = "S-" + UUID.randomUUID().toString().take(8),
                        tripId = tripId,
                        status = "active",
                        triggeredAt = nowIso
                    )
                )
            } catch (e: Exception) { 
                android.util.Log.e("SupabaseManager", "Remote triggerSos exception: ${e.localizedMessage}", e)
                return Result.success(
                    SupabaseSosRecord(
                        id = "S-" + UUID.randomUUID().toString().take(8),
                        tripId = tripId,
                        status = "active",
                        triggeredAt = nowIso
                    )
                )
            }
        }
        val idx = mockTrips.indexOfFirst { it.id == tripId }
        if (idx != -1) mockTrips[idx] = mockTrips[idx].copy(status = "sos")
        val r = SupabaseSosRecord(id = "S-" + UUID.randomUUID().toString().take(5).uppercase(), tripId = tripId, status = "active", triggeredAt = "Now")
        mockSosRecords.add(r); return Result.success(r)
    }

    suspend fun appendSosActivityLog(tripId: String, logEntry: Map<String, Any>): Result<Unit> {
        val isMockTrip = tripId.startsWith("T-") || mockTrips.any { it.id == tripId }
        if (isConfigured && !isMockTrip) {
            try {
                val auth = currentAuthHeader
                val tripResp = dbService?.getTripById("eq.$tripId", auth)
                val currentLogs = tripResp?.body()?.firstOrNull()?.sosActivityLogs?.toMutableList() ?: mutableListOf()
                currentLogs.add(logEntry)
                dbService?.updateTrip("eq.$tripId", mapOf("sos_activity_logs" to currentLogs), auth)
                return Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseManager", "appendSosActivityLog failed: ${e.message}", e)
                return Result.failure(e)
            }
        }
        val idx = mockTrips.indexOfFirst { it.id == tripId }
        if (idx != -1) {
            val cur = mockTrips[idx].sosActivityLogs?.toMutableList() ?: mutableListOf()
            cur.add(logEntry)
            mockTrips[idx] = mockTrips[idx].copy(sosActivityLogs = cur)
        }
        return Result.success(Unit)
    }

    suspend fun updateTripSafetyStatus(tripId: String, status: String): Result<Unit> {
        val isMockTrip = tripId.startsWith("T-") || mockTrips.any { it.id == tripId }
        if (isConfigured && !isMockTrip) {
            try {
                val auth = currentAuthHeader
                dbService?.updateTrip("eq.$tripId", mapOf("safety_status" to status), auth)
                return Result.success(Unit)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        val idx = mockTrips.indexOfFirst { it.id == tripId }
        if (idx != -1) mockTrips[idx] = mockTrips[idx].copy(status = status)
        return Result.success(Unit)
    }

    // --- UTILITY FETCHERS ---
    suspend fun getLiveSosRecords(): Result<List<SupabaseSosRecord>> {
        if (isConfigured) {
            try {
                val resp = dbService?.getSosRecords("Bearer $currentSessionToken")
                if (resp?.isSuccessful == true && resp.body() != null) return Result.success(resp.body()!!)
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to fetch SOS records."))
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.success(emptyList())
    }

    suspend fun getLiveHomepageBanners(): Result<List<SupabaseHomepageBanner>> {
        if (isConfigured) {
            try {
                val auth = SupabaseClient.getValidAuthHeader()
                val resp = dbService?.getHomepageBanners(auth)
                if (resp?.isSuccessful == true && resp.body() != null) return Result.success(resp.body()!!)
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to fetch banners."))
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.success(emptyList())
    }

    suspend fun getSafetyNews(): Result<List<SupabaseSafetyNews>> {
        if (isConfigured) {
            try {
                val auth = SupabaseClient.getValidAuthHeader()
                val resp = dbService?.getSafetyNews(auth)
                if (resp?.isSuccessful == true && resp.body() != null) return Result.success(resp.body()!!)
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to fetch news."))
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.success(emptyList())
    }

    suspend fun fetchUserTrips(userId: String): Result<List<SupabaseTrip>> {
        if (isConfigured) {
            try {
                val resp = dbService?.getTrips("eq.$userId", "Bearer $currentSessionToken")
                if (resp?.isSuccessful == true && resp.body() != null) {
                    val remoteTrips = resp.body()!!
                    appContext?.let { ctx ->
                        try {
                            val dao = AppDatabase.getDatabase(ctx).safeTravelDao()
                            remoteTrips.forEach { dao.insertTrip(it.toOffline()) }
                        } catch (e: Exception) {}
                    }
                    return Result.success(remoteTrips)
                }
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to fetch journeys."))
            } catch (e: Exception) { 
                Log.w(TAG, "Failed to fetch remote trips, loading local Room trips: ${e.localizedMessage}")
            }
            val localTrips = appContext?.let { ctx ->
                try {
                    AppDatabase.getDatabase(ctx).safeTravelDao().getTripsForUser(userId).map { it.toSupabase() }
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
            return Result.success(localTrips)
        }

        val localTrips = appContext?.let { ctx ->
            try {
                AppDatabase.getDatabase(ctx).safeTravelDao().getTripsForUser(userId).map { it.toSupabase() }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        return Result.success(localTrips.filter { it.userId == userId })
    }

    suspend fun logPoints(userId: String, points: Int, source: String): Result<SupabasePointsLog> {
        if (isConfigured) {
            try {
                val resp = dbService?.createPointsLog(mapOf("user_id" to userId, "points_added" to points, "source" to source), "Bearer $currentSessionToken")
                if (resp?.isSuccessful == true && !resp.body().isNullOrEmpty()) return Result.success(resp.body()!!.first())
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to record loyalty points."))
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.failure(Exception("Database connection unavailable."))
    }

    // Remote Configs Cache & Helper for Dynamic Point Control
    private var cachedRemoteConfigs: Map<String, String>? = null

    fun getRemoteConfig(key: String, defaultVal: String = ""): String {
        return cachedRemoteConfigs?.get(key) ?: getCachedConfig(key, defaultVal)
    }

    fun getPointConfig(key: String, defaultVal: Int): Int {
        val strVal = getRemoteConfig(key, "")
        return strVal.toIntOrNull() ?: defaultVal
    }

    suspend fun fetchPointsLogs(userId: String): Result<List<SupabasePointsLog>> {
        if (isConfigured) {
            try {
                val resp = dbService?.getPointsLogs(
                    userAuth = currentAuthHeader,
                    userIdQuery = "eq.$userId",
                    order = "timestamp.desc",
                    limit = 25
                )
                if (resp?.isSuccessful == true) {
                    val list = resp.body() ?: emptyList()
                    return Result.success(list)
                }
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to fetch points history."))
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.success(emptyList())
    }

    suspend fun redeemPoints(
        userId: String,
        cost: Int,
        benefitType: String,
        currentPoints: Int,
        currentCredits: Int
    ): Result<Boolean> {
        if (currentPoints < cost) {
            return Result.failure(Exception("Insufficient loyalty points (Required: $cost PTS)"))
        }
        val newBalance = currentPoints - cost
        val updates = mutableMapOf<String, Any>("points_balance" to newBalance)
        var sourceLabel = "Points Redeemed: $benefitType"

        when (benefitType) {
            "premium_7d", "premium_30d" -> {
                updates["is_premium"] = true
                sourceLabel = if (benefitType == "premium_7d") "Unlocked 7 Days Premium Access" else "Unlocked 30 Days Premium Access"
            }
            "credit" -> {
                updates["trip_credits"] = currentCredits + 1
                sourceLabel = "Redeemed 1 Emergency Trip Credit"
            }
        }

        val updateResult = updateProfileFields(userId, updates)
        if (updateResult.isFailure) {
            return Result.failure(updateResult.exceptionOrNull() ?: Exception("Failed to update profile"))
        }

        logPoints(userId, -cost, sourceLabel)
        return Result.success(true)
    }

    val mockSafetyAudioLogs = mutableListOf<SupabaseSafetyAudioLog>()

    suspend fun fetchSafetyAudioLogs(userId: String): Result<List<SupabaseSafetyAudioLog>> {
        if (isConfigured) {
            try {
                val resp = dbService?.getSafetyAudioLogs(
                    userIdQuery = "eq.$userId",
                    userAuth = "Bearer $currentSessionToken",
                    isDeletedQuery = "neq.true"
                )
                if (resp?.isSuccessful == true) {
                    val list = (resp.body() ?: emptyList()).filter { it.isDeletedByUser != true }
                    return Result.success(list)
                }
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to fetch audio logs."))
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.success(emptyList())
    }

    suspend fun softDeleteSafetyAudioLog(logId: String): Result<Boolean> {
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        val body = mapOf<String, Any>(
            "is_deleted_by_user" to true,
            "user_deleted_at" to nowIso
        )
        if (isConfigured) {
            try {
                val resp = dbService?.updateSafetyAudioLog("eq.$logId", body, "Bearer $currentSessionToken")
                if (resp?.isSuccessful == true) {
                    return Result.success(true)
                }
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to delete audio log."))
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(Exception("Database connection unavailable."))
    }

    suspend fun createSafetyAudioLog(
        userId: String?,
        tripId: String?,
        audioUrl: String,
        cloudinaryPublicId: String? = null,
        durationSec: Int = 0,
        fileSizeBytes: Long = 0L,
        recordedAddress: String? = null,
        recordedCoords: String? = null,
        sourceTrigger: String = "manual_toolkit"
    ): Result<SupabaseSafetyAudioLog> {
        val body = mutableMapOf<String, Any>(
            "audio_url" to audioUrl,
            "duration_sec" to durationSec,
            "file_size_bytes" to fileSizeBytes,
            "source_trigger" to sourceTrigger
        )
        userId?.let { body["user_id"] = it }
        tripId?.let { body["trip_id"] = it }
        cloudinaryPublicId?.let { body["cloudinary_public_id"] = it }
        recordedAddress?.let { body["recorded_address"] = it }
        recordedCoords?.let { body["recorded_coords"] = it }

        if (isConfigured) {
            try {
                val resp = dbService?.createSafetyAudioLog(body, "Bearer $currentSessionToken")
                if (resp?.isSuccessful == true && !resp.body().isNullOrEmpty()) {
                    val created = resp.body()!!.first()
                    return Result.success(created)
                }
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to save audio log."))
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.failure(Exception("Database connection unavailable."))
    }

    suspend fun fetchGuardians(userId: String): Result<List<Guardian>> {
        if (isConfigured) {
            try {
                val resp = dbService?.getGuardians("eq.$userId", "Bearer $currentSessionToken")
                if (resp?.isSuccessful == true) return Result.success(resp.body() ?: emptyList())
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to fetch guardians."))
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.success(emptyList())
    }

    suspend fun saveGuardian(guardian: Guardian): Result<Guardian> {
        if (isConfigured) {
            try {
                val resp = dbService?.createGuardian(guardian, "Bearer $currentSessionToken")
                if (resp?.isSuccessful == true && !resp.body().isNullOrEmpty()) return Result.success(resp.body()!!.first())
                val code = resp?.code()
                val errBody = resp?.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("Failed to save guardian."))
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.failure(Exception("Database connection unavailable."))
    }

    suspend fun deleteGuardian(id: String): Result<Unit> {
        if (isConfigured) {
            try {
                val resp = dbService?.deleteGuardian("eq.$id", "Bearer $currentSessionToken")
                if (resp?.isSuccessful == true) return Result.success(Unit)
            } catch (e: Exception) { return Result.failure(e) }
        }
        mockGuardians.removeAll { it.id == id }
        saveGuardianSync()
        return Result.success(Unit)
    }

    suspend fun submitFeedback(feedback: SupabaseFeedback): Result<SupabaseFeedback> {
        if (isConfigured) {
            try {
                val resp = dbService?.createFeedback(feedback, "Bearer $currentSessionToken")
                if (resp?.isSuccessful == true && !resp.body().isNullOrEmpty()) return Result.success(resp.body()!!.first())
                return Result.failure(Exception(resp?.errorBody()?.string() ?: "Feedback submission failed"))
            } catch (e: Exception) { return Result.failure(e) }
        }
        // In mock mode, we just return success as if it was saved
        return Result.success(feedback.copy(id = UUID.randomUUID().toString(), createdAt = "Now"))
    }

    // --- TRIP SHARING LOGIC ---
    suspend fun shareTrip(tripId: String, sharedBy: String, phones: List<String>): Result<Unit> {
        if (isConfigured) {
            try {
                val authHeader = "Bearer $currentSessionToken"
                val profileResp = dbService?.getProfile("eq.$sharedBy", authHeader)
                val sharingUserName = profileResp?.body()?.firstOrNull()?.fullName ?: "Someone"

                phones.forEach { phone ->
                    val cleanPhone = phone.trim().replace("[\\s\\-\\(\\)]".toRegex(), "")
                    val lastDigits = if (cleanPhone.length >= 10) cleanPhone.takeLast(10) else cleanPhone
                    
                    var matchedGuardianUserId: String? = null
                    
                    // Attempt to locate guardian user_id by matching normalized phone
                    if (cleanPhone.length >= 6) {
                        try {
                            val filter = "(phone_number.eq.$cleanPhone,phone_number.eq.$phone,phone_number.ilike.%$lastDigits%,phone_number.ilike.*$lastDigits*)"
                            val profilesResp = dbService?.getProfilesWithFilter(filter, authHeader)
                            val matchedProfile = profilesResp?.body()?.firstOrNull()
                            if (matchedProfile != null) {
                                matchedGuardianUserId = matchedProfile.id
                            } else {
                                // Direct fallback
                                val directResp = dbService?.getProfileByPhone("eq.$cleanPhone", authHeader)
                                matchedGuardianUserId = directResp?.body()?.firstOrNull()?.id
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Guardian profile lookup warning: ${e.message}")
                        }
                    }

                    val body = mutableMapOf<String, Any>(
                        "trip_id" to tripId,
                        "shared_by" to sharedBy,
                        "shared_with_phone" to phone
                    )
                    if (matchedGuardianUserId != null) {
                        body["shared_with_user_id"] = matchedGuardianUserId
                    }

                    val shareResp = dbService?.createTripShare(body, authHeader)
                    if (shareResp != null && shareResp.isSuccessful) {
                        Log.d(TAG, "Successfully shared trip $tripId with $phone (Guardian UID: $matchedGuardianUserId)")
                    } else {
                        val errorStr = shareResp?.errorBody()?.string()
                        Log.w(TAG, "Failed to share trip $tripId with $phone. Code: ${shareResp?.code()}, Error: $errorStr")
                    }

                    // Lookup Guardian's profiles by phone or matched user ID to trigger FCM Push Notification
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            if (matchedGuardianUserId != null) {
                                val devicesResponse = dbService?.getUserDevices("eq.$matchedGuardianUserId", authHeader)
                                val devices = devicesResponse?.body()
                                if (!devices.isNullOrEmpty()) {
                                    devices.forEach { deviceMap ->
                                        val token = deviceMap["fcm_token"] as? String
                                        if (!token.isNullOrBlank()) {
                                            Log.d(TAG, "Found guardian FCM token: $token. Triggering push alert...")
                                            com.safetravel.tracker.notification.FcmNotificationSender.sendNotificationToGuardian(
                                                guardianFcmToken = token,
                                                title = "Live Trip Shared! 🛡️",
                                                body = "$sharingUserName has started a safe trip and shared their live location with you.",
                                                tripId = tripId
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error triggering push alert: ", e)
                        }
                    }
                }
                return Result.success(Unit)
            } catch (e: Exception) { return Result.failure(e) }
        } else {
            phones.forEach { phone ->
                val trip = mockTrips.find { it.id == tripId }
                val profile = mockProfiles[sharedBy]
                mockTripShares.add(SupabaseTripShare(
                    tripId = tripId,
                    sharedBy = sharedBy,
                    sharedWithPhone = phone,
                    tripDetails = trip,
                    sharedByProfile = profile
                ))
            }
            return Result.success(Unit)
        }
    }

    suspend fun fetchIncomingShares(phone: String, explicitUserId: String? = null): Result<List<SupabaseTripShare>> {
        if (isConfigured) {
            try {
                val authHeader = currentAuthHeader
                val rawPhone = phone.trim()
                val cleanPhone = rawPhone.replace("[^0-9+]".toRegex(), "")
                val digitsOnly = rawPhone.replace("[^0-9]".toRegex(), "")
                val last10 = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly
                val last9 = if (digitsOnly.length >= 9) digitsOnly.takeLast(9) else digitsOnly
                val userId = explicitUserId ?: currentUserId

                val sharesList = mutableListOf<SupabaseTripShare>()

                // 1. Direct query by user_id first (most accurate)
                if (!userId.isNullOrBlank()) {
                    try {
                        val userSharesResp = dbService?.getTripSharesByUserId("eq.$userId", authHeader)
                        if (userSharesResp?.isSuccessful == true && !userSharesResp.body().isNullOrEmpty()) {
                            sharesList.addAll(userSharesResp.body()!!)
                        } else {
                            val plainResp = dbService?.getTripSharesByUserId("eq.$userId", authHeader, select = "*")
                            if (plainResp?.isSuccessful == true && !plainResp.body().isNullOrEmpty()) {
                                sharesList.addAll(plainResp.body()!!)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "User ID trip share fetch failed: ${e.message}")
                    }
                }

                // 2. Direct query by raw phone
                if (rawPhone.isNotBlank()) {
                    try {
                        val phoneSharesResp = dbService?.getTripShares("eq.$rawPhone", authHeader)
                        if (phoneSharesResp?.isSuccessful == true && !phoneSharesResp.body().isNullOrEmpty()) {
                            sharesList.addAll(phoneSharesResp.body()!!)
                        } else {
                            val plainResp = dbService?.getTripShares("eq.$rawPhone", authHeader, select = "*")
                            if (plainResp?.isSuccessful == true && !plainResp.body().isNullOrEmpty()) {
                                sharesList.addAll(plainResp.body()!!)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Raw phone trip share fetch failed: ${e.message}")
                    }
                }

                // 3. Query with clean phone (e.g. +8801779980886 or 01779980886)
                if (cleanPhone.isNotBlank() && cleanPhone != rawPhone) {
                    try {
                        val phoneSharesResp = dbService?.getTripShares("eq.$cleanPhone", authHeader, select = "*")
                        if (phoneSharesResp?.isSuccessful == true && !phoneSharesResp.body().isNullOrEmpty()) {
                            sharesList.addAll(phoneSharesResp.body()!!)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Clean phone trip share fetch failed: ${e.message}")
                    }
                }

                // 4. Query with leading + or leading 0 format variations
                val formatVariations = mutableSetOf<String>()
                if (digitsOnly.isNotBlank()) {
                    formatVariations.add("+$digitsOnly")
                    formatVariations.add("0$last10")
                    formatVariations.add(digitsOnly)
                }
                for (formatted in formatVariations) {
                    if (formatted != rawPhone && formatted != cleanPhone) {
                        try {
                            val resp = dbService?.getTripShares("eq.$formatted", authHeader, select = "*")
                            if (resp?.isSuccessful == true && !resp.body().isNullOrEmpty()) {
                                sharesList.addAll(resp.body()!!)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Formatted phone $formatted trip share fetch failed: ${e.message}")
                        }
                    }
                }

                // 5. Query with like wildcard on last digits if still empty
                if (sharesList.isEmpty() && last10.isNotBlank()) {
                    try {
                        val ilikeResp = dbService?.getTripShares("ilike.*$last10*", authHeader, select = "*")
                        if (ilikeResp?.isSuccessful == true && !ilikeResp.body().isNullOrEmpty()) {
                            sharesList.addAll(ilikeResp.body()!!)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Wildcard phone trip share fetch failed: ${e.message}")
                    }
                }

                val distinctShares = sharesList.distinctBy { it.id }

                // 6. Ensure tripDetails and sharedByProfile are populated for each share
                val populatedShares = distinctShares.map { share ->
                    var populatedShare = share
                    // Populate trip details if missing
                    if (populatedShare.tripDetails == null) {
                        try {
                            val tripResp = dbService?.getTripById("eq.${share.tripId}", authHeader)
                            val tripObj = tripResp?.body()?.firstOrNull()
                            if (tripObj != null) {
                                populatedShare = populatedShare.copy(tripDetails = tripObj)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed populating tripDetails for share ${share.id}: ${e.message}")
                        }
                    }
                    // Populate sharedByProfile if missing
                    if (populatedShare.sharedByProfile == null && share.sharedBy.isNotBlank()) {
                        try {
                            val profileResp = dbService?.getProfile("eq.${share.sharedBy}", authHeader)
                            val profileObj = profileResp?.body()?.firstOrNull()
                            if (profileObj != null) {
                                populatedShare = populatedShare.copy(sharedByProfile = profileObj)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed populating profile for share ${share.id}: ${e.message}")
                        }
                    }
                    populatedShare
                }

                return Result.success(populatedShares)
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.success(emptyList())
    }

    suspend fun fetchTripById(tripId: String): Result<SupabaseTrip> {
        if (isConfigured) {
            try {
                val resp = dbService?.getTripById("eq.$tripId", currentAuthHeader)
                if (resp?.isSuccessful == true && !resp.body().isNullOrEmpty()) return Result.success(resp.body()!!.first())
                return Result.failure(Exception("Trip record not found."))
            } catch (e: Exception) { return Result.failure(e) }
        }
        return Result.failure(Exception("Database connection unavailable."))
    }

    // --- REMOTE CONFIG ARCHITECTURE (Mapbox, Cloudinary, etc.) ---
    suspend fun fetchRemoteConfigs(): Result<Map<String, String>> {
        val cached = getCachedRemoteConfigs()
        if (isConfigured) {
            try {
                val resp = dbService?.getRemoteConfigs(currentAuthHeader)
                if (resp?.isSuccessful == true && resp.body() != null) {
                    val map = resp.body()!!.associate { it.key to it.value }
                    cachedRemoteConfigs = map
                    saveRemoteConfigsCache(map)
                    return Result.success(map)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch remote configs, using cached values: ${e.message}")
            }
        }
        cachedRemoteConfigs = cached
        return Result.success(cached)
    }

    fun getCachedRemoteConfigs(): Map<String, String> {
        val prefs = appContext?.getSharedPreferences("safetravel_remote_configs", android.content.Context.MODE_PRIVATE)
            ?: return emptyMap()
        val all = prefs.all
        return all.mapNotNull { (k, v) -> if (v is String) k to v else null }.toMap()
    }

    private fun saveRemoteConfigsCache(configs: Map<String, String>) {
        val prefs = appContext?.getSharedPreferences("safetravel_remote_configs", android.content.Context.MODE_PRIVATE) ?: return
        val editor = prefs.edit()
        configs.forEach { (k, v) -> editor.putString(k, v) }
        editor.apply()
    }

    fun getCachedConfig(key: String, fallback: String = ""): String {
        return getCachedRemoteConfigs()[key] ?: fallback
    }

    // --- SUBSCRIPTION PLANS & MANUAL PAYMENTS ---
    suspend fun fetchSubscriptionPlans(): Result<List<SupabaseSubscriptionPlan>> {
        if (isConfigured) {
            try {
                val response = dbService?.getSubscriptionPlans(currentAuthHeader)
                if (response?.isSuccessful == true && response.body() != null) {
                    return Result.success(response.body()!!)
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.success(emptyList())
    }

    suspend fun fetchUserPaymentTransactions(userId: String): Result<List<SupabasePaymentTransaction>> {
        if (isConfigured) {
            try {
                val response = dbService?.getPaymentTransactions(
                    userAuth = currentAuthHeader,
                    userIdQuery = "eq.$userId"
                )
                if (response?.isSuccessful == true && response.body() != null) {
                    return Result.success(response.body()!!)
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.success(emptyList())
    }

    suspend fun submitManualPayment(
        userId: String,
        planId: String?,
        amount: Double,
        paymentMethod: String,
        senderNumber: String,
        transactionId: String
    ): Result<SupabasePaymentTransaction> {
        if (isConfigured) {
            try {
                val payload = mutableMapOf<String, Any>(
                    "user_id" to userId,
                    "amount" to amount,
                    "currency" to "BDT",
                    "payment_method" to paymentMethod,
                    "sender_number" to senderNumber,
                    "transaction_id" to transactionId,
                    "status" to "pending"
                )
                if (planId != null) {
                    payload["plan_id"] = planId
                }
                val response = dbService?.submitPaymentTransaction(payload, currentAuthHeader)
                if (response?.isSuccessful == true && !response.body().isNullOrEmpty()) {
                    return Result.success(response.body()!!.first())
                } else {
                    val err = response?.errorBody()?.string() ?: response?.message() ?: "Failed to submit transaction"
                    return Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(Exception("Payment service is currently unavailable."))
    }

    suspend fun fetchVersionLogs(): Result<List<SupabaseVersionLog>> {
        if (isConfigured) {
            try {
                val response = dbService?.getVersionLogs(currentAuthHeader)
                if (response?.isSuccessful == true && response.body() != null) {
                    return Result.success(response.body()!!)
                } else {
                    val err = response?.errorBody()?.string() ?: response?.message() ?: "Failed to fetch version logs"
                    return Result.failure(Exception(err))
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.success(emptyList())
    }
}

