package com.safetravel.tracker.util

import com.mapbox.geojson.Point
import java.util.Locale
import kotlin.math.*

/**
 * Utility for analyzing, bounding, and extracting metrics from travel routes.
 * Supports camera auto-fit calculations, distance computation, and replay telemetry.
 */
object MapRouteUtils {

    data class RouteBoundsInfo(
        val center: Point,
        val zoom: Double,
        val minLat: Double,
        val maxLat: Double,
        val minLng: Double,
        val maxLng: Double
    )

    data class TripMetrics(
        val distanceKm: Double,
        val durationMinutes: Long,
        val avgSpeedKmh: Double,
        val distanceFormatted: String,
        val durationFormatted: String,
        val speedFormatted: String
    )

    /**
     * Calculates the geographic bounding box and optimal camera zoom level
     * so that the full journey route fits cleanly into the screen viewport.
     */
    fun calculateRouteBounds(points: List<Point>, defaultCenter: Point? = null): RouteBoundsInfo {
        if (points.isEmpty()) {
            val center = defaultCenter ?: Point.fromLngLat(90.4125, 23.8103)
            return RouteBoundsInfo(center = center, zoom = 14.5, 0.0, 0.0, 0.0, 0.0)
        }

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE

        for (p in points) {
            val lat = p.latitude()
            val lng = p.longitude()
            if (lat < minLat) minLat = lat
            if (lat > maxLat) maxLat = lat
            if (lng < minLng) minLng = lng
            if (lng > maxLng) maxLng = lng
        }

        val centerLat = (minLat + maxLat) / 2.0
        val centerLng = (minLng + maxLng) / 2.0
        val center = Point.fromLngLat(centerLng, centerLat)

        val deltaLat = abs(maxLat - minLat)
        val deltaLng = abs(maxLng - minLng)
        val maxDelta = max(deltaLat, deltaLng)

        // Balanced zoom level calculation based on coordinate span
        val zoom = when {
            maxDelta < 0.005 -> 15.8
            maxDelta < 0.012 -> 14.8
            maxDelta < 0.025 -> 14.0
            maxDelta < 0.05 -> 13.2
            maxDelta < 0.10 -> 12.4
            maxDelta < 0.20 -> 11.5
            maxDelta < 0.40 -> 10.5
            maxDelta < 0.80 -> 9.5
            maxDelta < 1.60 -> 8.5
            else -> 7.5
        }

        return RouteBoundsInfo(center, zoom, minLat, maxLat, minLng, maxLng)
    }

    /**
     * Computes the Great-Circle distance (Haversine formula) in meters between two lat/lng pairs.
     */
    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }

    /**
     * Computes the total path distance in kilometers along a list of recorded GPS coordinates.
     */
    fun calculateTotalDistanceKm(points: List<Point>): Double {
        if (points.size < 2) return 0.0
        var totalMeters = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            totalMeters += haversineDistanceMeters(p1.latitude(), p1.longitude(), p2.latitude(), p2.longitude())
        }
        return totalMeters / 1000.0
    }

    /**
     * Calculates duration in elapsed minutes between start and end timestamps.
     */
    fun calculateDurationMinutes(startTimeIso: String?, endTimeIso: String?): Long {
        val start = DateTimeUtils.parseIsoToDate(startTimeIso) ?: return 0L
        val end = DateTimeUtils.parseIsoToDate(endTimeIso) ?: return 0L
        val diffMs = end.time - start.time
        return if (diffMs > 0) diffMs / (1000 * 60) else 0L
    }

    /**
     * Generates a comprehensive TripMetrics object containing distance, duration, and average speed.
     */
    fun calculateTripMetrics(
        points: List<Point>,
        startTimeIso: String?,
        endTimeIso: String?
    ): TripMetrics {
        val distKm = calculateTotalDistanceKm(points)
        val durationMinutes = calculateDurationMinutes(startTimeIso, endTimeIso)
        val avgSpeed = if (durationMinutes > 0 && distKm > 0.0) {
            distKm / (durationMinutes / 60.0)
        } else 0.0

        val distStr = String.format(Locale.US, "%.1f km", distKm)
        val durStr = when {
            durationMinutes <= 0 -> "< 1 min"
            durationMinutes < 60 -> "$durationMinutes mins"
            else -> {
                val hrs = durationMinutes / 60
                val mins = durationMinutes % 60
                if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
            }
        }
        val speedStr = String.format(Locale.US, "%.1f km/h", avgSpeed)

        return TripMetrics(distKm, durationMinutes, avgSpeed, distStr, durStr, speedStr)
    }
}
