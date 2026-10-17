package com.safetravel.tracker.ui.components.travel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mapbox.geojson.Point
import com.mapbox.common.MapboxOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.AnnotatedFeature
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.safetravel.tracker.R
import com.safetravel.tracker.ui.theme.Emerald400
import com.safetravel.tracker.ui.theme.Slate700
import com.safetravel.tracker.ui.theme.Slate800
import com.safetravel.tracker.ui.theme.Slate900
import com.safetravel.tracker.util.MapRouteUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun HistoryMapView(
    startPoint: Point?,
    endPoint: Point?,
    recordedPath: List<Point>?,
    mapboxToken: String,
    transportMode: String? = "driving",
    plannedRoute: List<Point>? = null,
    modifier: Modifier = Modifier
) {
    var isFullscreenOpen by remember { mutableStateOf(false) }

    HistoryMapContent(
        startPoint = startPoint,
        endPoint = endPoint,
        recordedPath = recordedPath,
        mapboxToken = mapboxToken,
        transportMode = transportMode,
        plannedRoute = plannedRoute,
        modifier = modifier,
        isFullscreen = false,
        onToggleFullscreen = { isFullscreenOpen = true }
    )

    if (isFullscreenOpen) {
        Dialog(
            onDismissRequest = { isFullscreenOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Slate900) {
                HistoryMapContent(
                    startPoint = startPoint,
                    endPoint = endPoint,
                    recordedPath = recordedPath,
                    mapboxToken = mapboxToken,
                    transportMode = transportMode,
                    plannedRoute = plannedRoute,
                    modifier = Modifier.fillMaxSize(),
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreenOpen = false }
                )
            }
        }
    }
}

@Composable
fun HistoryMapContent(
    startPoint: Point?,
    endPoint: Point?,
    recordedPath: List<Point>?,
    mapboxToken: String,
    transportMode: String? = "driving",
    plannedRoute: List<Point>? = null,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit
) {
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    if (isPreview) {
        Box(modifier = modifier.background(Slate800), contentAlignment = Alignment.Center) {
            Text("Map Preview", color = Color.White)
        }
        return
    }

    LaunchedEffect(mapboxToken) {
        if (mapboxToken.isNotBlank()) {
            MapboxOptions.accessToken = mapboxToken
        }
    }

    // Interactive states
    var is3DMode by remember { mutableStateOf(false) }
    var plannedPath by remember(startPoint, endPoint, plannedRoute) { 
        mutableStateOf<List<Point>>(plannedRoute ?: emptyList()) 
    }

    // Fetch planned path (Red Line) - If plannedRoute is stored in database, use it directly (0 API calls!)
    LaunchedEffect(startPoint, endPoint, transportMode, mapboxToken, plannedRoute) {
        if (!plannedRoute.isNullOrEmpty()) {
            plannedPath = plannedRoute
            return@LaunchedEffect
        }
        if (startPoint != null && endPoint != null && mapboxToken.isNotBlank()) {
            val profile = when (transportMode?.lowercase()) {
                "walking" -> "walking"
                "cycling" -> "cycling"
                else -> "driving"
            }
            
            withContext(Dispatchers.IO) {
                try {
                    val urlStr = "https://api.mapbox.com/directions/v5/mapbox/$profile/${startPoint.longitude()},${startPoint.latitude()};${endPoint.longitude()},${endPoint.latitude()}?geometries=geojson&overview=full&access_token=$mapboxToken"
                    val url = URL(urlStr)
                    val conn = url.openConnection() as HttpURLConnection
                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val root = org.json.JSONObject(response)
                        val routes = root.optJSONArray("routes")
                        if (routes != null && routes.length() > 0) {
                            val geometry = routes.getJSONObject(0).optJSONObject("geometry")
                            val coordinates = geometry?.optJSONArray("coordinates")
                            if (coordinates != null) {
                                val points = mutableListOf<Point>()
                                for (i in 0 until coordinates.length()) {
                                    val coord = coordinates.getJSONArray(i)
                                    points.add(Point.fromLngLat(coord.getDouble(0), coord.getDouble(1)))
                                }
                                withContext(Dispatchers.Main) {
                                    plannedPath = points
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // 1. Calculate complete route bounds for auto-fit camera
    val allRoutePoints = remember(startPoint, endPoint, recordedPath, plannedPath) {
        val list = mutableListOf<Point>()
        startPoint?.let { list.add(it) }
        endPoint?.let { list.add(it) }
        recordedPath?.let { list.addAll(it) }
        if (list.size < 2 && plannedPath.isNotEmpty()) {
            list.addAll(plannedPath)
        }
        list
    }

    val boundsInfo = remember(allRoutePoints) {
        MapRouteUtils.calculateRouteBounds(allRoutePoints, startPoint)
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(boundsInfo.center)
            zoom(boundsInfo.zoom)
            pitch(0.0)
            bearing(0.0)
        }
    }

    // Auto-fit camera smoothly whenever bounds are ready
    LaunchedEffect(boundsInfo) {
        mapViewportState.flyTo(
            CameraOptions.Builder()
                .center(boundsInfo.center)
                .zoom(boundsInfo.zoom)
                .build()
        )
    }

    // 2. Route Replay State
    var isPlaying by remember(startPoint, endPoint, recordedPath) { mutableStateOf(false) }
    var replayIndex by remember(startPoint, endPoint, recordedPath) { mutableIntStateOf(0) }
    val replayPoints = remember(recordedPath, plannedPath) {
        if (!recordedPath.isNullOrEmpty() && recordedPath.size >= 2) recordedPath
        else plannedPath
    }

    LaunchedEffect(isPlaying, replayPoints) {
        if (isPlaying && replayPoints.size >= 2) {
            while (isPlaying) {
                delay(120)
                if (replayIndex < replayPoints.size - 1) {
                    replayIndex++
                } else {
                    delay(800)
                    replayIndex = 0
                }
            }
        }
    }

    Box(modifier = modifier.then(if (!isFullscreen) Modifier.clip(RoundedCornerShape(24.dp)).border(1.dp, Slate700, RoundedCornerShape(24.dp)) else Modifier)) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style = { MapStyle(style = "mapbox://styles/suvosheikh/cmqcnkso0000m01r6ce5y5dch") },
            logo = {},
            attribution = {}
        ) {
            // 1. Draw planned path (Subtle Red/Coral Line - Background)
            if (plannedPath.isNotEmpty()) {
                key(plannedPath) {
                    PolylineAnnotation(
                        points = plannedPath,
                        lineColorString = "#EF4444",
                        lineWidth = 4.0,
                        lineOpacity = 0.5
                    )
                }
            }

            // 2. Draw recorded path (Glowing Cyber Cyan with Bright Emerald Core)
            if (!recordedPath.isNullOrEmpty() && recordedPath.size >= 2) {
                key(recordedPath) {
                    // Outer Cyber Cyan Glow
                    PolylineAnnotation(
                        points = recordedPath,
                        lineColorString = "#06B6D4",
                        lineWidth = 7.0,
                        lineOpacity = 0.85
                    )
                    // Inner Bright Emerald Neon Core
                    PolylineAnnotation(
                        points = recordedPath,
                        lineColorString = "#10B981",
                        lineWidth = 3.0,
                        lineOpacity = 1.0
                    )
                }
            }

            // 3. Start Marker (Emerald Green Flag Pin)
            val actualStart = startPoint ?: recordedPath?.firstOrNull()
            if (actualStart != null) {
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature.valueOf(actualStart))
                        .allowOverlap(true)
                        .build()
                ) {
                    HistoryTeardropMarker(color = Color(0xFF10B981), icon = Icons.Default.Flag)
                }
            }

            // 4. End Marker (High-Contrast Red Destination Pin)
            val actualEnd = endPoint ?: recordedPath?.lastOrNull()
            if (actualEnd != null) {
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature.valueOf(actualEnd))
                        .allowOverlap(true)
                        .build()
                ) {
                    HistoryTeardropMarker(color = Color(0xFFEF4444), icon = Icons.Default.Place)
                }
            }

            // 5. Moving Animated Replay Vehicle Marker
            val currentReplayPoint = if (isPlaying || replayIndex > 0) replayPoints.getOrNull(replayIndex) else null
            if (currentReplayPoint != null) {
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature.valueOf(currentReplayPoint))
                        .allowOverlap(true)
                        .build()
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF06B6D4))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val transportIcon = when (transportMode?.lowercase()) {
                            "walking" -> Icons.Default.DirectionsWalk
                            "cycling", "bike" -> Icons.Default.DirectionsBike
                            else -> Icons.Default.DirectionsCar
                        }
                        Icon(
                            imageVector = transportIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        // --- OVERLAY CONTROLS ---

        // 1. Archive Mode Label (Top End)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Slate900.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .border(1.dp, Emerald400.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(Emerald400, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("TRIP ROUTE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }

        // 2. Fullscreen Button (Top Left - User Requested)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            MapControlButton(icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen) {
                onToggleFullscreen()
            }
        }

        // 3. Right Control Bar (Zoom, 3D)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Zoom In
            MapControlButton(icon = Icons.Default.Add) {
                val currentZoom = mapViewportState.cameraState?.zoom ?: 14.5
                mapViewportState.flyTo(CameraOptions.Builder().zoom(currentZoom + 1.0).build())
            }

            // Zoom Out
            MapControlButton(icon = Icons.Default.Remove) {
                val currentZoom = mapViewportState.cameraState?.zoom ?: 14.5
                mapViewportState.flyTo(CameraOptions.Builder().zoom(currentZoom - 1.0).build())
            }

            // 3D Perspective Toggle
            MapControlButton(
                icon = Icons.Default.ViewInAr,
                tint = if (is3DMode) Emerald400 else Color.White
            ) {
                is3DMode = !is3DMode
                mapViewportState.flyTo(
                    CameraOptions.Builder()
                        .pitch(if (is3DMode) 60.0 else 0.0) // Deeper tilt for history
                        .zoom(if (is3DMode) 15.5 else 14.5)
                        .build()
                )
            }
        }

        // 4. Interactive Route Replay Floating Bar
        if (replayPoints.size >= 2) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isFullscreen) 32.dp else 12.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Slate900.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color(0x5506B6D4))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "Replaying..." else "Replay Route",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = if (replayPoints.isNotEmpty()) replayIndex.toFloat() / (replayPoints.size - 1) else 0f,
                        onValueChange = { frac ->
                            isPlaying = false
                            replayIndex = (frac * (replayPoints.size - 1)).toInt()
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF06B6D4),
                            activeTrackColor = Color(0xFF06B6D4),
                            inactiveTrackColor = Slate700
                        ),
                        modifier = Modifier.width(110.dp).height(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryTeardropMarker(color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .width(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Teardrop Shape
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val path = android.graphics.Path().apply {
                moveTo(size.width / 2f, size.height) // Tip
                cubicTo(
                    size.width, size.height * 0.7f,
                    size.width, 0f,
                    size.width / 2f, 0f
                )
                cubicTo(
                    0f, 0f,
                    0f, size.height * 0.7f,
                    size.width / 2f, size.height
                )
                close()
            }
            drawContext.canvas.nativeCanvas.drawPath(
                path,
                android.graphics.Paint().apply {
                    this.color = color.toArgb()
                    this.isAntiAlias = true
                    this.setShadowLayer(8f, 0f, 4f, android.graphics.Color.BLACK)
                }
            )
        }
        
        // Inner Circle & Icon
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(24.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = Slate900.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
