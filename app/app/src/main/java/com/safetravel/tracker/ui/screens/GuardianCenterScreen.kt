package com.safetravel.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.SupabaseTripShare
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.util.AddressUtils
import com.safetravel.tracker.util.DateTimeUtils
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@Composable
fun GuardianCenterScreen(vm: SafeTravelViewModel, onWatchTrip: (String) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val shares by vm.incomingTripShares.collectAsState()
    val userProfile by vm.userProfile.collectAsState()
    val isPremium = userProfile?.isPremium == true
    val context = androidx.compose.ui.platform.LocalContext.current
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

    // Auto-focus Live Watch if there are active shares and not manually chosen
    var hasAutoSwitched by remember { mutableStateOf(false) }
    LaunchedEffect(liveShares.size, sosShares.size) {
        if (sosShares.isNotEmpty()) {
            selectedTab = 0 // Critical SOS distress takes top priority
        } else if (liveShares.isNotEmpty() && !hasAutoSwitched) {
            selectedTab = 1
            hasAutoSwitched = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
    ) {
        // Custom Tab Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(Slate800, RoundedCornerShape(12.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Pair("SOS", sosShares.size), 
                Pair("Live Watch", liveShares.size), 
                Pair("History", historyShares.size)
            )
            tabs.forEachIndexed { index, (title, count) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Emerald400 else Color.Transparent)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFF022C22) else Slate300,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (count > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val badgeBg = if (index == 0) Color(0xFFEF4444) else if (isSelected) Color(0xFF022C22) else Emerald400
                            val badgeText = if (index == 0) Color.White else if (isSelected) Emerald400 else Color(0xFF022C22)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(badgeBg)
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "$count",
                                    color = badgeText,
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Emergency & Alerts Center",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Broadcast SOS coordinates to your guardian network in case of emergency.",
                fontSize = 12.sp,
                color = Slate300
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "SOS",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Trigger Direct SOS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pressing below immediately shares your live location with your emergency guardians.",
                        fontSize = 12.sp,
                        color = Slate300,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { vm.triggerSos() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Broadcast Emergency SOS", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (sosAlerts.isNotEmpty()) {
            item {
                Text(
                    text = "Emergency SOS Alerts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(sosAlerts) { share ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onWatchTrip(share.tripId) },
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EMERGENCY: ${share.sharedByProfile?.fullName ?: "Someone"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Emergency alert triggered! Tap to track their live location immediately.",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFEF4444).copy(0.85f), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatShareTimestamp(share.createdAt ?: share.tripDetails?.createdAt),
                                    color = Slate400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SOS ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
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
        EmptyState("No Active Shared Trips", Icons.Outlined.Visibility, "When someone shares their live journey with you, it will appear here.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Ongoing Journeys", color = Emerald400, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
            }
            itemsIndexed(activeShares) { index, share ->
                SharedTripCard(share, onWatchTrip)
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
fun HistorySection(
    pastShares: List<SupabaseTripShare>,
    isPremium: Boolean = false,
    adInterval: Int = 3,
    adSize: String = "large_banner",
    onWatchTrip: (String) -> Unit
) {
    if (pastShares.isEmpty()) {
        EmptyState("No Shared History", Icons.Outlined.History, "Records of completed journeys shared with you will be archived here.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Past Shared Journeys", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
            }
            itemsIndexed(pastShares) { index, share ->
                SharedTripCard(share, onWatchTrip = onWatchTrip)
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

fun formatShareTimestamp(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return "Active now"
    return DateTimeUtils.formatBstShort(isoTimestamp).ifBlank { "Active now" }
}

@Composable
fun SharedTripCard(share: SupabaseTripShare, onWatchTrip: (String) -> Unit) {
    val trip = share.tripDetails
    val tripId = trip?.id ?: share.tripId
    val destination = AddressUtils.formatDisplayAddress(trip?.endAddress ?: trip?.endLocation)
    val isSos = share.status == "sos" || trip?.status == "sos"
    val isActive = (share.status == "active" || trip?.status == "ongoing") && !isSos

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWatchTrip(tripId) },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSos) Color(0xFFEF4444).copy(0.6f) else if (isActive) Emerald400.copy(0.3f) else Slate700)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isSos) Color(0x33EF4444) else Slate900),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSos) Icons.Default.Warning else Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSos) Color(0xFFEF4444) else Emerald400
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(share.sharedByProfile?.fullName ?: "Someone", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Traveling to $destination", color = Slate400, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Slate400, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatShareTimestamp(share.createdAt ?: trip?.createdAt),
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
                if (isSos) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFEF4444), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SOS EMERGENCY", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                } else if (isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Emerald400, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LIVE NOW", color = Emerald400, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            if (isSos || isActive) {
                Icon(Icons.Default.ChevronRight, null, tint = if (isSos) Color(0xFFEF4444) else Slate600)
            }
        }
    }
}

@Composable
fun EmptyState(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, description: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = Slate700)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(description, color = Slate400, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
