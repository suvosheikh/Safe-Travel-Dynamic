package com.safetravel.tracker.util

import android.util.Log
import com.mapbox.geojson.Point
import com.safetravel.tracker.ui.components.travel.RouteInfo
import com.safetravel.tracker.ui.components.travel.parseMapboxRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/**
 * Dedicated Modular Utility for Intelligent Multi-Modal Route Planning.
 * 
 * Rules & Logic:
 * 1. Walk (0): Combines pedestrian shortcuts + main road driving corridors (walkable via sidewalks/roadsides).
 * 2. Cycle (1): Combines cycling paths + main road driving corridors.
 * 3. Car (2) & Bus (3): STRICTLY fetches motorized driving corridors only (never alleys or footbridges).
 * 4. Shortest Route Priority: Guarantees the shortest distance route is at index 0.
 * 5. Multi-Route Alternatives: Returns all discovered candidate routes up to a maximum of 5.
 */
object SmartRoutePlanner {
    private const val TAG = "SmartRoutePlanner"

    suspend fun fetchSmartRoutes(
        origin: Point,
        destination: Point,
        transportMode: Int,
        mapboxToken: String
    ): List<RouteInfo> = withContext(Dispatchers.IO) {
        val originLng = origin.longitude()
        val originLat = origin.latitude()
        val destLng = destination.longitude()
        val destLat = destination.latitude()

        if (mapboxToken.isBlank()) return@withContext emptyList()

        val rawCandidates = mutableListOf<RouteInfo>()

        try {
            when (transportMode) {
                0 -> { // Walk Mode: Combines Pedestrian shortcuts + Motor Vehicle Corridors (walkable via sidewalks)
                    coroutineScope {
                        val walkDeferred = async { fetchRawRoutes("walking", originLng, originLat, destLng, destLat, mapboxToken) }
                        val driveDeferred = async { fetchRawRoutes("driving", originLng, originLat, destLng, destLat, mapboxToken) }

                        val walkRoutes = walkDeferred.await()
                        val driveRoutes = driveDeferred.await()

                        // Pedestrian dedicated routes
                        walkRoutes.forEachIndexed { i, r ->
                            val tag = if (r.summary.isNotBlank()) r.summary else if (i == 0) "Walk Path" else "Walk Alt ${i + 1}"
                            rawCandidates.add(r.copy(summary = tag))
                        }

                        // Vehicle corridors adapted for walking (~4.5 km/h walking speed)
                        driveRoutes.forEachIndexed { _, r ->
                            val walkingDurationMin = (r.distanceKm / 4.5) * 60.0
                            val tag = if (r.summary.isNotBlank()) "Main Road (${r.summary})" else "Main Road"
                            rawCandidates.add(
                                RouteInfo(
                                    points = r.points,
                                    durationMin = walkingDurationMin,
                                    distanceKm = r.distanceKm,
                                    summary = tag
                                )
                            )
                        }
                    }
                }
                1 -> { // Cycle Mode: Combines Cycling paths + Motor Vehicle Corridors
                    coroutineScope {
                        val cycleDeferred = async { fetchRawRoutes("cycling", originLng, originLat, destLng, destLat, mapboxToken) }
                        val driveDeferred = async { fetchRawRoutes("driving", originLng, originLat, destLng, destLat, mapboxToken) }

                        val cycleRoutes = cycleDeferred.await()
                        val driveRoutes = driveDeferred.await()

                        cycleRoutes.forEachIndexed { i, r ->
                            val tag = if (r.summary.isNotBlank()) r.summary else if (i == 0) "Cycle Path" else "Cycle Alt ${i + 1}"
                            rawCandidates.add(r.copy(summary = tag))
                        }

                        // Vehicle corridors adapted for cycling (~15.0 km/h cycling speed)
                        driveRoutes.forEachIndexed { _, r ->
                            val cycleDurationMin = (r.distanceKm / 15.0) * 60.0
                            val tag = if (r.summary.isNotBlank()) "Main Road (${r.summary})" else "Main Road"
                            rawCandidates.add(
                                RouteInfo(
                                    points = r.points,
                                    durationMin = cycleDurationMin,
                                    distanceKm = r.distanceKm,
                                    summary = tag
                                )
                            )
                        }
                    }
                }
                else -> { // Car (2) or Bus (3): STRICTLY Driving Corridors only (never alleys)
                    val driveRoutes = fetchRawRoutes("driving", originLng, originLat, destLng, destLat, mapboxToken)
                    driveRoutes.forEachIndexed { i, r ->
                        val prefix = if (transportMode == 3) "Bus Corridor" else "Road"
                        val tag = if (r.summary.isNotBlank()) r.summary else "$prefix ${i + 1}"
                        rawCandidates.add(r.copy(summary = tag))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching smart routes: ${e.message}", e)
        }

        if (rawCandidates.isEmpty()) return@withContext emptyList()

        // Deduplicate routes with nearly identical geometry / distance (< 40 meters)
        val deduplicated = mutableListOf<RouteInfo>()
        for (cand in rawCandidates) {
            val exists = deduplicated.any { existing ->
                abs(existing.distanceKm - cand.distanceKm) < 0.04 &&
                        existing.points.size == cand.points.size
            }
            if (!exists) {
                deduplicated.add(cand)
            }
        }

        // 1. Prioritize Shortest Route First: Sort by distanceKm ascending
        // 2. Max 5 routes
        val sortedShortestFirst = deduplicated
            .sortedBy { it.distanceKm }
            .take(5)

        sortedShortestFirst.mapIndexed { index, route ->
            if (index == 0) {
                val clean = route.summary.removePrefix("Shortest (").removeSuffix(")").trim()
                val label = if (clean.isNotBlank()) "Shortest ($clean)" else "Shortest Route"
                route.copy(summary = label)
            } else {
                route
            }
        }
    }

    private fun fetchRawRoutes(
        profile: String,
        originLng: Double,
        originLat: Double,
        destLng: Double,
        destLat: Double,
        token: String
    ): List<RouteInfo> {
        return try {
            val urlStr = "https://api.mapbox.com/directions/v5/mapbox/$profile/$originLng,$originLat;$destLng,$destLat?geometries=geojson&overview=full&alternatives=true&access_token=$token"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                parseMapboxRoutes(json)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch raw $profile routes: ${e.message}")
            emptyList()
        }
    }
}
