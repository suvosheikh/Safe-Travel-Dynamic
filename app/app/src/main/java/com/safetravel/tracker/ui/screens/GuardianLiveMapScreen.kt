package com.safetravel.tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.safetravel.tracker.ui.components.travel.MapboxView
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.util.AddressUtils
import com.safetravel.tracker.util.DateTimeUtils
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@Composable
fun GuardianLiveMapScreen(vm: SafeTravelViewModel, tripId: String, onBack: () -> Unit) {
    val trip by vm.selectedSharedTrip.collectAsState()
    val mapboxToken by vm.mapboxToken.collectAsState()
    val incomingShares by vm.incomingTripShares.collectAsState()

    var isSheetExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(tripId) {
        vm.observeSharedTrip(tripId)
    }

    // Resolve trip details with immediate fallback from cache if trip is still fetching
    val activeTripDetails = remember(trip, incomingShares) {
        trip ?: incomingShares.find { it.tripId == tripId }?.tripDetails
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

    Box(modifier = Modifier.fillMaxSize().background(Slate950)) {
        // 1. Unconditional Native Map Canvas
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
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (activeTripDetails?.status == "sos") Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (activeTripDetails?.status == "sos") "RED ALERT ACTIVE" else if (activeTripDetails != null) "Live GPS Beacon Active" else "Connecting to Beacon...",
                            color = if (activeTripDetails?.status == "sos") Color(0xFFEF4444) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = activeTripDetails?.vehiclePlateNumber ?: activeTripDetails?.vehicleDescription ?: "Syncing real-time coordinates...",
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
                                    imageVector = if (activeTripDetails?.status == "sos") Icons.Default.Warning else Icons.Default.DirectionsRun,
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
                        Divider(color = Slate800, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(18.dp))

                        // Live Telemetry Grid (Battery, Device Model, Status)
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

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = Slate800, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(20.dp))

                        // Start & Destination Visual Address Timeline
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimelineAddressItem(
                                label = "Departure Point",
                                address = AddressUtils.formatDisplayAddress(activeTripDetails?.startAddress ?: "Current GPS Departure Point"),
                                isStart = true
                            )
                            TimelineAddressItem(
                                label = "Estimated Destination",
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
        "walk", "walking" -> TransitModeInfo("Walking", Icons.Default.DirectionsWalk, Color(0xFF10B981))
        "cycling", "cycle", "bike" -> TransitModeInfo("Cycling", Icons.Default.DirectionsBike, Color(0xFF3B82F6))
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

