package com.safetravel.tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.safetravel.tracker.ui.components.travel.HistoryMapView
import com.safetravel.tracker.ui.components.travel.MapboxView
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.util.AddressUtils
import com.safetravel.tracker.util.DateTimeUtils
import com.safetravel.tracker.util.MapRouteUtils
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@Composable
fun GuardianLiveMapScreen(vm: SafeTravelViewModel, tripId: String, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler {
        onBack()
    }

    val trip by vm.selectedSharedTrip.collectAsState()
    val mapboxToken by vm.mapboxToken.collectAsState()
    val incomingShares by vm.incomingTripShares.collectAsState()

    var isSheetExpanded by remember { mutableStateOf(true) }

    DisposableEffect(tripId) {
        vm.observeSharedTrip(tripId)
        onDispose {
            vm.stopObservingSharedTrip()
        }
    }

    // Resolve trip details with immediate fallback from cache if trip is still fetching
    val activeTripDetails = remember(trip, incomingShares, tripId) {
        if (trip?.id == tripId) trip
        else incomingShares.find { it.tripId == tripId }?.tripDetails
    }

    val liveCoords = remember(activeTripDetails) {
        if (activeTripDetails?.currentLat != null && activeTripDetails.currentLng != null && activeTripDetails.currentLat != 0.0) {
            Pair(activeTripDetails.currentLat, activeTripDetails.currentLng)
        } else {
            activeTripDetails?.routePathLog?.lastOrNull()?.let { 
                val lat = it["lat"] ?: 0.0
                val lng = it["lng"] ?: 0.0
                if (lat != 0.0 && lng != 0.0) Pair(lat, lng) else null
            }
        }
    }

    val startCoords = remember(activeTripDetails) {
        activeTripDetails?.startCoords?.let { coords ->
            try {
                val parts = coords.split(",")
                val lat = parts[0].trim().toDouble()
                val lng = parts[1].trim().toDouble()
                if (lat > 50.0) Pair(lng, lat) else Pair(lat, lng)
            } catch (e: Exception) {
                null
            }
        } ?: activeTripDetails?.routePathLog?.firstOrNull()?.let {
            val lat = it["lat"] ?: 0.0
            val lng = it["lng"] ?: 0.0
            if (lat != 0.0 && lng != 0.0) Pair(lat, lng) else null
        }
    }

    val modeInt = remember(activeTripDetails) {
        when (activeTripDetails?.transportMode?.lowercase()) {
            "walk", "walking" -> 0
            "cycling", "cycle", "bike" -> 1
            "driving", "car" -> 2
            "transit", "bus" -> 3
            else -> 2
        }
    }

    val destinationAddress = remember(activeTripDetails) {
        val dest = activeTripDetails?.endAddress ?: activeTripDetails?.endLocation ?: ""
        if (dest.isBlank() || dest.equals("destination", ignoreCase = true) || 
            dest.equals("not specified", ignoreCase = true) || 
            dest.equals("live beacon navigation", ignoreCase = true) ||
            dest.equals("dhanmondi, dhaka", ignoreCase = true)) {
            ""
        } else {
            dest
        }
    }

    val destinationCoords = remember(activeTripDetails, destinationAddress) {
        activeTripDetails?.endCoords?.let { AddressUtils.extractCoordinates(it) }
            ?: AddressUtils.extractCoordinates(destinationAddress)
    }

    val liveRoutePoints = remember(activeTripDetails?.routePathLog) {
        activeTripDetails?.routePathLog?.mapNotNull { item ->
            val lat = item["lat"]
            val lng = item["lng"]
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                Point.fromLngLat(lng, lat)
            } else null
        }
    }

    val fixedPlannedRoutePoints = remember(activeTripDetails?.plannedRouteLog) {
        activeTripDetails?.plannedRouteLog?.mapNotNull { item ->
            val lat = item["lat"]
            val lng = item["lng"]
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                Point.fromLngLat(lng, lat)
            } else null
        }?.takeIf { it.isNotEmpty() }
    }

    val isCompletedTrip = remember(activeTripDetails?.status, activeTripDetails?.endTime) {
        val s = activeTripDetails?.status?.lowercase()
        s == "completed" || s == "ended" || activeTripDetails?.endTime != null
    }

    val allPoints = remember(liveRoutePoints, startCoords, destinationCoords) {
        val list = mutableListOf<Point>()
        if (!liveRoutePoints.isNullOrEmpty()) {
            list.addAll(liveRoutePoints)
        } else {
            startCoords?.let { list.add(Point.fromLngLat(it.second, it.first)) }
            destinationCoords?.let { list.add(Point.fromLngLat(it.second, it.first)) }
        }
        list
    }

    val tripMetrics = remember(allPoints, activeTripDetails?.createdAt, activeTripDetails?.endTime) {
        MapRouteUtils.calculateTripMetrics(allPoints, activeTripDetails?.createdAt, activeTripDetails?.endTime)
    }

    Box(modifier = Modifier.fillMaxSize().background(Slate950)) {
        // 1. Map Canvas: If journey is completed, render HistoryMapView with auto-fit bounds & replay; otherwise MapboxView for live tracking
        if (isCompletedTrip) {
            val startP = startCoords?.let { Point.fromLngLat(it.second, it.first) }
            val endP = destinationCoords?.let { Point.fromLngLat(it.second, it.first) }

            key(tripId) {
                HistoryMapView(
                    startPoint = startP,
                    endPoint = endP,
                    recordedPath = liveRoutePoints,
                    mapboxToken = mapboxToken,
                    transportMode = activeTripDetails?.transportMode,
                    plannedRoute = fixedPlannedRoutePoints,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            MapboxView(
                destination = destinationAddress,
                destinationCoords = destinationCoords,
                startCoords = startCoords,
                liveRoutePath = liveRoutePoints,
                fixedPlannedRoute = fixedPlannedRoutePoints,
                onLocationSelected = { _, _, _ -> },
                mapboxToken = mapboxToken,
                liveUserCoordinates = liveCoords,
                navigationRouteActive = (destinationAddress.isNotBlank() || destinationCoords != null),
                modifier = Modifier.fillMaxSize(),
                transportMode = modeInt,
                hasCenteredInitially = vm.mapHasCenteredInitially,
                onCenteredInitially = { vm.mapHasCenteredInitially = true }
            )
        }

        // 2. Top Glassmorphic Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(46.dp)
                    .background(Slate900.copy(0.85f), RoundedCornerShape(12.dp))
                    .border(1.dp, Slate700, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900.copy(0.85f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate700),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSos = activeTripDetails?.status == "sos"
                    val statusDotColor = when {
                        isSos -> Color(0xFFEF4444)
                        isCompletedTrip -> Color(0xFF10B981)
                        else -> Color(0xFF06B6D4)
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        val headerTitle = when {
                            isSos -> "RED ALERT ACTIVE"
                            isCompletedTrip -> "JOURNEY COMPLETED"
                            activeTripDetails != null -> "Live GPS Beacon Active"
                            else -> "Connecting to Beacon..."
                        }
                        val headerSubtitle = when {
                            isCompletedTrip -> {
                                val endT = activeTripDetails?.endTime ?: activeTripDetails?.createdAt
                                if (!endT.isNullOrBlank()) "Finished at " + DateTimeUtils.formatAppDateTime(endT)
                                else "Trip Completed Safely"
                            }
                            else -> activeTripDetails?.vehiclePlateNumber ?: activeTripDetails?.vehicleDescription ?: "Syncing real-time coordinates..."
                        }
                        Text(
                            text = headerTitle,
                            color = if (isSos) Color(0xFFEF4444) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = headerSubtitle,
                            color = Slate400,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 3. Floating Quick Action Controls (Right side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Toggle Bottom Sheet button
            IconButton(
                onClick = { isSheetExpanded = !isSheetExpanded },
                modifier = Modifier
                    .size(46.dp)
                    .background(Slate900.copy(0.88f), RoundedCornerShape(14.dp))
                    .border(1.dp, Slate700, RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = if (isSheetExpanded) Icons.Default.LayersClear else Icons.Default.Layers,
                    contentDescription = "Toggle Sheet",
                    tint = Emerald400
                )
            }
        }

        // 4. Bottom Details Telemetry Sheet (Glassmorphic)
        if (isSheetExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xD90F172A)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0x5538BDF8), Color(0x15334155))
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // User Profile & Status Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = if (activeTripDetails?.status == "sos") {
                                                listOf(Color(0xFFEF4444), Color(0xFFB91C1C))
                                            } else {
                                                listOf(Emerald400, Color(0xFF047857))
                                            }
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activeTripDetails?.status == "sos") Icons.Default.Warning else Icons.AutoMirrored.Filled.DirectionsRun,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SECURED TRAVEL LOG",
                                    color = if (activeTripDetails?.status == "sos") Color(0xFFEF4444) else Emerald400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = activeTripDetails?.vehicleDescription ?: "Active Beacon Session",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Dynamic Transit Mode Chip
                            val modeInfo = getTransitModeInfo(activeTripDetails?.transportMode)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(modeInfo.color.copy(alpha = 0.15f))
                                    .border(1.dp, modeInfo.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(modeInfo.icon, null, tint = modeInfo.color, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = modeInfo.title.uppercase(),
                                        color = modeInfo.color,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        HorizontalDivider(color = Slate800, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(18.dp))

                        // Telemetry Grid (If completed: Distance, Duration, Speed, Battery; If active: live telemetry)
                        if (isCompletedTrip) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TelemetryGridItem(
                                    label = "Distance",
                                    value = tripMetrics.distanceFormatted,
                                    valueColor = Color(0xFF06B6D4),
                                    icon = Icons.Default.Straighten
                                )
                                TelemetryGridItem(
                                    label = "Duration",
                                    value = tripMetrics.durationFormatted,
                                    valueColor = Emerald400,
                                    icon = Icons.Default.Timer
                                )
                                TelemetryGridItem(
                                    label = "Avg Speed",
                                    value = tripMetrics.speedFormatted,
                                    valueColor = Color.White,
                                    icon = Icons.Default.Speed
                                )
                                val startB = activeTripDetails?.startBatteryLevel ?: 100
                                val endB = activeTripDetails?.endBatteryLevel ?: startB
                                TelemetryGridItem(
                                    label = "Battery Used",
                                    value = "$startB% -> $endB%",
                                    valueColor = Slate300,
                                    icon = Icons.Default.BatteryStd
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val battery = activeTripDetails?.startBatteryLevel ?: 100
                                val batteryColor = when {
                                    battery <= 20 -> Color(0xFFEF4444)
                                    battery <= 50 -> Color(0xFFF59E0B)
                                    else -> Color(0xFF10B981)
                                }
                                TelemetryGridItem(
                                    label = "Device Battery",
                                    value = "$battery%",
                                    valueColor = batteryColor,
                                    icon = Icons.Default.BatteryChargingFull
                                )
                                TelemetryGridItem(
                                    label = "Device Model",
                                    value = activeTripDetails?.deviceModel ?: "Android Device",
                                    valueColor = Color.White,
                                    icon = Icons.Default.PhoneAndroid
                                )
                                TelemetryGridItem(
                                    label = "Trip Status",
                                    value = (activeTripDetails?.status ?: "ACTIVE").uppercase(),
                                    valueColor = if (activeTripDetails?.status == "sos") Color(0xFFEF4444) else Color(0xFF10B981),
                                    icon = Icons.Default.Security
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Slate800, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(20.dp))

                        // Start & Destination Visual Address Timeline
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val startLabel = if (isCompletedTrip && !activeTripDetails?.createdAt.isNullOrBlank()) {
                                "Departure Point • " + DateTimeUtils.formatAppDateTime(activeTripDetails.createdAt)
                            } else "Departure Point"

                            val endLabel = if (isCompletedTrip && !activeTripDetails?.endTime.isNullOrBlank()) {
                                "Arrival Destination • " + DateTimeUtils.formatAppDateTime(activeTripDetails.endTime)
                            } else "Estimated Destination"

                            TimelineAddressItem(
                                label = startLabel,
                                address = AddressUtils.formatDisplayAddress(activeTripDetails?.startAddress ?: "Current GPS Departure Point"),
                                isStart = true
                            )
                            TimelineAddressItem(
                                label = endLabel,
                                address = AddressUtils.formatDisplayAddress(activeTripDetails?.endAddress ?: activeTripDetails?.endLocation),
                                isStart = false
                            )
                        }
                    }
                }
            }
        }
    }
}

data class TransitModeInfo(val title: String, val icon: ImageVector, val color: Color)

fun getTransitModeInfo(mode: String?): TransitModeInfo {
    return when (mode?.lowercase()) {
        "walk", "walking" -> TransitModeInfo("Walking", Icons.AutoMirrored.Filled.DirectionsWalk, Color(0xFF10B981))
        "cycling", "cycle", "bike" -> TransitModeInfo("Cycling", Icons.AutoMirrored.Filled.DirectionsBike, Color(0xFF3B82F6))
        "driving", "car" -> TransitModeInfo("Driving", Icons.Default.DirectionsCar, Color(0xFFF59E0B))
        "transit", "bus" -> TransitModeInfo("Bus Transit", Icons.Default.DirectionsBus, Color(0xFF8B5CF6))
        else -> TransitModeInfo("In Vehicle", Icons.Default.DirectionsCar, Color(0xFFE2E8F0))
    }
}

@Composable
fun TelemetryGridItem(label: String, value: String, valueColor: Color, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = Slate400, modifier = Modifier.size(12.dp))
            Text(label.uppercase(), color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun TimelineAddressItem(label: String, address: String, isStart: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 2.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isStart) Color(0xFF3B82F6) else Color(0xFFE11D48))
            )
            if (isStart) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(18.dp)
                        .background(Slate700)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                address,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

