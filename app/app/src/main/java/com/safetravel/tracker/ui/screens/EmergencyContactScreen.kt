package com.safetravel.tracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.Guardian
import com.safetravel.tracker.supabase.SupabaseHotline
import com.safetravel.tracker.ui.components.GlassBottomSheetContainer
import com.safetravel.tracker.ui.components.GlassDragHandle
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactScreen(vm: SafeTravelViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val hotlines by vm.hotlines.collectAsState()
    val guardians by vm.userGuardians.collectAsState()
    val searchQuery by vm.emergencySearchQuery.collectAsState()
    val currentLocationName by vm.currentLocationName.collectAsState()
    val currentPosition by vm.currentPosition.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val haptic = LocalHapticFeedback.current

    var selectedHotline by remember { mutableStateOf<SupabaseHotline?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        vm.refreshHotlines()
        vm.refreshGuardians()
    }

    val filteredHotlines = remember(hotlines, searchQuery) {
        hotlines.filter { it.name?.contains(searchQuery, ignoreCase = true) == true || it.category?.contains(searchQuery, ignoreCase = true) == true }
    }

    val filteredGuardians = remember(guardians, searchQuery) {
        guardians.filter { it.name.contains(searchQuery, ignoreCase = true) || it.relationship.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Slate900)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 0. LOCATION INDICATOR
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Red500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Area: ", color = Slate400, fontSize = 12.sp)
                    Text(
                        text = if (currentLocationName.isNotBlank()) currentLocationName else "Detecting...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 1. SEARCH BAR (Glassmorphic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Slate800.copy(alpha = 0.5f))
                    .border(BorderStroke(1.dp, Slate700), RoundedCornerShape(16.dp))
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { vm.emergencySearchQuery.value = it },
                    placeholder = { Text("Search emergency services...", color = Slate500, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.emergencySearchQuery.value = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Slate400)
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // 2. HEADER TABS
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = if (selectedTab == 0) Red500 else Color(0xFF3B82F6),
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 0 
                    },
                    text = { Text("OFFICIAL", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 1 
                    },
                    text = { Text("PERSONAL", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
                )
            }

            // 3. ANIMATED CONTENT
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }.using(SizeTransform(clip = false))
                },
                label = "TabAnimation",
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Red500)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (targetTab == 0) {
                            // Highlight 999 as Hero if available
                            val heroHotline = filteredHotlines.find { it.phone == "999" }
                            if (heroHotline != null && searchQuery.isEmpty()) {
                                item {
                                    HeroHotlineCard(heroHotline) { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedHotline = it 
                                    }
                                }
                            }
                            
                            items(filteredHotlines.filter { it.phone != "999" }) { hotline ->
                                HotlineCard(hotline) { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedHotline = it 
                                }
                            }
                        } else {
                            if (filteredGuardians.isEmpty()) {
                                item { EmptyGuardiansView() }
                            } else {
                                items(filteredGuardians) { guardian ->
                                    GuardianCard(guardian, currentPosition)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. OFFICIAL HOTLINE BOTTOM SHEET
    if (selectedHotline != null) {
        OfficialHotlineBottomSheet(
            hotline = selectedHotline!!,
            sheetState = sheetState,
            onDismiss = { selectedHotline = null }
        )
    }
}

@Composable
fun HeroHotlineCard(hotline: SupabaseHotline, onClick: (SupabaseHotline) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(hotline) },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, Brush.linearGradient(listOf(Red500, Color(0xFFF97316))))
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Red500.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Red500,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hotline.name ?: "",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Most Trusted National Service",
                    color = Red500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = hotline.phone ?: "",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun HotlineCard(hotline: SupabaseHotline, onClick: (SupabaseHotline) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(hotline) },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate700.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = Slate300,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hotline.name ?: "Hotline",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = hotline.description ?: hotline.category ?: "",
                    color = Slate400,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Slate500,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GuardianCard(guardian: Guardian, currentPosition: Pair<Double, Double>?) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = guardian.name.take(1).uppercase(),
                    color = Color(0xFF3B82F6),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = guardian.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = guardian.relationship,
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick SMS with Location
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val locQuery = if (currentPosition != null) "${currentPosition.first},${currentPosition.second}" else "current+location"
                        val message = "Emergency! I need help. My current location: https://www.google.com/maps/search/?api=1&query=$locQuery"
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:${guardian.phone}")
                            putExtra("sms_body", message)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate700)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "SMS Location", tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${guardian.phone}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialHotlineBottomSheet(
    hotline: SupabaseHotline,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isEnglish by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        GlassBottomSheetContainer(
            topStartRadius = 28.dp,
            topEndRadius = 28.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Neon Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Red500.copy(alpha = 0.1f))
                    .border(BorderStroke(2.dp, Red500), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = Red500,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = hotline.name ?: "",
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = hotline.phone ?: "",
                color = Slate400,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Preparation Tips Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Preparation Tips" else "কল করার আগে প্রস্তুতি",
                            color = Slate300,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (isEnglish) SfProFontFamily else HindSiliguriFontFamily
                        )
                        TextButton(onClick = { isEnglish = !isEnglish }) {
                            Text(
                                text = if (isEnglish) "বাংলা" else "English",
                                color = Red500,
                                fontSize = 11.sp,
                                fontFamily = if (isEnglish) HindSiliguriFontFamily else SfProFontFamily
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val tips = if (isEnglish) {
                        listOf(
                            "Stay calm and speak clearly.",
                            "Provide your precise location.",
                            "Describe the emergency in short."
                        )
                    } else {
                        listOf(
                            "শান্ত থাকুন এবং স্পষ্টভাবে কথা বলুন।",
                            "আপনার সঠিক অবস্থান জানান।",
                            "সংক্ষেপে আপনার সমস্যার কথা বলুন।"
                        )
                    }

                    tips.forEach { tip ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("•", color = Red500, modifier = Modifier.padding(end = 8.dp))
                            Text(
                                text = tip,
                                color = Slate100,
                                fontSize = 13.sp,
                                fontFamily = if (isEnglish) SfProFontFamily else HindSiliguriFontFamily
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Massive Dial Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hotline.phone}"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Red500),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "DIAL ${hotline.phone}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}
}

@Composable
fun EmptyGuardiansView() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = Slate700, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Personal Guardians Found",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add your trusted contacts from the 'Profile' tab to notify them during emergencies.",
            color = Slate400,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
