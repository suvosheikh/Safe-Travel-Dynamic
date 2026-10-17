package com.safetravel.tracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.SupabaseTripShare
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.util.AddressUtils
import com.safetravel.tracker.util.DateTimeUtils
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@Composable
fun GuardianCenterScreen(vm: SafeTravelViewModel, onWatchTrip: (String) -> Unit) {
    val selectedTab by vm.guardianCenterSelectedTab.collectAsState()
    val shares by vm.incomingTripShares.collectAsState()
    val userProfile by vm.userProfile.collectAsState()
    val isPremium = userProfile?.isPremium == true
    val context = LocalContext.current
    val alertAdInterval = remember { com.safetravel.tracker.ads.AdMobManager.getAlertCardInterval(context) }
    val alertAdSize = remember { com.safetravel.tracker.ads.AdMobManager.getAlertCardSize(context) }

    LaunchedEffect(Unit) {
        vm.refreshIncomingShares()
    }

    LaunchedEffect(selectedTab) {
        vm.refreshIncomingShares()
    }

    val sosShares = remember(shares) {
        shares.filter { 
            (it.status == "sos" || it.tripDetails?.status == "sos") && 
            it.status != "completed" && it.status != "ended" && it.tripDetails?.status != "completed"
        }
    }

    val liveShares = remember(shares) {
        shares.filter { 
            (it.status == "active" || it.tripDetails?.status == "ongoing") && 
            it.status != "sos" && it.tripDetails?.status != "sos" &&
            it.status != "completed" && it.status != "ended" && it.tripDetails?.status != "completed"
        }
    }

    val historyShares = remember(shares) {
        shares.filter { 
            it.status == "completed" || it.status == "ended" || it.tripDetails?.status == "completed" || 
            (it.status != "active" && it.status != "sos" && it.tripDetails?.status != "ongoing" && it.tripDetails?.status != "sos")
        }
    }

    // Auto-focus SOS or Live Watch only on initial entry if not yet switched
    LaunchedEffect(liveShares.size, sosShares.size) {
        if (!vm.hasAutoSwitchedGuardianTab) {
            if (sosShares.isNotEmpty()) {
                vm.guardianCenterSelectedTab.value = 0 // Critical SOS distress takes top priority
                vm.hasAutoSwitchedGuardianTab = true
            } else if (liveShares.isNotEmpty()) {
                vm.guardianCenterSelectedTab.value = 1
                vm.hasAutoSwitchedGuardianTab = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
    ) {
        // --- FROSTED CYBER TAB HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xCC0F172A), Color(0xD9090D16))))
                .border(1.dp, Color(0x3306B6D4), RoundedCornerShape(14.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple("SOS", sosShares.size, Color(0xFFEF4444)), 
                Triple("Live Watch", liveShares.size, Color(0xFF06B6D4)), 
                Triple("History", historyShares.size, Color(0xFF10B981))
            )
            tabs.forEachIndexed { index, (title, count, activeColor) ->
                val isSelected = selectedTab == index
                val tabModifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(activeColor.copy(alpha = 0.25f), activeColor.copy(alpha = 0.12f))
                                )
                            )
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        vm.guardianCenterSelectedTab.value = index
                        vm.hasAutoSwitchedGuardianTab = true
                    }
                    .padding(vertical = 9.dp)

                Box(
                    modifier = tabModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) activeColor else Slate400,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            maxLines = 1
                        )
                        if (count > 0) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(activeColor)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$count",
                                    color = Slate950,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> SosSection(vm, sosShares, onWatchTrip)
            1 -> LiveWatchSection(liveShares, isPremium, alertAdInterval, alertAdSize, onWatchTrip)
            2 -> HistorySection(historyShares, isPremium, alertAdInterval, alertAdSize, onWatchTrip)
        }
    }
}

@Composable
fun SosSection(
    vm: SafeTravelViewModel,
    sosAlerts: List<SupabaseTripShare> = emptyList(),
    onWatchTrip: (String) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- INCOMING SOS DISTRESS ALERTS ---
        if (sosAlerts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFEF4444), CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ACTIVE EMERGENCY ALERTS (${sosAlerts.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEF4444),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            items(sosAlerts) { share ->
                val trip = share.tripDetails
                val tripId = trip?.id ?: share.tripId
                val sharedUser = share.sharedByProfile
                val phone = sharedUser?.phoneNumber
                val locationAddress = AddressUtils.formatDisplayAddress(trip?.startAddress ?: trip?.startLocation ?: "Live GPS Coordinates")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.85f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Header: Avatar, Name & SOS Active Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x33EF4444)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = sharedUser?.fullName ?: "Someone in your network",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (!phone.isNullOrBlank()) {
                                        Text(
                                            text = phone,
                                            color = Slate400,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color(0xFFEF4444),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(4.dp).background(Color.White, CircleShape))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SOS ACTIVE",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Location Details
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = locationAddress,
                                    color = Slate200,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Timestamp row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = DateTimeUtils.formatAppDateTime(share.createdAt ?: trip?.createdAt),
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons Row: Track Live Map & Direct Call
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onWatchTrip(tripId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Track Live Map", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }

                            if (!phone.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(dialIntent)
                                        } catch (_: Exception) {}
                                    },
                                    border = BorderStroke(1.dp, Slate600),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Emerald400, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Call Now", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Reassuring Empty State
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Slate800.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Emerald400.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Your Network is Safe",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "No emergency distress signals are active in your network. When a family member or friend triggers an SOS, their live location and alert will sound here.",
                            color = Slate400,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveWatchSection(
    activeShares: List<SupabaseTripShare>,
    isPremium: Boolean = false,
    adInterval: Int = 3,
    adSize: String = "large_banner",
    onWatchTrip: (String) -> Unit
) {
    if (activeShares.isEmpty()) {
        EmptyState(
            title = "No Live Journeys Active",
            icon = Icons.Outlined.Visibility,
            description = "When a family member or contact starts a journey with live sharing, their real-time route, speed, and GPS will appear here."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(CyberCyan, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ONGOING JOURNEYS (${activeShares.size})",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            itemsIndexed(activeShares) { index, share ->
                LiveWatchTripCard(share = share, onWatchTrip = onWatchTrip)
                if ((index + 1) % adInterval == 0) {
                    com.safetravel.tracker.ads.AdCardView(
                        modifier = Modifier.padding(vertical = 4.dp),
                        isPremium = isPremium,
                        formatKey = "alert_card",
                        sizeType = adSize
                    )
                }
            }
        }
    }
}

@Composable
fun LiveWatchTripCard(share: SupabaseTripShare, onWatchTrip: (String) -> Unit) {
    val trip = share.tripDetails
    val tripId = trip?.id ?: share.tripId
    val destination = AddressUtils.formatDisplayAddress(trip?.endAddress ?: trip?.endLocation)
    val origin = AddressUtils.formatDisplayAddress(trip?.startAddress ?: trip?.startLocation ?: "Current Origin")
    val travelerName = share.sharedByProfile?.fullName ?: "Someone"
    val mode = trip?.transportMode ?: "car"

    val modeIcon = when (mode.lowercase()) {
        "walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "cycling", "bike" -> Icons.AutoMirrored.Filled.DirectionsBike
        else -> Icons.Default.DirectionsCar
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWatchTrip(tripId) },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Traveler Name & Live Now Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(modeIcon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = travelerName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Traveling via ${mode.replaceFirstChar { it.uppercase() }}",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = CyberCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(4.dp).background(CyberCyan, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE NOW",
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Origin -> Destination Route Flow
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Slate900.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Emerald400, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = origin,
                            color = Slate300,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFF43F5E), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = destination,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom action row: Timestamp and Watch Live button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateTimeUtils.formatAppDateTime(share.createdAt ?: trip?.createdAt),
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Watch Map",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun HistorySection(
    pastShares: List<SupabaseTripShare>,
    isPremium: Boolean = false,
    adInterval: Int = 3,
    adSize: String = "large_banner",
    onWatchTrip: (String) -> Unit
) {
    val sortedPastShares = remember(pastShares) {
        pastShares.sortedWith(
            compareByDescending<SupabaseTripShare> { it.createdAt ?: it.tripDetails?.createdAt ?: "" }
                .thenByDescending { it.id }
        )
    }

    if (sortedPastShares.isEmpty()) {
        EmptyState(
            title = "No Shared History Yet",
            icon = Icons.Outlined.History,
            description = "Records of completed journeys and resolved alerts shared with you will be archived here."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "PAST SHARED JOURNEYS (${sortedPastShares.size})",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
            itemsIndexed(
                items = sortedPastShares,
                key = { _, share -> share.id }
            ) { index, share ->
                PastTripShareCard(share = share, onWatchTrip = onWatchTrip)
                if ((index + 1) % adInterval == 0) {
                    com.safetravel.tracker.ads.AdCardView(
                        modifier = Modifier.padding(vertical = 4.dp),
                        isPremium = isPremium,
                        formatKey = "alert_card",
                        sizeType = adSize
                    )
                }
            }
        }
    }
}

@Composable
fun PastTripShareCard(share: SupabaseTripShare, onWatchTrip: (String) -> Unit) {
    val trip = share.tripDetails
    val tripId = trip?.id ?: share.tripId
    val destination = AddressUtils.formatDisplayAddress(trip?.endAddress ?: trip?.endLocation)
    val origin = AddressUtils.formatDisplayAddress(trip?.startAddress ?: trip?.startLocation ?: "Origin")
    val travelerName = share.sharedByProfile?.fullName ?: "Someone"
    val isSosResolved = share.status == "sos" || trip?.status == "sos"
    val mode = trip?.transportMode ?: "car"

    val modeIcon = when (mode.lowercase()) {
        "walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "cycling", "bike" -> Icons.AutoMirrored.Filled.DirectionsBike
        else -> Icons.Default.DirectionsCar
    }

    val statusColor = if (isSosResolved) Color(0xFFF59E0B) else Emerald400
    val statusText = if (isSosResolved) "SOS RESOLVED" else "COMPLETED SAFELY"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWatchTrip(tripId) },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Mode Avatar, Traveler Name & Resolution Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(modeIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = travelerName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Traveled via ${mode.replaceFirstChar { it.uppercase() }}",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(4.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Origin -> Destination Route Flow Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Slate900.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Emerald400, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = origin,
                            color = Slate300,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFF43F5E), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = destination,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom action row: Timestamp and View Route button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateTimeUtils.formatAppDateTime(share.createdAt ?: trip?.createdAt),
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "View Route",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, description: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Slate800.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(28.dp), tint = Slate500)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = Slate400,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
    }
}
