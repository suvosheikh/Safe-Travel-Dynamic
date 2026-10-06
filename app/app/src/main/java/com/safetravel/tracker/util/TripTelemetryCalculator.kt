package com.safetravel.tracker.util

import android.location.Location
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dedicated Modular Utility for Trip Telemetry Calculations.
 * Handles geodesic distance summation, remaining distance estimation,
 * duration projections, and formatting so ViewModels and UI stay clean.
 */
object TripTelemetryCalculator {

    /**
     * Calculates the exact, actual distance traveled (in kilometers) from a GPS breadcrumb log.
     * Uses Android's high-precision WGS84 geodesic distance calculation between consecutive fixes.
     * Filters out sub-meter sensor jitter (< 1.2m) to prevent phantom distance accumulation when stationary.
     */
    fun calculateActualTraveledDistanceKm(routePath: List<Map<String, Double>>?): Double {
        if (routePath == null || routePath.size < 2) return 0.0

        var totalMeters = 0.0
        val results = FloatArray(1)

        for (i in 0 until routePath.size - 1) {
            val p1 = routePath[i]
            val p2 = routePath[i + 1]

            val lat1 = p1["lat"] ?: continue
            val lng1 = p1["lng"] ?: continue
            val lat2 = p2["lat"] ?: continue
            val lng2 = p2["lng"] ?: continue

            if (lat1 == 0.0 || lng1 == 0.0 || lat2 == 0.0 || lng2 == 0.0) continue

            Location.distanceBetween(lat1, lng1, lat2, lng2, results)
            val segmentMeters = results[0]

            // Ignore minuscule jitter (< 1.2 meters) when phone is standing still
            if (segmentMeters >= 1.2f) {
                totalMeters += segmentMeters
            }
        }

        val totalKm = totalMeters / 1000.0
        // Round to 2 decimal places (e.g., 3.84 km)
        return (totalKm * 100.0).roundToInt() / 100.0
    }

    /**
     * Calculates the remaining distance to the destination in kilometers.
     * If a planned route polyline is available, it finds the nearest point to the current
     * location and sums the remaining route vertices to the destination.
     * Otherwise, calculates direct geodesic distance.
     */
    fun calculateRemainingDistanceKm(
        currentLat: Double,
        currentLng: Double,
        endLat: Double,
        endLng: Double,
        plannedRoute: List<Map<String, Double>>? = null
    ): Double {
        if (currentLat == 0.0 || currentLng == 0.0 || endLat == 0.0 || endLng == 0.0) return 0.0

        val results = FloatArray(1)

        // If we have planned route waypoints, calculate along the polyline
        if (plannedRoute != null && plannedRoute.size >= 2) {
            var closestIndex = 0
            var minDistanceToRoute = Float.MAX_VALUE

            for (i in plannedRoute.indices) {
                val pt = plannedRoute[i]
                val lat = pt["lat"] ?: continue
                val lng = pt["lng"] ?: continue
                Location.distanceBetween(currentLat, currentLng, lat, lng, results)
                if (results[0] < minDistanceToRoute) {
                    minDistanceToRoute = results[0]
                    closestIndex = i
                }
            }

            var remainingMeters = 0.0
            for (i in closestIndex until plannedRoute.size - 1) {
                val p1 = plannedRoute[i]
                val p2 = plannedRoute[i + 1]
                val lat1 = p1["lat"] ?: continue
                val lng1 = p1["lng"] ?: continue
                val lat2 = p2["lat"] ?: continue
                val lng2 = p2["lng"] ?: continue
                Location.distanceBetween(lat1, lng1, lat2, lng2, results)
                remainingMeters += results[0]
            }

            if (remainingMeters > 0.0) {
                return (remainingMeters / 1000.0 * 10.0).roundToInt() / 10.0
            }
        }

        // Direct fallback distance between current position and destination
        Location.distanceBetween(currentLat, currentLng, endLat, endLng, results)
        val directKm = results[0] / 1000.0
        return (directKm * 10.0).roundToInt() / 10.0
    }

    /**
     * Estimates remaining trip duration in minutes based on distance and speed.
     */
    fun calculateRemainingDurationMin(
        remainingDistanceKm: Double,
        currentSpeedKmh: Int = 0,
        transportMode: String? = null
    ): Int {
        if (remainingDistanceKm <= 0.05) return 0

        val effectiveSpeedKmh = if (currentSpeedKmh >= 6) {
            currentSpeedKmh.toDouble()
        } else {
            when (transportMode?.lowercase(Locale.US)) {
                "walk", "walking", "0" -> 4.5
                "cycle", "bike", "bicycling", "1" -> 15.0
                "bus", "transit", "3" -> 22.0
                else -> 28.0 // Car / Driving default city average
            }
        }

        val hours = remainingDistanceKm / effectiveSpeedKmh
        return (hours * 60.0).roundToInt().coerceAtLeast(1)
    }

    /**
     * Formats a distance in kilometers to a clean, user-facing string (e.g. "5.4 km").
     */
    fun formatDistance(distanceKm: Double?): String {
        if (distanceKm == null || distanceKm <= 0.0) return "--- km"
        return String.format(Locale.US, "%.1f km", distanceKm)
    }

    /**
     * Formats duration in minutes to user-facing string (e.g. "18 min", "1 hr 12 min").
     */
    fun formatDuration(minutes: Int?): String {
        if (minutes == null || minutes <= 0) return "---"
        return if (minutes < 60) {
            "$minutes min"
        } else {
            val hrs = minutes / 60
            val rem = minutes % 60
            if (rem == 0) "$hrs hr" else "$hrs hr $rem min"
        }
    }
}
