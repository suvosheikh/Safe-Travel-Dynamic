package com.safetravel.tracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.SupabaseManager
import com.safetravel.tracker.supabase.SupabasePaymentTransaction
import com.safetravel.tracker.supabase.SupabaseSafetyNews
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTrayBottomSheet(
    vm: SafeTravelViewModel,
    onDismiss: () -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onNewsClick: (SupabaseSafetyNews) -> Unit,
    onOpenSosSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeTrip by vm.activeTrip.collectAsState()
    val safetyNews by vm.safetyNews.collectAsState()
    val guardians by vm.userGuardians.collectAsState()
    val profile by vm.userProfile.collectAsState()

    var pendingTx by remember { mutableStateOf<SupabasePaymentTransaction?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }

    // Fetch user pending transaction dynamically
    LaunchedEffect(profile?.id) {
        val uid = profile?.id
        if (uid != null) {
            val txResult = SupabaseManager.fetchUserPaymentTransactions(uid)
            if (txResult.isSuccess) {
                pendingTx = txResult.getOrNull()?.firstOrNull { it.status == "pending" }
            }
        }
    }

    val isTripActive = activeTrip != null && (activeTrip?.status == "ongoing" || activeTrip?.status == "sos")
    val hasGuardians = guardians.isNotEmpty()
    val hasActionableAlerts = isTripActive || pendingTx != null || !hasGuardians

    val filteredNews = remember(safetyNews, selectedCategory) {
        if (selectedCategory == "All") {
            safetyNews
        } else {
            safetyNews.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val totalAlertCount = (if (isTripActive) 1 else 0) + 
                          (if (pendingTx != null) 1 else 0) + 
                          (if (!hasGuardians) 1 else 0) + 
                          safetyNews.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        GlassBottomSheetContainer(
            topStartRadius = 28.dp,
            topEndRadius = 28.dp,
            maxHeightFraction = 0.90f,
            scrollable = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .navigationBarsPadding()
            ) {
                // Top Drag Handle Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp, bottom = 12.dp)
                        .size(width = 44.dp, height = 4.dp)
                        .background(Slate600.copy(alpha = 0.6f), CircleShape)
                )

                // Header with Badge & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Cyan500.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, Cyan500.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Safety Activity Tray",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                if (totalAlertCount > 0) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "$totalAlertCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFCA5A5),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Real-Time Road Advisories & Alerts",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Slate800.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate300,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Alerts & Bulletins List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    // --- SECTION 1: ACTIONABLE PRIORITY CARDS ---
                    if (hasActionableAlerts) {
                        item {
                            Text(
                                text = "ACTION REQUIRED & LIVE STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cyan400,
                                letterSpacing = 1.sp
                            )
                        }

                        // 1. Ongoing Journey Card
                        if (isTripActive) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNavigateToTab(1)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Slate900.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Explore,
                                                    contentDescription = null,
                                                    tint = Emerald400,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "JOURNEY IN PROGRESS",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Emerald400
                                                )
                                            }
                                            Surface(
                                                color = Emerald400.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "LIVE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Emerald400,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Heading to: ${activeTrip?.endLocation?.ifBlank { "Destination" } ?: "Destination"}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Mode: ${(activeTrip?.transportMode ?: "Transit").uppercase()}",
                                                fontSize = 11.sp,
                                                color = Slate400
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Open Tracker",
                                                    fontSize = 11.sp,
                                                    color = Emerald400,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Emerald400,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Pending Verification Card
                        if (pendingTx != null) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNavigateToTab(7)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E1A11),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.HourglassTop,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "PASS VERIFICATION IN REVIEW",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFF59E0B)
                                                )
                                            }
                                            Surface(
                                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "PENDING",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFF59E0B),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Your manual ${pendingTx?.paymentMethod?.uppercase()} payment of BDT ${pendingTx?.amount?.toInt()} is currently under review by our operations room.",
                                            fontSize = 12.sp,
                                            color = Slate300,
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "TrxID: ${pendingTx?.transactionId}",
                                                fontSize = 11.sp,
                                                color = Cyan400,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "View Subscription",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFF59E0B),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. No Guardian Alert Card
                        if (!hasGuardians) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNavigateToTab(3)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF221115),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "No Guardians Enlisted",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Enlist trusted contacts to receive emergency SOS telemetry.",
                                                fontSize = 11.sp,
                                                color = Slate400,
                                                lineHeight = 15.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- SECTION 2: LIVE ROAD & TRAVEL BULLETINS ---
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ROAD & TRAVEL BULLETINS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 1.sp
                            )
                            if (safetyNews.isNotEmpty()) {
                                Text(
                                    text = "${filteredNews.size} updates",
                                    fontSize = 10.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }

                    // Category Filter Pills
                    if (safetyNews.isNotEmpty()) {
                        item {
                            val categories = listOf("All", "Traffic", "Weather", "Police", "General")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(categories) { cat ->
                                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                                    Surface(
                                        color = if (isSelected) Cyan500.copy(alpha = 0.2f) else Slate800.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            1.dp, 
                                            if (isSelected) Cyan500 else Slate700
                                        ),
                                        modifier = Modifier.clickable {
                                            selectedCategory = cat
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Cyan400 else Slate300,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bulletin Cards
                    if (filteredNews.isNotEmpty()) {
                        items(filteredNews) { news ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onNewsClick(news)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = Slate900.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, Slate800)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val (iconVector, iconTint) = when (news.category.lowercase()) {
                                        "weather" -> Icons.Default.CloudQueue to Color(0xFF38BDF8)
                                        "traffic", "road" -> Icons.Default.Traffic to Color(0xFFF59E0B)
                                        "police" -> Icons.Default.LocalPolice to Color(0xFF818CF8)
                                        else -> Icons.Default.Campaign to Cyan400
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = iconTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = Slate800,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = news.category.uppercase(),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = iconTint,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = (news.publishedAt ?: news.createdAt ?: "").take(10),
                                                fontSize = 9.sp,
                                                color = Slate500
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = news.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = news.description,
                                            fontSize = 11.sp,
                                            color = Slate400,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else if (safetyNews.isEmpty() && !hasActionableAlerts) {
                        // Honest Empty State (Rule 3)
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Slate900.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Slate800)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Emerald400.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Emerald400,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "All Clear & Secured",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "No active road alerts, emergency warnings, or pending transactions at this time.",
                                        fontSize = 11.sp,
                                        color = Slate400,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Footer: Quick Safety Settings Link
                Surface(
                    color = Slate900.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenSosSettings()
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Configure SOS Hold & Safety Dispatch Settings",
                                fontSize = 11.sp,
                                color = Slate300
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
