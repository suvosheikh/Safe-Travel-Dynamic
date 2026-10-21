package com.safetravel.tracker.util

import android.content.Context
import android.util.Log
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState

/**
 * Dedicated coordinator for Mapbox camera positioning and viewport stabilization.
 * Guarantees zero-globe cold starts and manages permission-aware location fallbacks.
 */
object MapViewportCoordinator {

    private const val TAG = "MapViewportCoordinator"

    // Default city fallback when no GPS or permission is available (Central Dhaka)
    val DEFAULT_FALLBACK_POINT: Point = Point.fromLngLat(90.4125, 23.8103)
    const val DEFAULT_CITY_ZOOM = 14.5
    const val DEFAULT_STREET_ZOOM = 15.5
    const val ROUTE_OVERVIEW_ZOOM = 14.0

    /**
     * Enforces 2D Mercator projection to prevent any 3D globe rendering under all circumstances.
     */
    fun enforceMercatorProjection(style: Style) {
        try {
            style.setStyleProjectionProperty("name", Value("mercator"))
            Log.d(TAG, "Map projection explicitly set to Flat 2D Mercator (No Globe)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enforce Mercator projection", e)
        }
    }

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
            else -> DEFAULT_STREET_ZOOM
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
    fun flyToLocation(
        viewportState: MapViewportState,
        lat: Double,
        lng: Double,
        zoom: Double = DEFAULT_STREET_ZOOM,
        bearing: Double = 0.0
    ) {
        if (lat == 0.0 && lng == 0.0) return
        val targetZoom = if (zoom < 13.0) DEFAULT_STREET_ZOOM else zoom
        viewportState.flyTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(lng, lat))
                .zoom(targetZoom)
                .bearing(bearing)
                .pitch(0.0)
                .build()
        )
    }

    /**
     * Smoothly keeps the camera centered on the user during continuous tracking
     * without triggering Mapbox's dramatic flight curve / zoom dip.
     */
    fun smoothPanToLocation(
        viewportState: MapViewportState,
        lat: Double,
        lng: Double,
        zoom: Double = DEFAULT_STREET_ZOOM,
        bearing: Double = 0.0
    ) {
        if (lat == 0.0 && lng == 0.0) return
        val targetZoom = if (zoom < 13.0) DEFAULT_STREET_ZOOM else zoom
        viewportState.setCameraOptions {
            center(Point.fromLngLat(lng, lat))
            zoom(targetZoom)
            bearing(bearing)
            pitch(0.0)
        }
    }
}
