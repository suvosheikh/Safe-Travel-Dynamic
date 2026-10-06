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
import kotlinx.coroutines.Dispatchers
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
    var plannedPath by remember { mutableStateOf<List<Point>>(emptyList()) }

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

    // Determine initial camera center
    val initialCenter = remember(startPoint, recordedPath) {
        startPoint ?: recordedPath?.firstOrNull() ?: Point.fromLngLat(90.4125, 23.8103)
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(initialCenter)
            zoom(14.5)
            pitch(0.0)
            bearing(0.0)
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
            // 1. Draw planned path (Red Line - Background)
            if (plannedPath.isNotEmpty()) {
                key(plannedPath) {
                    PolylineAnnotation(
                        points = plannedPath,
                        lineColorString = "#EF4444", // Tailwind Red-500
                        lineWidth = 4.0,
                        lineOpacity = 0.6
                    )
                }
            }

            // 2. Draw recorded path (Actual Path - Blue with white core)
            if (!recordedPath.isNullOrEmpty() && recordedPath.size >= 2) {
                key(recordedPath) {
                    // Secondary blue glow path
                    PolylineAnnotation(
                        points = recordedPath,
                        lineColorString = "#3B82F6", // Primary Blue
                        lineWidth = 6.0,
                        lineOpacity = 1.0
                    )
                    // Main white core path for neon effect
                    PolylineAnnotation(
                        points = recordedPath,
                        lineColorString = "#FFFFFF",
                        lineWidth = 2.0,
                        lineOpacity = 0.9
                    )
                }
            }

            // 3. Markers (Teardrop Design from Image)
            val actualStart = startPoint ?: recordedPath?.firstOrNull()
            if (actualStart != null) {
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature.valueOf(actualStart))
                        .allowOverlap(true)
                        .build()
                ) {
                    HistoryTeardropMarker(color = Color(0xFF10B981), icon = Icons.Default.Flag) // Green for start
                }
            }

            val actualEnd = endPoint ?: recordedPath?.lastOrNull()
            if (actualEnd != null) {
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature.valueOf(actualEnd))
                        .allowOverlap(true)
                        .build()
                ) {
                    HistoryTeardropMarker(color = Color(0xFF6366F1), icon = Icons.Default.Place) // Blue/Purple for end
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
