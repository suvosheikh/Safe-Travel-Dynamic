package com.safetravel.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.R
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel
import com.safetravel.tracker.supabase.SupabaseTrip
import com.safetravel.tracker.ui.components.travel.HistoryMapView
import com.safetravel.tracker.ui.components.GlassBottomSheetContainer
import com.safetravel.tracker.ui.components.GlassDragHandle
import com.safetravel.tracker.util.AddressUtils
import com.safetravel.tracker.util.DateTimeUtils
import com.safetravel.tracker.ui.components.AudioBlackboxCard
import com.safetravel.tracker.supabase.SupabaseSafetyAudioLog
import com.mapbox.geojson.Point
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: SafeTravelViewModel) {
    val pastTrips by vm.pastTrips.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    
    val totalDist by vm.totalDistanceTraveled.collectAsState()
    val successRate by vm.safetySuccessRate.collectAsState()
    val totalPoints by vm.totalPointsAccrued.collectAsState()
    val safetyAudioLogs by vm.safetyAudioLogs.collectAsState()
    val profile by vm.userProfile.collectAsState()
    val isPremium = profile?.isPremium == true
    val context = androidx.compose.ui.platform.LocalContext.current
    val historyAdInterval = remember { com.safetravel.tracker.ads.AdMobManager.getHistoryCardInterval(context) }
    val historyAdSize = remember { com.safetravel.tracker.ads.AdMobManager.getHistoryCardSize(context) }

    val sortedTrips = remember(pastTrips) {
        pastTrips.sortedByDescending { it.createdAt }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Trips, 1: Voice Vault
    var selectedTrip by remember { mutableStateOf<SupabaseTrip?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        vm.refreshData()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Slate900),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                Text(
                    text = "Activity & Safety Vault",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "View your past journeys and audio evidence logs.",
                    fontSize = 13.sp,
                    color = Slate400,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // Segmented Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Slate800)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 0: Trips
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 0) Emerald500 else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.White else Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Trips (${sortedTrips.size})",
                                color = if (selectedTab == 0) Color.White else Slate400,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Tab 1: Voice Vault
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == 1) Cyan500 else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.White else Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voice Vault (${safetyAudioLogs.size})",
                                color = if (selectedTab == 1) Color.White else Slate400,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (selectedTab == 0) {
                // 1. Analytics Dashboard
                item {
                    VoyageAnalyticsCard(
                        totalDist = totalDist,
                        successRate = successRate,
                        totalPoints = totalPoints
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Text(
                        text = "Recent Trips",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald400,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (isLoading && pastTrips.isEmpty()) {
                    items(5) { HistorySkeletonCard() }
                } else if (pastTrips.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.History, null, tint = Slate700, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No trip records found.", color = Slate500, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    itemsIndexed(sortedTrips) { index, rawTrip ->
                        var tripState by remember { mutableStateOf(rawTrip) }
                        LaunchedEffect(rawTrip) {
                            tripState = vm.resolveTripAddresses(rawTrip)
                        }
                        
                        EnhancedTripCard(
                            trip = tripState,
                            onClick = { selectedTrip = tripState }
                        )

                        if ((index + 1) % historyAdInterval == 0) {
                            com.safetravel.tracker.ads.AdCardView(
                                modifier = Modifier.padding(vertical = 8.dp),
                                isPremium = isPremium,
                                formatKey = "history_card",
                                sizeType = historyAdSize
                            )
                        }
                    }
                }
            } else {
                // 2. Voice Vault (Silent Audio Blackbox Logs)
                item {
                    Text(
                        text = "Safety Voice Blackbox",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Cyan400,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "High-compressed encrypted audio clips synchronized to Cloudinary & Supabase.",
                        fontSize = 12.sp,
                        color = Slate400,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (isLoading && safetyAudioLogs.isEmpty()) {
                    items(3) { HistorySkeletonCard() }
                } else if (safetyAudioLogs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 50.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MicOff, null, tint = Slate700, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No audio evidence recorded yet.", color = Slate400, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Use Silent Audio in Safety Toolkit or trigger SOS to capture evidence.", color = Slate500, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(safetyAudioLogs, key = { it.id }) { log ->
                        AudioBlackboxCard(
                            audioLog = log,
                            onDelete = { vm.deleteSafetyAudioLog(log) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            if (selectedTab == 1 || pastTrips.isEmpty()) {
                item {
                    com.safetravel.tracker.ads.AdBannerView(
                        modifier = Modifier.padding(top = 16.dp),
                        isPremium = isPremium
                    )
                }
            }
        }
    }

    // Detail Bottom Sheet
    if (selectedTrip != null) {
        TripDetailSheet(
            trip = selectedTrip!!,
            vm = vm,
            sheetState = sheetState,
            onDismiss = { selectedTrip = null }
        )
    }
}

@Composable
fun VoyageAnalyticsCard(totalDist: Double, successRate: Int, totalPoints: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Trip Statistics", color = Slate300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.AutoAwesome, null, tint = PremiumGold, modifier = Modifier.size(16.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                AnalyticsStatItem(
                    label = "Total Dist",
                    value = "%.1f".format(totalDist),
                    unit = "km",
                    icon = Icons.Default.Route,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 12.dp), color = Slate700)
                AnalyticsStatItem(
                    label = "Safety Rate",
                    value = "$successRate",
                    unit = "%",
                    icon = Icons.Default.Shield,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 12.dp), color = Slate700)
                AnalyticsStatItem(
                    label = "Accrued",
                    value = "$totalPoints",
                    unit = "pts",
                    icon = Icons.Default.Token,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun AnalyticsStatItem(label: String, value: String, unit: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Emerald400, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = unit, color = Slate400, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
        Text(text = label, color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EnhancedTripCard(trip: SupabaseTrip, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isCompleted = trip.status == "completed"
                val statusColor = if (isCompleted) Emerald400 else Red500
                
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (isCompleted) "SUCCESSFUL ARRIVAL" else "STATUS: ${trip.status.uppercase()}",
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Text(
                    text = DateTimeUtils.formatBstDateTime(trip.createdAt, "dd MMM yyyy, hh:mm a"),
                    color = Slate500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location Timeline
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RadioButtonChecked, null, tint = Emerald400, modifier = Modifier.size(14.dp))
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Slate700))
                    Icon(Icons.Default.Place, null, tint = Red500, modifier = Modifier.size(14.dp))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                val startDisp = AddressUtils.formatDisplayAddress(trip.startLocation)
                val endDisp = AddressUtils.formatDisplayAddress(trip.endLocation)
                
                Column {
                    Text(
                        text = startDisp,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = endDisp,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Divider(color = Slate700, thickness = 1.dp, modifier = Modifier.padding(vertical = 16.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TripStat(Icons.Default.Timer, trip.durationString)
                    TripStat(Icons.Default.Route, trip.totalDistanceFormatted)
                    TripStat(Icons.Default.BatteryChargingFull, trip.batteryConsumed)
                }
                
                Icon(
                    imageVector = when(trip.transportMode?.lowercase()) {
                        "walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
                        "cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
                        "bus" -> Icons.Default.DirectionsBus
                        else -> Icons.Default.DirectionsCar
                    },
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TripStat(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Slate500, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = value, color = Slate300, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailSheet(
    trip: SupabaseTrip,
    vm: SafeTravelViewModel,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val mapboxToken by vm.mapboxToken.collectAsState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        GlassBottomSheetContainer(
            topStartRadius = 32.dp,
            topEndRadius = 32.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
            ) {
            Text(
                text = "Trip Summary",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            val bstCreatedAt = DateTimeUtils.formatBstDateTime(trip.createdAt, "dd MMM yyyy, hh:mm a")
            Text(
                text = "Trip ID: #${trip.id.take(8).uppercase()}" + if (bstCreatedAt.isNotBlank()) " • $bstCreatedAt" else "",
                color = Slate500,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Origin & Destination Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate800.copy(alpha = 0.6f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("FROM", color = Emerald400, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(AddressUtils.formatDisplayAddress(trip.startLocation), color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(DateTimeUtils.formatBstTime(trip.createdAt).ifBlank { "---" }, color = Slate400, fontSize = 10.sp)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Slate500, modifier = Modifier.padding(horizontal = 8.dp).size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("TO", color = Red500, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(AddressUtils.formatDisplayAddress(trip.endLocation), color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val endTimeStr = if (trip.endTime != null) DateTimeUtils.formatBstTime(trip.endTime) else "In Progress"
                    Text(endTimeStr, color = Slate400, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini Map View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val recordedPoints = trip.routePathLog?.mapNotNull { 
                    val lat = it["lat"]
                    val lng = it["lng"]
                    if (lat != null && lng != null) Point.fromLngLat(lng, lat) else null
                }

                val startP = trip.startCoords?.let { 
                    val c = vm.getCoordsForAddress(it)
                    if (c != null) Point.fromLngLat(c.second, c.first) else null
                }
                
                val endP = trip.endCoords?.let { 
                    val c = vm.getCoordsForAddress(it)
                    if (c != null) Point.fromLngLat(c.second, c.first) else null
                }

                val plannedPoints = trip.plannedRouteLog?.mapNotNull { item ->
                    val lat = item["lat"]
                    val lng = item["lng"]
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        Point.fromLngLat(lng, lat)
                    } else null
                }

                HistoryMapView(
                    startPoint = startP,
                    endPoint = endP,
                    recordedPath = recordedPoints,
                    mapboxToken = mapboxToken,
                    transportMode = trip.transportMode,
                    plannedRoute = plannedPoints,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Detailed Info Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoColumn(
                    label = "VEHICLE INFO",
                    value = trip.vehiclePlateNumber ?: "N/A",
                    subValue = trip.vehicleDescription ?: "No description",
                    modifier = Modifier.weight(1f)
                )
                InfoColumn(
                    label = "DEVICE & BATTERY",
                    value = trip.deviceModel ?: "Unknown",
                    subValue = "Battery: ${trip.startBatteryLevel}% → ${trip.endBatteryLevel}%",
                    modifier = Modifier.weight(1f)
                )
            }

            val safetyAudioLogs by vm.safetyAudioLogs.collectAsState()
            val tripAudioLog = remember(trip.id, trip.audioClipUrl, safetyAudioLogs) {
                safetyAudioLogs.find { it.tripId == trip.id }
                    ?: if (!trip.audioClipUrl.isNullOrBlank()) {
                        SupabaseSafetyAudioLog(
                            id = "trip_${trip.id}",
                            userId = trip.userId,
                            tripId = trip.id,
                            audioUrl = trip.audioClipUrl,
                            durationSec = 0,
                            recordedAddress = trip.startLocation,
                            sourceTrigger = "trip_bound",
                            createdAt = trip.createdAt
                        )
                    } else null
            }

            if (tripAudioLog != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "TRIP AUDIO BLACKBOX",
                    color = Cyan400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                AudioBlackboxCard(
                    audioLog = tripAudioLog,
                    onDelete = { vm.deleteSafetyAudioLog(tripAudioLog) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { /* Share Logic */ },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Trip")
                }
                
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.weight(1f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald400),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Dismiss", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
}

@Composable
fun InfoColumn(label: String, value: String, subValue: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = Slate500, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(text = subValue, color = Slate400, fontSize = 12.sp)
    }
}

@Composable
fun HistorySkeletonCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(shimmerBrush()))
    }
}
