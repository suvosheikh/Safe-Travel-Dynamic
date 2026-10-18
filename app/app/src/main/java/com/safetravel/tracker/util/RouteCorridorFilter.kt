package com.safetravel.tracker.util

import android.location.Location
import kotlin.math.*

/**
 * Dedicated Modular Utility for Route Corridor Snapping and Multipath Building Rejection.
 * Prevents GPS breadcrumbs from drifting into roadside buildings, shops, or elevated metro rail pillars
 * while strictly allowing natural off-route detours and alley turns.
 */
object RouteCorridorFilter {

    private const val DEFAULT_CORRIDOR_WIDTH_METERS = 22.0 // Typical avenue + sidewalk width
    private const val OFF_ROUTE_DETOUR_THRESHOLD_METERS = 38.0
    private const val CONSECUTIVE_OFF_ROUTE_LIMIT = 3

    private var consecutiveOffRouteCount = 0

    /**
     * Resets detour counter (call on trip start or route reload).
     */
    fun reset() {
        consecutiveOffRouteCount = 0
    }

    /**
     * Evaluates a coordinate against the planned route polyline.
     * If the point is within the corridor width, snaps/blends it smoothly to the road centerline.
     * If the user is genuinely detouring, leaves the point untouched.
     *
     * @param lat Current latitude
     * @param lng Current longitude
     * @param plannedRoute List of coordinate maps with "lat" and "lng" keys
     * @return Pair<Double, Double> containing the corridor-corrected coordinate
     */
    fun snapToCorridorIfApplicable(
        lat: Double,
        lng: Double,
        plannedRoute: List<Map<String, Double>>?
    ): Pair<Double, Double> {
        if (plannedRoute.isNullOrEmpty() || plannedRoute.size < 2) {
            return Pair(lat, lng)
        }

        var minDistanceMeters = Double.MAX_VALUE
        var bestProjectedLat = lat
        var bestProjectedLng = lng

        val results = FloatArray(1)

        // Iterate over line segments of the planned route
        for (i in 0 until plannedRoute.size - 1) {
            val p1Lat = plannedRoute[i]["lat"] ?: continue
            val p1Lng = plannedRoute[i]["lng"] ?: continue
            val p2Lat = plannedRoute[i + 1]["lat"] ?: continue
            val p2Lng = plannedRoute[i + 1]["lng"] ?: continue

            val projected = projectPointOnSegment(lat, lng, p1Lat, p1Lng, p2Lat, p2Lng)
            Location.distanceBetween(lat, lng, projected.first, projected.second, results)
            val dist = results[0].toDouble()

            if (dist < minDistanceMeters) {
                minDistanceMeters = dist
                bestProjectedLat = projected.first
                bestProjectedLng = projected.second
            }
        }

        // If user is far from route, they may be detouring or turning into an unmapped alley
        if (minDistanceMeters > OFF_ROUTE_DETOUR_THRESHOLD_METERS) {
            consecutiveOffRouteCount++
            if (consecutiveOffRouteCount >= CONSECUTIVE_OFF_ROUTE_LIMIT) {
                // Genuine detour confirmed: follow user's natural track
                return Pair(lat, lng)
            }
        } else {
            consecutiveOffRouteCount = 0
        }

        // If within corridor (e.g. within 22 meters of road center), apply smooth corridor snapping
        return if (minDistanceMeters <= DEFAULT_CORRIDOR_WIDTH_METERS) {
            // Smooth blend: closer to road = higher snap weight, edge of corridor = gentle blend
            val snapWeight = (1.0 - (minDistanceMeters / DEFAULT_CORRIDOR_WIDTH_METERS).pow(2)).coerceIn(0.65, 0.98)
            val correctedLat = bestProjectedLat * snapWeight + lat * (1.0 - snapWeight)
            val correctedLng = bestProjectedLng * snapWeight + lng * (1.0 - snapWeight)
            Pair(correctedLat, correctedLng)
        } else {
            Pair(lat, lng)
        }
    }

    /**
     * Projects a point (lat, lng) onto a line segment defined by (aLat, aLng) and (bLat, bLng).
     */
    private fun projectPointOnSegment(
        pLat: Double, pLng: Double,
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double
    ): Pair<Double, Double> {
        // Metric approximation using flat-earth projection centered at aLat
        val latFactor = 111132.0
        val lngFactor = 111132.0 * cos(Math.toRadians(aLat))

        val ax = aLng * lngFactor
        val ay = aLat * latFactor
        val bx = bLng * lngFactor
        val by = bLat * latFactor
        val px = pLng * lngFactor
        val py = pLat * latFactor

        val abx = bx - ax
        val aby = by - ay
        val abLenSq = abx * abx + aby * aby

        if (abLenSq < 1e-6) {
            return Pair(aLat, aLng)
        }

        val apx = px - ax
        val apy = py - ay
        val t = ((apx * abx + apy * aby) / abLenSq).coerceIn(0.0, 1.0)

        val projX = ax + t * abx
        val projY = ay + t * aby

        val projLng = projX / lngFactor
        val projLat = projY / latFactor

        return Pair(projLat, projLng)
    }
}
