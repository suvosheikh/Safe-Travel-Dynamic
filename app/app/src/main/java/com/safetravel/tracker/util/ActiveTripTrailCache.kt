package com.safetravel.tracker.util

import android.content.Context
import android.content.SharedPreferences
import com.safetravel.tracker.supabase.SupabaseClient
import com.squareup.moshi.Types

/**
 * Dedicated Modular Utility to guarantee persistent active trip route continuity.
 * Prevents loss of traveled green breadcrumbs when the app is swiped away from Recents,
 * crashes, or when the phone restarts mid-journey.
 */
object ActiveTripTrailCache {
    private const val PREFS_NAME = "safe_travel_active_trip_trail"
    private const val KEY_PREFIX = "trail_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Persists the current traveled route points locally to device storage.
     */
    fun saveTrail(context: Context, tripId: String, path: List<Map<String, Double>>) {
        if (tripId.isBlank() || path.isEmpty()) return
        try {
            val listType = Types.newParameterizedType(
                List::class.java,
                Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
            )
            val json = SupabaseClient.moshi.adapter<List<Map<String, Double>>>(listType).toJson(path)
            getPrefs(context).edit().putString(KEY_PREFIX + tripId, json).apply()
        } catch (e: Exception) {
            // Ignore gracefully
        }
    }

    /**
     * Loads the persisted traveled route points from local storage.
     */
    fun loadTrail(context: Context, tripId: String): List<Map<String, Double>> {
        if (tripId.isBlank()) return emptyList()
        try {
            val json = getPrefs(context).getString(KEY_PREFIX + tripId, null) ?: return emptyList()
            val listType = Types.newParameterizedType(
                List::class.java,
                Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
            )
            return SupabaseClient.moshi.adapter<List<Map<String, Double>>>(listType).fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * Clears the cached trail once the trip is completed or cancelled.
     */
    fun clearTrail(context: Context, tripId: String) {
        if (tripId.isBlank()) return
        try {
            getPrefs(context).edit().remove(KEY_PREFIX + tripId).apply()
        } catch (e: Exception) {
            // Ignore gracefully
        }
    }

    /**
     * Seamlessly stitches together the historical breadcrumbs with incoming fixes.
     * Prevents accidental overwrite or truncation of previously traveled path.
     */
    fun stitchTrail(
        inMemoryTrail: List<Map<String, Double>>,
        persistedTrail: List<Map<String, Double>>?,
        cachedTrail: List<Map<String, Double>>?,
        newPoint: Map<String, Double>? = null
    ): List<Map<String, Double>> {
        val candidates = listOfNotNull(persistedTrail, cachedTrail, inMemoryTrail)
        val bestHistory = candidates.maxByOrNull { it.size } ?: inMemoryTrail

        val combined = ArrayList<Map<String, Double>>(bestHistory.size + (if (newPoint != null) 1 else 0))
        combined.addAll(bestHistory)

        if (newPoint != null) {
            val lat = newPoint["lat"] ?: 0.0
            val lng = newPoint["lng"] ?: 0.0
            if (lat != 0.0 && lng != 0.0) {
                val last = combined.lastOrNull()
                val lastLat = last?.get("lat") ?: 0.0
                val lastLng = last?.get("lng") ?: 0.0
                // Avoid redundant identical micro-points
                if (Math.abs(lat - lastLat) > 0.00001 || Math.abs(lng - lastLng) > 0.00001) {
                    combined.add(newPoint)
                }
            }
        }

        return combined
    }
}
