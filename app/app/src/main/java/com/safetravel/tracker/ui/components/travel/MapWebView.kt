package com.safetravel.tracker.ui.components.travel

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.safetravel.tracker.util.LocationHelper
import com.safetravel.tracker.R
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.safetravel.tracker.ui.theme.Slate300
import com.safetravel.tracker.ui.theme.Emerald400
import com.safetravel.tracker.ui.theme.Slate700
import com.safetravel.tracker.ui.theme.Slate800
import com.safetravel.tracker.ui.components.PermissionDisclosureDialog
import com.safetravel.tracker.ui.components.SafetyPermissionType
import kotlinx.coroutines.awaitCancellation
import android.util.Log

import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.AnnotatedFeature
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.geojson.Point
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.*
import com.mapbox.maps.CameraOptions
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.OnMapClickListener

/**
 * Shared method to find coordinate fallback matching main landmarks
 */
fun getCoordsForAddress(address: String): Pair<Double, Double>? {
    val clean = address.trim().lowercase()
    if (clean.isBlank() || clean == "destination" || clean == "live beacon navigation" || 
        clean == "not specified" || clean == "dhanmondi, dhaka" || clean == "user location stream") {
        return null
    }
    // 1. Try robust address extractor first (handles "lat,lng", "[lng, lat]", JSON formats)
    val extracted = com.safetravel.tracker.util.AddressUtils.extractCoordinates(address)
    if (extracted != null && extracted.first != 0.0 && extracted.second != 0.0) {
        return extracted
    }
    return when {
        clean.contains("point") || clean.contains("map") || (clean.contains("[") && clean.contains("]")) -> {
            try {
                val content = clean.substringAfter("[").substringBefore("]")
                val parts = content.split(",")
                val lng = parts[0].trim().toDouble()
                val lat = parts[1].trim().toDouble()
                Pair(lat, lng)
            } catch (e: Exception) {
                null
            }
        }
        clean.contains(",") -> {
            try {
                val parts = clean.split(",")
                val first = parts[0].trim().toDoubleOrNull()
                val second = parts[1].trim().toDoubleOrNull()
                if (first != null && second != null) {
                    if (first > 50.0) Pair(second, first) else Pair(first, second)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        else -> null
    }
}

/**
 * Helper to programmatically draw high-contrast, beautiful custom vector marker bitmaps
 */
fun createCustomMarkerBitmap(colorHex: Int, isPulsingUser: Boolean): Bitmap {
    val size = if (isPulsingUser) 64 else 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
    }
    
    if (isPulsingUser) {
        // Outer pulsing dynamic glow circle
        paint.color = colorHex
        paint.alpha = 55
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        // Mid solid dynamic color ring
        paint.color = colorHex
        paint.alpha = 150
        canvas.drawCircle(size / 2f, size / 2f, size / 3.2f, paint)
        
        // Inner white solid beacon dot
        paint.color = AndroidColor.WHITE
        paint.alpha = 255
        canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint)
    } else {
        // High-contrast custom Mapbox Style Destination Needle Pin
        // Outer glow/shadow
        paint.color = AndroidColor.BLACK
        paint.alpha = 60
        canvas.drawCircle(size / 2f, size / 3f, size / 3f + 1.5f, paint)

        // Main colored teardrop body
        paint.color = colorHex
        paint.alpha = 255
        canvas.drawCircle(size / 2f, size / 3f, size / 3.4f, paint)
        
        // Drawing pin needle
        val path = Path()
        path.moveTo(size / 2f - size / 4f, size / 3f)
        path.lineTo(size / 2f, size * 0.9f)
        path.lineTo(size / 2f + size / 4f, size / 3f)
        path.close()
        canvas.drawPath(path, paint)
        
        // Inner white highlight core
        paint.color = AndroidColor.WHITE
        canvas.drawCircle(size / 2f, size / 3f, size / 8f, paint)
    }
    return bitmap
}

/**
 * Data class to hold parsed route info
 */
data class RouteInfo(
    val points: List<Point>,
    val durationMin: Double,
    val distanceKm: Double,
    val summary: String = ""
)

/**
 * Checks if a clicked map point is near a polyline within a given tolerance (in meters)
 */
fun isPointNearPolyline(click: Point, polyline: List<Point>, toleranceMeters: Float = 120f): Boolean {
    val results = FloatArray(1)
    val step = (polyline.size / 80).coerceAtLeast(1)
    for (i in 0 until polyline.size step step) {
        val pt = polyline[i]
        android.location.Location.distanceBetween(
            click.latitude(), click.longitude(),
            pt.latitude(), pt.longitude(),
            results
        )
        if (results[0] <= toleranceMeters) {
            return true
        }
    }
    return false
}

/**
 * Parses all available route alternatives returned from the Mapbox Directions API
 */
fun parseMapboxRoutes(jsonStr: String): List<RouteInfo> {
    val allRoutes = mutableListOf<RouteInfo>()
    try {
        val root = org.json.JSONObject(jsonStr)
        val routes = root.optJSONArray("routes")
        if (routes != null) {
            for (i in 0 until routes.length()) {
                val routePoints = mutableListOf<Point>()
                val route = routes.getJSONObject(i)
                
                // Extract metadata
                val duration = route.optDouble("duration", 0.0) / 60.0 // seconds to minutes
                val distance = route.optDouble("distance", 0.0) / 1000.0 // meters to km
                
                // Extract road summary (e.g., "Mirpur Rd")
                val legs = route.optJSONArray("legs")
                val summary = if (legs != null && legs.length() > 0) {
                    legs.getJSONObject(0).optString("summary", "")
                } else ""
                
                val geometry = route.optJSONObject("geometry")
                if (geometry != null) {
                    val coordinates = geometry.optJSONArray("coordinates")
                    if (coordinates != null) {
                        for (j in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(j)
                            val lng = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            routePoints.add(Point.fromLngLat(lng, lat))
                        }
                    }
                }
                if (routePoints.isNotEmpty()) {
                    allRoutes.add(RouteInfo(routePoints, duration, distance, summary))
                }
            }
        }
    } catch (e: Exception) {
        Log.e("MapWebView", "Error parsing Mapbox routes JSON", e)
    }
    return allRoutes
}

@Composable
fun MapboxView(
    destination: String,
    onLocationSelected: (Double, Double, String) -> Unit,
    mapboxToken: String,
    modifier: Modifier = Modifier,
    liveUserCoordinates: Pair<Double, Double>? = null,
    navigationRouteActive: Boolean = false,
    onEngineLoaded: (String) -> Unit = {},
    onLogReceived: (String) -> Unit = {},
    isFullscreen: Boolean = false,
    onCloseFullscreen: (() -> Unit)? = null,
    onDeviceLocationChanged: (Double, Double) -> Unit = { _, _ -> },
    transportMode: Int = 2, // 0: Walk, 1: Bike, 2: Car, 3: Bus
    onTripDetailsCalculated: (String, String) -> Unit = { _, _ -> },
    recordedPath: List<Point>? = null,
    showUserLocation: Boolean = recordedPath == null,
    startCoords: Pair<Double, Double>? = null,
    hasCenteredInitially: Boolean = false,
    onCenteredInitially: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onToggleSafetyToolkit: (() -> Unit)? = null,
    destinationCoords: Pair<Double, Double>? = null,
    liveRoutePath: List<Point>? = null,
    selectedRouteIndex: Int = 0,
    onRouteSelected: ((Int, RouteInfo) -> Unit)? = null,
    onRouteChange: ((List<RouteInfo>) -> Unit)? = null,
    fixedPlannedRoute: List<Point>? = null
) {
    NativeMapLibreView(
        destination = destination,
        onLocationSelected = onLocationSelected,
        mapboxToken = mapboxToken,
        modifier = modifier,
        liveUserCoordinates = liveUserCoordinates,
        navigationRouteActive = navigationRouteActive,
        onEngineLoaded = onEngineLoaded,
        onLogReceived = onLogReceived,
        isFullscreen = isFullscreen,
        onCloseFullscreen = onCloseFullscreen,
        onDeviceLocationChanged = onDeviceLocationChanged,
        transportMode = transportMode,
        onTripDetailsCalculated = onTripDetailsCalculated,
        recordedPath = recordedPath,
        showUserLocation = showUserLocation,
        startCoords = startCoords,
        hasCenteredInitially = hasCenteredInitially,
        onCenteredInitially = onCenteredInitially,
        bottomPadding = bottomPadding,
        onToggleSafetyToolkit = onToggleSafetyToolkit,
        destinationCoords = destinationCoords,
        liveRoutePath = liveRoutePath,
        selectedRouteIndex = selectedRouteIndex,
        onRouteSelected = onRouteSelected,
        onRouteChange = onRouteChange,
        fixedPlannedRoute = fixedPlannedRoute
    )
}

@SuppressLint("MissingPermission")
@Composable
fun NativeMapLibreView(
    destination: String,
    onLocationSelected: (Double, Double, String) -> Unit,
    mapboxToken: String,
    modifier: Modifier = Modifier,
    liveUserCoordinates: Pair<Double, Double>? = null,
    navigationRouteActive: Boolean = false,
    onEngineLoaded: (String) -> Unit = {},
    onLogReceived: (String) -> Unit = {},
    isFullscreen: Boolean = false,
    onCloseFullscreen: (() -> Unit)? = null,
    onDeviceLocationChanged: (Double, Double) -> Unit = { _, _ -> },
    transportMode: Int = 2,
    onTripDetailsCalculated: (String, String) -> Unit = { _, _ -> },
    recordedPath: List<Point>? = null,
    showUserLocation: Boolean = recordedPath == null,
    startCoords: Pair<Double, Double>? = null,
    hasCenteredInitially: Boolean = false,
    onCenteredInitially: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    onToggleSafetyToolkit: (() -> Unit)? = null,
    destinationCoords: Pair<Double, Double>? = null,
    liveRoutePath: List<Point>? = null,
    selectedRouteIndex: Int = 0,
    onRouteSelected: ((Int, RouteInfo) -> Unit)? = null,
    onRouteChange: ((List<RouteInfo>) -> Unit)? = null,
    fixedPlannedRoute: List<Point>? = null
) {
    // Check if we are in Preview mode to avoid UnsatisfiedLinkError from Mapbox native libraries
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    if (isPreview) {
        Box(
            modifier = modifier.background(Color(0xFF090D16)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = Slate700,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Map Preview Mode",
                    color = Slate300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    val context = LocalContext.current
    var isFullscreenOpen by remember { mutableStateOf(false) }

    // 1. Set Mapbox Access Token statically on load/update
    LaunchedEffect(mapboxToken) {
        if (mapboxToken.isNotBlank()) {
            MapboxOptions.accessToken = mapboxToken
            onLogReceived("Mapbox native access token initialized successfully.")
        }
    }

    // 2. Check & Request Location Permission with Prominent Disclosure
    var showLocationDisclosure by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // 3. Fetch Device Location Coordinates
    var deviceCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    LaunchedEffect(hasLocationPermission, liveUserCoordinates) {
        // If liveUserCoordinates is provided by the ViewModel, use ViewModel as the single source of truth
        if (liveUserCoordinates != null) return@LaunchedEffect

        if (hasLocationPermission) {
            try {
                val appContext = context.applicationContext
                val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var bestLoc: Location? = null
                for (provider in locationManager.getProviders(true)) {
                    val loc = try { locationManager.getLastKnownLocation(provider) } catch (e: SecurityException) { null }
                    if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                        if (bestLoc == null || loc.time > bestLoc.time) bestLoc = loc
                    }
                }
                if (bestLoc != null) {
                    deviceCoords = Pair(bestLoc.latitude, bestLoc.longitude)
                }

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (location.latitude != 0.0 && location.longitude != 0.0) {
                            deviceCoords = Pair(location.latitude, location.longitude)
                        }
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                var isListenerRegistered = false
                // Use pure GPS provider only to avoid coarse jumping from cell towers
                val activeProvider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    LocationManager.GPS_PROVIDER
                } else null

                if (activeProvider != null) {
                    try {
                        locationManager.requestLocationUpdates(activeProvider, 2000L, 1.0f, listener)
                        isListenerRegistered = true
                    } catch (e: Exception) {
                        Log.e("MapWebView", "Failed to request updates from $activeProvider", e)
                    }
                }

                try {
                    awaitCancellation()
                } finally {
                    if (isListenerRegistered) {
                        try {
                            locationManager.removeUpdates(listener)
                        } catch (e: Exception) {
                            Log.e("MapWebView", "Failed to remove updates", e)
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MapWebView", "SecurityException during location tracking", e)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // Rethrow to allow standard Compose coroutine teardown
            } catch (e: Exception) {
                Log.e("MapWebView", "Error during location tracking setup", e)
            }
        }
    }

    LaunchedEffect(deviceCoords) {
        deviceCoords?.let { (lat, lng) ->
            if (lat != 0.0 && lng != 0.0) {
                onDeviceLocationChanged(lat, lng)
            }
        }
    }

    // 4. Resolve user coordinates: strictly use real GPS/device coordinates without mock data
    val resolvedUserCoords = remember(deviceCoords, liveUserCoordinates) {
        liveUserCoordinates ?: deviceCoords
    }

    val destCoords = remember(destination, destinationCoords, recordedPath) { 
        if (recordedPath != null && recordedPath.isNotEmpty()) {
            val last = recordedPath.last()
            Pair(last.latitude(), last.longitude())
        } else if (destinationCoords != null && destinationCoords.first != 0.0 && destinationCoords.second != 0.0) {
            destinationCoords
        } else if (destination.isBlank() || destination == "Dhanmondi, Dhaka") {
            null 
        } else {
            getCoordsForAddress(destination)
        }
    }
    
    val userPoint = remember(resolvedUserCoords) {
        resolvedUserCoords?.let { Point.fromLngLat(it.second, it.first) }
    }

    var routeAlternatives by remember { mutableStateOf<List<RouteInfo>>(emptyList()) }
    var isFetchingRoute by remember { mutableStateOf(false) }
    var lastFetchedRouteOrigin by remember { mutableStateOf<Point?>(null) }
    var lastFetchedRouteDest by remember { mutableStateOf<Point?>(null) }
    var lastFetchedTransportMode by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(userPoint, startCoords, destCoords, navigationRouteActive, mapboxToken, transportMode, recordedPath, fixedPlannedRoute) {
        if (recordedPath != null || (fixedPlannedRoute != null && fixedPlannedRoute.isNotEmpty())) {
            routeAlternatives = emptyList()
            isFetchingRoute = false
            return@LaunchedEffect
        }
        
        val originPoint = userPoint ?: (startCoords?.let { Point.fromLngLat(it.second, it.first) })

        if (navigationRouteActive && mapboxToken.isNotBlank() && destCoords != null && originPoint != null) {
            val destinationPoint = Point.fromLngLat(destCoords.second, destCoords.first)

            // Throttling: If mode hasn't changed, origin moved less than 100m, and destination hasn't changed, don't re-fetch from Mapbox API!
            val prevOrigin = lastFetchedRouteOrigin
            val prevDest = lastFetchedRouteDest
            val prevMode = lastFetchedTransportMode
            if (prevMode == transportMode && prevOrigin != null && prevDest == destinationPoint && routeAlternatives.isNotEmpty()) {
                val distanceResults = FloatArray(1)
                android.location.Location.distanceBetween(
                    prevOrigin.latitude(), prevOrigin.longitude(),
                    originPoint.latitude(), originPoint.longitude(),
                    distanceResults
                )
                if (distanceResults[0] < 100f) {
                    return@LaunchedEffect
                }
            }

            isFetchingRoute = true
            val modeName = when (transportMode) {
                0 -> "Walk"
                1 -> "Cycle"
                2 -> "Car"
                3 -> "Bus"
                else -> "Trip"
            }
            onLogReceived("Fetching smart multi-modal routes for $modeName (shortest first, max 5)...")
            val routes = com.safetravel.tracker.util.SmartRoutePlanner.fetchSmartRoutes(
                origin = originPoint,
                destination = destinationPoint,
                transportMode = transportMode,
                mapboxToken = mapboxToken
            )
            if (routes.isNotEmpty()) {
                routeAlternatives = routes
                onRouteChange?.invoke(routes)
                lastFetchedRouteOrigin = originPoint
                lastFetchedRouteDest = destinationPoint
                lastFetchedTransportMode = transportMode
                val selectedRoute = routes.getOrNull(selectedRouteIndex) ?: routes[0]
                val timeStr = if (selectedRoute.durationMin < 60) "${selectedRoute.durationMin.toInt()} min" else "${(selectedRoute.durationMin/60).toInt()} hr ${(selectedRoute.durationMin%60).toInt()} min"
                val distStr = String.format("%.1f km", selectedRoute.distanceKm)
                onTripDetailsCalculated(timeStr, distStr)
            } else {
                val fallbackList = listOf(RouteInfo(listOf(originPoint, destinationPoint), 0.0, 0.0))
                routeAlternatives = fallbackList
                onRouteChange?.invoke(fallbackList)
                lastFetchedTransportMode = transportMode
            }
            isFetchingRoute = false
        } else {
            routeAlternatives = emptyList()
            onRouteChange?.invoke(emptyList())
        }
    }

    // Sync trip details when selectedRouteIndex changes
    LaunchedEffect(selectedRouteIndex, routeAlternatives) {
        if (routeAlternatives.isNotEmpty()) {
            val selectedRoute = routeAlternatives.getOrNull(selectedRouteIndex) ?: routeAlternatives[0]
            val timeStr = if (selectedRoute.durationMin < 60) "${selectedRoute.durationMin.toInt()} min" else "${(selectedRoute.durationMin/60).toInt()} hr ${(selectedRoute.durationMin%60).toInt()} min"
            val distStr = String.format("%.1f km", selectedRoute.distanceKm)
            onTripDetailsCalculated(timeStr, distStr)
        }
    }

    // 5. Initialize Camera Viewport State
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            val centerPoint = when {
                recordedPath != null && recordedPath.isNotEmpty() -> recordedPath.first()
                userPoint != null -> userPoint
                startCoords != null -> Point.fromLngLat(startCoords.second, startCoords.first)
                destCoords != null -> Point.fromLngLat(destCoords.second, destCoords.first)
                else -> null
            }
            
            if (centerPoint != null) {
                center(centerPoint)
                zoom(if (recordedPath != null) 14.0 else 13.5)
            }
            pitch(0.0)
            bearing(0.0)
        }
    }

    // Auto-center on user's current location on map load (once coordinates are acquired)
    LaunchedEffect(resolvedUserCoords) {
        if (recordedPath == null) {
            resolvedUserCoords?.let { coords ->
                if (coords.first != 0.0 && coords.second != 0.0 && !hasCenteredInitially) {
                    onCenteredInitially()
                    mapViewportState.flyTo(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(coords.second, coords.first))
                            .zoom(14.5)
                            .build()
                    )
                }
            }
        }
    }

    // Call onEngineLoaded on launch to satisfy any telemetry triggers
    LaunchedEffect(Unit) {
        onEngineLoaded("Mapbox SDK v11 Native Kotlin Compose")
    }

    Box(modifier = modifier.background(Color(0xFF090D16))) {
        // Native Mapbox Compose Map (hiding ornaments and remembering style lambda to prevent constant reloads)
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = remember { { MapStyle(style = "mapbox://styles/suvosheikh/cmqcnkso0000m01r6ce5y5dch") } },
            logo = {},
            onMapClickListener = OnMapClickListener { point ->
                if (routeAlternatives.size > 1) {
                    for (i in routeAlternatives.indices) {
                        if (isPointNearPolyline(point, routeAlternatives[i].points, toleranceMeters = 150f)) {
                            onRouteSelected?.invoke(i, routeAlternatives[i])
                            return@OnMapClickListener true
                        }
                    }
                }
                onLocationSelected(point.latitude(), point.longitude(), "Locating address... [${point.longitude()}, ${point.latitude()}]")
                true
            }
        ) {
            // Destination custom view annotation using provided drawable (Only if destination exists)
            if (destCoords != null) {
                val destinationPoint = Point.fromLngLat(destCoords.second, destCoords.first)
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature.valueOf(destinationPoint))
                        .allowOverlap(true)
                        .build()
                ) {
                    // To prevent clipping, we use a container twice the height of the icon.
                    // The center of this Box (at 40dp) will be the anchor point on the map.
                    // We place the icon in the TopCenter so its bottom tip is at the anchor point.
                    Box(
                        modifier = Modifier
                            .height(80.dp)
                            .width(40.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.mapdestination),
                            contentDescription = "Destination",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // User beacon live location pulsing view annotation
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "alpha"
            )

            if (recordedPath != null && recordedPath.isNotEmpty()) {
                val startPoint = recordedPath.first()
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature.valueOf(startPoint))
                        .allowOverlap(true)
                        .build()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .background(Emerald400, CircleShape)
                    )
                }
            } else if (startCoords != null) {
                val startPoint = Point.fromLngLat(startCoords.second, startCoords.first)
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature.valueOf(startPoint))
                        .allowOverlap(true)
                        .build()
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .background(Color(0xFF3B82F6), CircleShape), // Blue for starting point
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }
            }

            if (showUserLocation && userPoint != null) {
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature.valueOf(userPoint))
                        .allowOverlap(true)
                        .build()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size((16 * pulseScale).dp)
                                .alpha(pulseAlpha)
                                .background(Color(0xFF007AFF), shape = CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .shadow(2.dp, CircleShape)
                                .background(Color.White, shape = CircleShape)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF007AFF), shape = CircleShape)
                            )
                        }
                    }
                }
            }

            // Live Traveled Trail (Breadcrumbs log from GPS/Supabase)
            if (liveRoutePath != null && liveRoutePath.size >= 2) {
                key("live_traveled_path_${liveRoutePath.size}") {
                    PolylineAnnotation(
                        points = liveRoutePath,
                        lineColorString = "#10B981", // Emerald-500 for traveled trail
                        lineWidth = 5.0,
                        lineOpacity = 0.95
                    )
                }
            }

            // Dynamic route polyline annotation with alternatives
            if (recordedPath != null) {
                key(recordedPath) {
                    PolylineAnnotation(
                        points = recordedPath,
                        lineColorString = "#10B981", // Emerald-500 for history path
                        lineWidth = 6.0,
                        lineOpacity = 1.0
                    )
                }
            } else if (fixedPlannedRoute != null && fixedPlannedRoute.isNotEmpty()) {
                // FIXED / LOCKED PLANNED ROUTE (During active voyage - never changes/reroutes)
                key("fixed_planned_route_${fixedPlannedRoute.size}_${fixedPlannedRoute.first().latitude()}_${fixedPlannedRoute.last().latitude()}") {
                    PolylineAnnotation(
                        points = fixedPlannedRoute,
                        lineColorString = "#E11D48", // Rose-600 for locked planned path
                        lineWidth = 6.0,
                        lineOpacity = 1.0
                    )
                }
            } else if (navigationRouteActive && destCoords != null) {
                val destinationPoint = Point.fromLngLat(destCoords.second, destCoords.first)
                val effectiveOrigin = userPoint ?: (startCoords?.let { Point.fromLngLat(it.second, it.first) })

                if (routeAlternatives.isNotEmpty()) {
                    val activeIdx = selectedRouteIndex.coerceIn(0, routeAlternatives.lastIndex)

                    // Draw alternatives first (bottom layer)
                    routeAlternatives.forEachIndexed { index, route ->
                        if (index != activeIdx && index < 5) { // Show all alternatives up to 5
                            key("alt_${index}_${route.points.hashCode()}") {
                                PolylineAnnotation(
                                    points = route.points,
                                    lineColorString = "#64748B", // Slate-500 for alternatives
                                    lineWidth = 4.5,
                                    lineOpacity = 0.55 // Less highlighted
                                )
                            }
                        }
                    }
                    // Draw selected route on top
                    val primaryRoute = routeAlternatives[activeIdx]
                    key("primary_route_${activeIdx}_${primaryRoute.points.hashCode()}") {
                        PolylineAnnotation(
                            points = primaryRoute.points,
                            lineColorString = "#E11D48", // Rose-600 for primary
                            lineWidth = 6.0,
                            lineOpacity = 1.0 // Fully highlighted
                        )
                    }
                } else if (effectiveOrigin != null) {
                    key("simple_route_${effectiveOrigin.latitude()}_${effectiveOrigin.longitude()}_${destinationPoint.latitude()}_${destinationPoint.longitude()}") {
                        PolylineAnnotation(
                            points = listOf(effectiveOrigin, destinationPoint),
                            lineColorString = "#E11D48",
                            lineWidth = 4.0
                        )
                    }
                }
            }
        }

        // Driving Route Fetching overlay indicator
        if (isFetchingRoute && navigationRouteActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color(0xFF0F172A).copy(alpha = 0.9f), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .border(1.dp, Slate700, androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE11D48),
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Calculating driving route...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Custom Overlay UI: Top-Left Controls (FullScreen Entry/Exit)
        val hasCloseAction = isFullscreen && onCloseFullscreen != null
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .shadow(6.dp, CircleShape)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .border(1.dp, Slate700, CircleShape)
                .clickable {
                    if (hasCloseAction) {
                        onCloseFullscreen?.invoke()
                    } else {
                        isFullscreenOpen = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasCloseAction) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (hasCloseAction) "Exit Full Screen" else "Enter Full Screen",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Custom Overlay UI: Center on current user location trigger
        val coroutineScope = rememberCoroutineScope()
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp + bottomPadding)
                .shadow(6.dp, CircleShape)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .border(1.dp, Slate700, CircleShape)
                .clickable {
                    if (!hasLocationPermission) {
                        showLocationDisclosure = true
                        return@clickable
                    }
                    coroutineScope.launch {
                        try {
                            val loc = LocationHelper.getCurrentLocation(context)
                            val (lat, lng) = if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                                onDeviceLocationChanged(loc.latitude, loc.longitude)
                                Pair(loc.latitude, loc.longitude)
                            } else {
                                resolvedUserCoords ?: startCoords ?: Pair(0.0, 0.0)
                            }
                            if (lat != 0.0 && lng != 0.0) {
                                val targetLoc = Point.fromLngLat(lng, lat)
                                mapViewportState.flyTo(
                                    CameraOptions.Builder()
                                        .center(targetLoc)
                                        .zoom(15.5)
                                        .build()
                                )
                            }
                        } catch (e: Exception) {
                            val fallback = resolvedUserCoords ?: startCoords
                            if (fallback != null && fallback.first != 0.0 && fallback.second != 0.0) {
                                val targetLoc = Point.fromLngLat(fallback.second, fallback.first)
                                mapViewportState.flyTo(
                                    CameraOptions.Builder()
                                        .center(targetLoc)
                                        .zoom(15.0)
                                        .build()
                                )
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Center live location",
                tint = Emerald400,
                modifier = Modifier.size(22.dp)
            )
        }

        // Custom Overlay UI: Panning/Zooming incremental buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = {
                    val currentZoom = mapViewportState.cameraState?.zoom ?: 13.0
                    mapViewportState.flyTo(
                        CameraOptions.Builder()
                            .zoom(currentZoom + 1.0)
                            .build()
                    )
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f), CircleShape)
                    .border(1.dp, Slate700, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
            
            IconButton(
                onClick = {
                    val currentZoom = mapViewportState.cameraState?.zoom ?: 13.0
                    mapViewportState.flyTo(
                        CameraOptions.Builder()
                            .zoom(currentZoom - 1.0)
                            .build()
                    )
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f), CircleShape)
                    .border(1.dp, Slate700, CircleShape)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }

            if (onToggleSafetyToolkit != null) {
                IconButton(
                    onClick = onToggleSafetyToolkit,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))),
                            CircleShape
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Instant Safety Toolkit",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Beautiful location permission fallback dialog shown if permissions are missing
        if (!hasLocationPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16).copy(alpha = 0.95f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Location Access Required",
                        tint = Color(0xFFE11D48), // Rose-600
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "LOCATION ACCESS REQUIRED",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To track and secure your journey, please allow high-accuracy GPS location permission.",
                        color = Slate300,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            showLocationDisclosure = true
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE11D48) // Rose-600
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ALLOW PERMISSION",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Google Play Store Compliant Prominent Location Disclosure
        PermissionDisclosureDialog(
            showDialog = showLocationDisclosure,
            permissionType = SafetyPermissionType.LOCATION,
            onConfirm = {
                showLocationDisclosure = false
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            },
            onDismiss = {
                showLocationDisclosure = false
            }
        )
    }

    // Modal Fullscreen Dialog Overlay
    if (isFullscreenOpen) {
        Dialog(
            onDismissRequest = { isFullscreenOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF090D16))
            ) {
                NativeMapLibreView(
                    destination = destination,
                    onLocationSelected = onLocationSelected,
                    mapboxToken = mapboxToken,
                    modifier = Modifier.fillMaxSize(),
                    liveUserCoordinates = resolvedUserCoords,
                    navigationRouteActive = navigationRouteActive,
                    onEngineLoaded = onEngineLoaded,
                    onLogReceived = onLogReceived,
                    isFullscreen = true,
                    onCloseFullscreen = { isFullscreenOpen = false },
                    destinationCoords = destinationCoords,
                    startCoords = startCoords,
                    liveRoutePath = liveRoutePath,
                    transportMode = transportMode,
                    selectedRouteIndex = selectedRouteIndex,
                    onRouteSelected = onRouteSelected,
                    onRouteChange = onRouteChange,
                    fixedPlannedRoute = fixedPlannedRoute
                )
            }
        }
    }
}
