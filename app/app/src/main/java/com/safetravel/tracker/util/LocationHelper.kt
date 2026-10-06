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
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(location)
                } else {
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                        cont.resume(lastLoc)
                    }.addOnFailureListener {
                        cont.resume(null)
                    }
                }
            }.addOnFailureListener {
                fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                    cont.resume(lastLoc)
                }.addOnFailureListener {
                    cont.resume(null)
                }
            }
        } catch (e: Exception) {
            cont.resume(null)
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
                result.lastLocation?.let { trySend(it) }
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
}
