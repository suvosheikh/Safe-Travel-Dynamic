package com.safetravel.tracker.util

import android.content.Context
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState

/**
 * Dedicated coordinator for Mapbox camera positioning and viewport stabilization.
 * Guarantees zero-globe cold starts and manages permission-aware location fallbacks.
 */
object MapViewportCoordinator {

    // Default city fallback when no GPS or permission is available (Central Dhaka)
    val DEFAULT_FALLBACK_POINT: Point = Point.fromLngLat(90.4125, 23.8103)
    const val DEFAULT_CITY_ZOOM = 14.5
    const val DEFAULT_STREET_ZOOM = 15.5
    const val ROUTE_OVERVIEW_ZOOM = 14.0

    /**
     * Resolves the camera position immediately upon map initialization.
     * Prevents Null Island (0,0) and Zoom 0.0 globe view under all circumstances.
     */
    fun resolveInitialCamera(
        context: Context,
        liveCoords: Pair<Double, Double>?,
        startCoords: Pair<Double, Double>?,
        destCoords: Pair<Double, Double>?,
        recordedPath: List<Point>?,
        hasPermission: Boolean
    ): CameraOptions {
        val cachedLoc = if (hasPermission) LocationHelper.getLastSavedLocation(context) else null

        val centerPoint: Point = when {
            recordedPath != null && recordedPath.isNotEmpty() -> recordedPath.first()
            liveCoords != null && liveCoords.first != 0.0 && liveCoords.second != 0.0 -> {
                Point.fromLngLat(liveCoords.second, liveCoords.first)
            }
            cachedLoc != null && cachedLoc.first != 0.0 && cachedLoc.second != 0.0 -> {
                Point.fromLngLat(cachedLoc.second, cachedLoc.first)
            }
            startCoords != null && startCoords.first != 0.0 && startCoords.second != 0.0 -> {
                Point.fromLngLat(startCoords.second, startCoords.first)
            }
            destCoords != null && destCoords.first != 0.0 && destCoords.second != 0.0 -> {
                Point.fromLngLat(destCoords.second, destCoords.first)
            }
            else -> DEFAULT_FALLBACK_POINT
        }

        val zoomLevel = when {
            recordedPath != null && recordedPath.isNotEmpty() -> ROUTE_OVERVIEW_ZOOM
            liveCoords != null || cachedLoc != null -> DEFAULT_STREET_ZOOM
            hasPermission -> DEFAULT_STREET_ZOOM
            else -> DEFAULT_CITY_ZOOM
        }

        return CameraOptions.Builder()
            .center(centerPoint)
            .zoom(zoomLevel)
            .pitch(0.0)
            .bearing(0.0)
            .build()
    }

    /**
     * Calculates the minimal angular difference between two bearings in degrees (-180..+180).
     */
    fun calculateAngleDiff(target: Double, current: Double): Double {
        return ((target - current + 540.0) % 360.0) - 180.0
    }

    /**
     * Smoothly flies the camera to the specified coordinates with specified zoom and bearing.
     */
    suspend fun flyToLocation(
        viewportState: MapViewportState,
        lat: Double,
        lng: Double,
        zoom: Double = DEFAULT_STREET_ZOOM,
        bearing: Double = 0.0
    ) {
        if (lat == 0.0 && lng == 0.0) return
        viewportState.flyTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(zoom)
                .bearing(bearing)
                .pitch(0.0)
                .build()
        )
    }
}
