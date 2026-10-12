package com.safetravel.tracker.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {
    private const val TAG = "LocationHelper"

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = suspendCancellableCoroutine { cont ->
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            // 1. Immediately check lastLocation for 0ms cold-start response
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && lastLoc.latitude != 0.0 && lastLoc.longitude != 0.0) {
                    saveLastSavedLocation(context, lastLoc.latitude, lastLoc.longitude)
                    if (cont.isActive) cont.resume(lastLoc)
                } else {
                    // Fallback to high-accuracy fresh fix
                    fusedClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { location ->
                        if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
                            saveLastSavedLocation(context, location.latitude, location.longitude)
                            if (cont.isActive) cont.resume(location)
                        } else {
                            if (cont.isActive) cont.resume(null)
                        }
                    }.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }.addOnFailureListener {
                // Fallback to high-accuracy fresh fix
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
                        saveLastSavedLocation(context, location.latitude, location.longitude)
                        if (cont.isActive) cont.resume(location)
                    } else {
                        if (cont.isActive) cont.resume(null)
                    }
                }.addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
            }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(context: Context, intervalMs: Long = 2000): Flow<Location> = callbackFlow {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        ).apply {
            setMinUpdateIntervalMillis(intervalMs / 2)
            setMinUpdateDistanceMeters(1.0f) // Fine-grained stream so AdaptiveLocationFilter can smoothly process trajectory
            setWaitForAccurateLocation(false)
        }.build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    saveLastSavedLocation(context, it.latitude, it.longitude)
                    trySend(it)
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try {
                fusedClient.removeLocationUpdates(callback)
            } catch (e: Exception) {
                // ignore on cleanup
            }
        }
    }

    /**
     * Performs reverse geocoding to get a clean, short place name from coordinates.
     */
    suspend fun getPlaceName(context: Context, latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) {
            return@withContext "Unknown Location"
        }
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val road = address.thoroughfare
                val subLocality = address.subLocality ?: address.locality
                val locality = address.locality ?: address.adminArea
                val feature = address.featureName ?: address.premises
                
                val nameBuilder = StringBuilder()
                if (!road.isNullOrBlank()) {
                    nameBuilder.append(road).append(", ")
                } else if (!feature.isNullOrBlank() && feature != subLocality && feature != locality && !feature.matches(Regex("\\d+.*"))) {
                    nameBuilder.append(feature).append(", ")
                }
                
                if (!subLocality.isNullOrBlank()) {
                    nameBuilder.append(subLocality)
                    if (subLocality != locality && !locality.isNullOrBlank()) {
                        nameBuilder.append(", ").append(locality)
                    }
                } else if (!locality.isNullOrBlank()) {
                    nameBuilder.append(locality)
                }
                
                val result = nameBuilder.toString().trim().trimEnd(',')
                if (result.isNotBlank()) return@withContext result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Android Geocoder failed, executing HTTP geocoding fallback", e)
        }

        // Secondary fallback to free OpenStreetMap Nominatim reverse-geocoding API to guarantee real address results
        return@withContext fetchFromNominatim(latitude, longitude).first
    }

    suspend fun getDistrictName(context: Context, latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) {
            return@withContext "Dhaka"
        }
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // subAdminArea usually contains the District in Bangladesh
                return@withContext address.subAdminArea ?: address.locality
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoder failed for district", e)
        }
        return@withContext fetchFromNominatim(latitude, longitude).second
    }

    private fun fetchFromNominatim(latitude: Double, longitude: Double): Pair<String, String?> {
        try {
            val urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=18&addressdetails=1"
            val url = java.net.URL(urlStr)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "SafeTravelTracker/2.0")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(responseText)
                val addressObj = json.optJSONObject("address")
                var district: String? = null
                if (addressObj != null) {
                    district = addressObj.optString("city_district", addressObj.optString("county", addressObj.optString("state_district", null)))
                    
                    val road = addressObj.optString("road", "")
                    val neighbourhood = addressObj.optString("neighbourhood", "")
                    val suburb = addressObj.optString("suburb", "")
                    val houseNumber = addressObj.optString("house_number", "")
                    val city = addressObj.optString("city", addressObj.optString("town", addressObj.optString("village", "")))
                    
                    val nameBuilder = StringBuilder()
                    if (houseNumber.isNotEmpty()) nameBuilder.append(houseNumber).append(", ")
                    
                    val area = neighbourhood.ifEmpty { suburb }.ifEmpty { road }
                    if (area.isNotEmpty()) nameBuilder.append(area).append(", ")
                    
                    if (city.isNotEmpty()) nameBuilder.append(city)
                    
                    val name = nameBuilder.toString().trim().trimEnd(',')
                    return Pair(if (name.isNotEmpty()) name else json.optString("display_name", "My Location"), district)
                }
                val displayName = json.optString("display_name", "")
                if (displayName.isNotEmpty()) {
                    val parts = displayName.split(",")
                    val name = if (parts.size >= 2) {
                        "${parts[0].trim()}, ${parts[1].trim()}"
                    } else displayName
                    return Pair(name, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Nominatim HTTP geocoding fallback failed", e)
        }
        return Pair("My Location", "Dhaka")
    }

    /**
     * Persists user's last confirmed valid GPS coordinates in local SharedPreferences.
     * Prevents globe rendering and eliminates cold-start positioning delay.
     */
    fun saveLastSavedLocation(context: Context, lat: Double, lng: Double) {
        if (lat == 0.0 && lng == 0.0) return
        try {
            val prefs = context.getSharedPreferences("safetravel_location_cache", Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat("cached_lat", lat.toFloat())
                .putFloat("cached_lng", lng.toFloat())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving location cache", e)
        }
    }

    /**
     * Retrieves user's last saved GPS coordinates from local SharedPreferences.
     * Returns null on first install before any location fix.
     */
    fun getLastSavedLocation(context: Context): Pair<Double, Double>? {
        return try {
            val prefs = context.getSharedPreferences("safetravel_location_cache", Context.MODE_PRIVATE)
            if (prefs.contains("cached_lat") && prefs.contains("cached_lng")) {
                val lat = prefs.getFloat("cached_lat", 0f).toDouble()
                val lng = prefs.getFloat("cached_lng", 0f).toDouble()
                if (lat != 0.0 && lng != 0.0) Pair(lat, lng) else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
