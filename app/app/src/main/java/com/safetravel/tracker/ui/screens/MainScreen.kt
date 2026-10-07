package com.safetravel.tracker.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import com.safetravel.tracker.ui.components.*

import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.ui.screens.*
import com.safetravel.tracker.supabase.SupabaseSafetyNews
import androidx.compose.runtime.collectAsState
import com.safetravel.tracker.ui.components.ExitBottomSheet
import com.safetravel.tracker.viewmodel.SafeTravelViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import com.safetravel.tracker.R

import com.safetravel.tracker.MainActivity
import com.safetravel.tracker.ui.components.*
@Composable
fun MainTabbedLayout(vm: SafeTravelViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val profile by vm.userProfile.collectAsState()
    val guardians by vm.userGuardians.collectAsState()
    val isConnected by vm.isSupabaseConnected.collectAsState()
    val activeTrip by vm.activeTrip.collectAsState()
    val newsList by vm.safetyNews.collectAsState()
    
    // Drawer & Dialog state controllers
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Google Play Compliant Permission Disclosure & Wizard States
    val wizardPrefs = remember { context.getSharedPreferences("safetravel_wizard_prefs", android.content.Context.MODE_PRIVATE) }
    var hasSeenWizard by remember { mutableStateOf(wizardPrefs.getBoolean("has_seen_wizard", false)) }

    var showLocationDisclosureDialog by remember { mutableStateOf(false) }
    var showNotificationDisclosureDialog by remember { mutableStateOf(false) }
    var showSafetyWizardDialog by remember { mutableStateOf(false) }
    var showActionLocationWizard by remember { mutableStateOf(false) }
    var pendingActionScreen by remember { mutableStateOf<String?>(null) }
    var pendingTrackingTab by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Exit Sheet State
    var showExitSheet by remember { mutableStateOf(false) }
    
    // News Detail Navigation State
    var selectedNews by remember { mutableStateOf<SupabaseSafetyNews?>(null) }
    
    // Weather Navigation State
    var showWeather by remember { mutableStateOf(false) }

    // Thana Navigation State
    var showThana by remember { mutableStateOf(false) }

    // Safety Tips Navigation State
    var showSafetyTips by remember { mutableStateOf(false) }
    var selectedTip by remember { mutableStateOf<com.safetravel.tracker.supabase.SupabaseSafetyTip?>(null) }

    // Emergency Contact Navigation State
    var showEmergencyContacts by remember { mutableStateOf(false) }

    // Fire Station Navigation State
    var showFireService by remember { mutableStateOf(false) }

    // Hospital Navigation State
    var showHospitals by remember { mutableStateOf(false) }

    // Blood Bank Navigation State
    var showBloodBanks by remember { mutableStateOf(false) }

    // Version Log Navigation State
    var showVersionLog by remember { mutableStateOf(false) }
    
    // Guardian Tracking State
    var watchTripId by remember { mutableStateOf<String?>(null) }

    // Read pending trip id from activity intent (e.g. from notification click)
    val activity = LocalContext.current as? MainActivity
    LaunchedEffect(activity?.pendingTripId) {
        activity?.pendingTripId?.let { tripId ->
            watchTripId = tripId
            activity.pendingTripId = null // Clear after reading
        }
    }

    // Helper to clear sub-navigation states
    fun resetSubScreens() {
        showVersionLog = false
        showWeather = false
        showThana = false
        showFireService = false
        showHospitals = false
        showBloodBanks = false
        showSafetyTips = false
        showEmergencyContacts = false
        selectedTip = null
        selectedNews = null
        watchTripId = null
    }

    // Quick Action screen opener helper
    fun openActionScreen(label: String) {
        when (label) {
            "Weather Alerts" -> showWeather = true
            "Thana" -> showThana = true
            "Fire Service" -> showFireService = true
            "Nearby Hospitals" -> showHospitals = true
            "Blood Bank" -> showBloodBanks = true
            "Emergency Contacts" -> showEmergencyContacts = true
            "Safety Tips" -> showSafetyTips = true
        }
    }

    // Global Location Permission Handling with Prominent Disclosure
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            vm.startContinuousLocationUpdates()
            // If Tracking tab was clicked, navigate to it now!
            if (pendingTrackingTab) {
                pendingTrackingTab = false
                selectedTab = 1
            }
            // If a quick action was waiting for permission, open it now!
            pendingActionScreen?.let {
                openActionScreen(it)
                pendingActionScreen = null
            }
            // If location was granted and notification is pending on Android 13+, contextually offer notification disclosure
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                showNotificationDisclosureDialog = true
            }
        } else {
            pendingTrackingTab = false
        }
    }

    // Notification Permission Handling
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            vm.startContinuousLocationUpdates()
        } else if (!hasSeenWizard) {
            // Smooth timing: allow Home Screen to settle before displaying introductory wizard
            delay(750)
            showSafetyWizardDialog = true
        }
    }

    // Unified Back Handling Logic
    BackHandler(enabled = !showExitSheet) {
        if (selectedTip != null) {
            selectedTip = null
        } else if (showSafetyTips) {
            showSafetyTips = false
        } else if (showEmergencyContacts) {
            showEmergencyContacts = false
        } else if (showVersionLog) {
            showVersionLog = false
        } else if (showWeather) {
            showWeather = false
        } else if (showThana) {
            showThana = false
        } else if (showHospitals) {
            showHospitals = false
        } else if (showBloodBanks) {
            showBloodBanks = false
        } else if (selectedNews != null) {
            selectedNews = null
        } else if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (selectedTab != 0) {
            selectedTab = 0
        } else {
            showExitSheet = true
        }
    }

    // Exit Bottom Sheet
    if (showExitSheet) {
        ExitBottomSheet(
            onDismiss = { showExitSheet = false },
            onExit = { (context as? Activity)?.finish() }
        )
    }
    
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showNotificationTray by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedTab != 1,
        scrimColor = Color(0x6604020C),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Transparent,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight(),
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                // Frosted Dark-Glass Surface matching app theme
                GlassBackground(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                ) {
                    // Translucent frosted glass tint overlay to ensure high contrast for text while letting ambient neon glows shine through
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x331E1B4B), // Subtle frosted indigo tint
                                        Color(0x240F0C24), // Translucent deep space
                                        Color(0x38070514)  // Translucent bottom
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        // 1. USER PROFILE HEADER (Clickable -> Profile)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.09f),
                                            Color.White.copy(alpha = 0.03f)
                                        )
                                    )
                                )
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    resetSubScreens()
                                    selectedTab = 2
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Avatar with Status Indicator
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!profile?.avatarUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = profile?.avatarUrl,
                                            contentDescription = "Profile Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = profile?.fullName?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                // Online / Protected Indicator Dot
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF090717))
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile?.fullName?.ifBlank { "Civilian Traveler" } ?: "Civilian Traveler",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (profile?.isPremium == true) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFFB703).copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "PRO",
                                                color = Color(0xFFFFB703),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = profile?.phoneNumber?.ifBlank { "View Profile" } ?: "View Profile",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { scope.launch { drawerState.close() } },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Drawer",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. ESSENTIAL NAVIGATION MENU ITEMS (Clean, Single-line, No Clutter)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            SidebarMenuItem(
                                icon = Icons.Default.Shield,
                                iconTint = Color(0xFFEF4444),
                                label = "Emergency Numbers & SOS",
                                isSelected = showEmergencyContacts,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    resetSubScreens()
                                    showEmergencyContacts = true
                                }
                            )

                            SidebarMenuItem(
                                icon = Icons.Default.People,
                                iconTint = Color(0xFF10B981),
                                label = "Guardians & Live Watch",
                                isSelected = selectedTab == 3 && !showEmergencyContacts && !showSafetyTips,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    resetSubScreens()
                                    selectedTab = 3
                                }
                            )

                            SidebarMenuItem(
                                icon = Icons.Default.Lightbulb,
                                iconTint = Color(0xFFF59E0B),
                                label = "Safety Guidelines",
                                isSelected = showSafetyTips,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    resetSubScreens()
                                    showSafetyTips = true
                                }
                            )

                            SidebarMenuItem(
                                icon = Icons.Default.Article,
                                iconTint = Color(0xFF06B6D4),
                                label = "Safety News & Alerts",
                                badgeCount = newsList.size,
                                isSelected = selectedTab == 0 && !showEmergencyContacts && !showSafetyTips,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    resetSubScreens()
                                    selectedTab = 0
                                }
                            )

                            SidebarMenuItem(
                                icon = Icons.Default.History,
                                iconTint = Color(0xFF3B82F6),
                                label = "Trip History",
                                isSelected = selectedTab == 4 && !showEmergencyContacts && !showSafetyTips,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    resetSubScreens()
                                    selectedTab = 4
                                }
                            )

                            SidebarMenuItem(
                                icon = Icons.Default.Star,
                                iconTint = Color(0xFFFFB703),
                                label = "Safe Journey VIP",
                                badgeText = if (profile?.isPremium != true) "UPGRADE" else null,
                                badgeColor = Color(0xFFFFB703),
                                isSelected = selectedTab == 7 && !showEmergencyContacts && !showSafetyTips,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    resetSubScreens()
                                    selectedTab = 7
                                }
                            )

                            SidebarMenuItem(
                                icon = Icons.Default.Settings,
                                iconTint = Color(0xFF94A3B8),
                                label = "Notification Settings",
                                isSelected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    showNotificationDialog = true
                                }
                            )

                            SidebarMenuItem(
                                icon = Icons.Default.HelpCenter,
                                iconTint = Color(0xFF8B5CF6),
                                label = "Help & Support",
                                isSelected = selectedTab == 6 && !showEmergencyContacts && !showSafetyTips,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    resetSubScreens()
                                    selectedTab = 6
                                }
                            )
                        }

                        // 3. FOOTER ACTIONS & BRANDING (100% Borderless)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.07f))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sign Out Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x14EF4444))
                                .clickable {
                                    scope.launch { drawerState.close() }
                                    showLogoutDialog = true
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Sign Out",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sign Out",
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // App Version & Identity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.safetravel),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Safe Journey v1.4.0",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }
        }
    ) {

        GlassBackground(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                // --- TOP HEADER APP BAR ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding() // Directly below clock/battery
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        // 1. LEFT SIDE: Menu Bar (Drawer Toggle) or Back Button
                        val isSubscreen = showVersionLog || showWeather || showThana || showFireService ||
                                showHospitals || showBloodBanks || showEmergencyContacts || (showSafetyTips && selectedTip != null) ||
                                selectedNews != null

                        if (isSubscreen) {
                            IconButton(onClick = {
                                if (selectedTip != null) selectedTip = null
                                else if (selectedNews != null) selectedNews = null
                                else resetSubScreens()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = GlassColors.TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu Bar",
                                    tint = GlassColors.TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // 2. CENTER: Safe Journey Title & Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            val currentTitle = when {
                                showVersionLog -> "Version History"
                                showWeather -> "Weather Alerts"
                                showThana -> "Thana Explorer"
                                showFireService -> "Fire Stations"
                                showHospitals -> "Nearby Hospitals"
                                showBloodBanks -> "Blood Banks"
                                showEmergencyContacts -> "Emergency Contacts"
                                showSafetyTips && selectedTip != null -> "Tip Details"
                                showSafetyTips -> "Safety Tips"
                                selectedNews != null -> "News Report"
                                selectedTab == 1 -> "Live Tracking"
                                selectedTab == 2 -> "Profile"
                                selectedTab == 3 -> "Alerts"
                                selectedTab == 4 -> "Trip History"
                                else -> "Safe Journey"
                            }
                            
                            if (currentTitle == "Safe Journey") {
                                Icon(
                                    painter = painterResource(id = R.drawable.safetravel),
                                    contentDescription = "Safe Journey Logo",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            Text(
                                text = currentTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GlassColors.TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }

                        // 3. RIGHT SIDE: Notification Icon on the Left of Round Profile Avatar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Notification Icon
                            IconButton(
                                onClick = { showNotificationTray = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Notifications",
                                        tint = GlassColors.TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    // Notification alert dot
                                    if (newsList.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .align(Alignment.TopEnd)
                                                .offset(x = 1.dp, y = (-2).dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                        )
                                    }
                                }
                            }

                            // Round Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF914D))
                                    .clickable {
                                        resetSubScreens()
                                        selectedTab = 2
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!profile?.avatarUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = profile?.avatarUrl,
                                        contentDescription = "Profile Avatar",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = profile?.fullName?.firstOrNull()?.toString()?.uppercase() ?: "M",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

                // --- VIEWPORT / SCREENS CONTAINER ---
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTip != null) {
                        SafetyTipDetailScreen(
                            tip = selectedTip!!,
                            onBack = { selectedTip = null },
                            onMarkAsRead = { vm.markTipAsRead(it) }
                        )
                    } else if (showSafetyTips) {
                        SafetyTipsScreen(
                            vm = vm,
                            onBack = { showSafetyTips = false },
                            onTipClick = { selectedTip = it }
                        )
                    } else if (showVersionLog) {
                        VersionLogScreen(onBack = { showVersionLog = false })
                    } else if (showWeather) {
                        val currentPos by vm.currentPosition.collectAsState()
                        val cityName by vm.currentLocationName.collectAsState()
                        WeatherScreen(
                            lat = currentPos?.first ?: 23.7104,
                            lng = currentPos?.second ?: 90.4074,
                            cityName = cityName,
                            onBack = { showWeather = false }
                        )
                    } else if (showThana) {
                        ThanaScreen(vm = vm, onBack = { showThana = false })
                    } else if (showFireService) {
                        FireStationScreen(vm = vm, onBack = { showFireService = false })
                    } else if (showHospitals) {
                        HospitalScreen(vm = vm, onBack = { showHospitals = false })
                    } else if (showBloodBanks) {
                        BloodBankScreen(vm = vm, onBack = { showBloodBanks = false })
                    } else if (showEmergencyContacts) {
                        EmergencyContactScreen(vm = vm, onBack = { showEmergencyContacts = false })
                    } else if (watchTripId != null) {
                        GuardianLiveMapScreen(vm = vm, tripId = watchTripId!!, onBack = { watchTripId = null })
                    } else if (selectedNews != null) {
                        NewsDetailScreen(
                            news = selectedNews!!,
                            onBack = { selectedNews = null }
                        )
                    } else {
                        when (selectedTab) {
                            0 -> HomeScreen(
                                vm = vm, 
                                onNewsClick = { selectedNews = it },
                                onActionClick = { label ->
                                    if (label == "Emergency Contacts" || label == "Safety Tips") {
                                        openActionScreen(label)
                                    } else {
                                        // Location-sensitive quick actions: Thana, Fire Service, Hospitals, Blood Bank, Weather Alerts
                                        if (hasLocationPermission) {
                                            openActionScreen(label)
                                        } else {
                                            pendingActionScreen = label
                                            showActionLocationWizard = true
                                        }
                                    }
                                }
                            )
                            1 -> TravelScreen(vm)
                            2 -> ProfileScreen(vm, onAppInfoClick = { showVersionLog = true })
                            3 -> GuardianCenterScreen(vm, onWatchTrip = { watchTripId = it })
                            4 -> HistoryScreen(vm)
                            5 -> FeedbackScreen(
                                vm = vm,
                                onBack = { selectedTab = 0 }
                            )
                            6 -> SupportScreen(
                                onBack = { selectedTab = 0 }
                            )
                            7 -> PremiumScreen(
                                vm = vm,
                                onBack = { selectedTab = 0 }
                            )
                        }
                    }
                }
            }

            // --- FLOATING GLASS BOTTOM NAVIGATION BAR OVERLAY ---
            val isMainTabActive = !showVersionLog && !showWeather && !showThana && !showFireService &&
                    !showHospitals && !showBloodBanks && !showEmergencyContacts && !showSafetyTips &&
                    selectedNews == null && watchTripId == null
            val hideBottomBar = selectedNews != null || watchTripId != null || (showSafetyTips && selectedTip != null)

            if (!hideBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(26.dp),
                                ambientColor = Color.Black.copy(alpha = 0.55f),
                                spotColor = Color.Black.copy(alpha = 0.65f)
                            )
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xE60D111D))
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val haptic = LocalHapticFeedback.current
                            val activeColor = Color(0xFF06B6D4)
                            val inactiveColor = Color(0xFF94A3B8)

                            // 1. Dashboard (Home - Tab 0)
                            BarItem(
                                selected = selectedTab == 0 && isMainTabActive,
                                icon = Icons.Default.GridView,
                                label = "Dashboard",
                                selectedColor = activeColor,
                                unselectedColor = inactiveColor,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    resetSubScreens()
                                    selectedTab = 0
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // 2. Alerts (Tab 3)
                            BarItem(
                                selected = selectedTab == 3 && isMainTabActive,
                                icon = Icons.Default.Notifications,
                                label = "Alerts",
                                selectedColor = activeColor,
                                unselectedColor = inactiveColor,
                                hasBadge = newsList.isNotEmpty(),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    resetSubScreens()
                                    selectedTab = 3
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // 3. Tracking (Tab 1)
                            BarItem(
                                selected = selectedTab == 1 && isMainTabActive,
                                icon = Icons.Default.Explore,
                                label = "Tracking",
                                selectedColor = activeColor,
                                unselectedColor = inactiveColor,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    resetSubScreens()
                                    if (!hasLocationPermission) {
                                        pendingTrackingTab = true
                                        showLocationDisclosureDialog = true
                                    } else {
                                        selectedTab = 1
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // 4. History (Trip History - Tab 4)
                            BarItem(
                                selected = selectedTab == 4 && isMainTabActive,
                                icon = Icons.Default.History,
                                label = "History",
                                selectedColor = activeColor,
                                unselectedColor = inactiveColor,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    resetSubScreens()
                                    selectedTab = 4
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // 5. Profile (Tab 2)
                            BarItem(
                                selected = selectedTab == 2 && isMainTabActive,
                                icon = Icons.Default.Person,
                                label = "Profile",
                                selectedColor = activeColor,
                                unselectedColor = inactiveColor,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    resetSubScreens()
                                    selectedTab = 2
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            val headsUpAlert by vm.activeHeadsUpAlert.collectAsState()
            AnimatedVisibility(
                visible = headsUpAlert != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .zIndex(99f)
            ) {
                headsUpAlert?.let { alert ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                watchTripId = alert.tripId
                                vm.activeHeadsUpAlert.value = null
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFA0F172A)),
                        border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFEC4899)))),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x20EF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Alert",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = alert.message,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "Track Now",
                                        color = Color(0xFF34D399),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            watchTripId = alert.tripId
                                            vm.activeHeadsUpAlert.value = null
                                        }
                                    )
                                    Text(
                                        text = "Dismiss",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            vm.activeHeadsUpAlert.value = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // --- SMART NOTIFICATION TRAY ---
    if (showNotificationTray) {
        com.safetravel.tracker.ui.components.NotificationTrayBottomSheet(
            vm = vm,
            onDismiss = { showNotificationTray = false },
            onNavigateToTab = { tabIndex ->
                resetSubScreens()
                selectedTab = tabIndex
            },
            onNewsClick = { news ->
                selectedNews = news
            },
            onOpenSosSettings = {
                showNotificationDialog = true
            }
        )
    }

    // --- DIALOG POPUPS FOR SIDEBAR ITEMS ---

    if (showNotificationDialog) {
        var smsAlerts by remember { mutableStateOf(true) }
        var pushAlerts by remember { mutableStateOf(true) }
        var sosHoldSec by remember { mutableIntStateOf(com.safetravel.tracker.util.SosSettingsManager.getHoldDurationSec(context)) }
        var sosCheckInterval by remember { mutableIntStateOf(com.safetravel.tracker.util.SosSettingsManager.getSafetyCheckIntervalMin(context)) }
        var proDispatchMode by remember { mutableStateOf(com.safetravel.tracker.util.SosSettingsManager.getProDispatchMode(context)) }

        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp),
            title = {
                Text("Safety & SOS Settings", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. SOS Hold Duration
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(10.dp)
                    ) {
                        Text("SOS Hold Duration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Hold button duration before activation", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0 to "0s (Instant)", 1 to "1s", 2 to "2s (Default)", 3 to "3s").forEach { (sec, label) ->
                                val selected = sosHoldSec == sec
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0x33EF4444) else Color(0x1F1E293B))
                                        .border(1.dp, if (selected) Color(0xFFEF4444) else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable { sosHoldSec = sec }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(label, color = if (selected) Color(0xFFFCA5A5) else Color(0xFFCBD5E1), fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // 2. Safety Check Interval
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(10.dp)
                    ) {
                        Text("Active SOS Safety Check Interval", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("How often safety check wizard prompts", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1 to "1 min", 2 to "2 min (Default)", 3 to "3 min", 5 to "5 min").forEach { (min, label) ->
                                val selected = sosCheckInterval == min
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0x3306B6D4) else Color(0x1F1E293B))
                                        .border(1.dp, if (selected) Color(0xFF06B6D4) else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable { sosCheckInterval = min }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(label, color = if (selected) Color(0xFF67E8F9) else Color(0xFFCBD5E1), fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }

                    // 3. Pro Emergency Dispatch (Coming Soon)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Guardian Alert Dispatch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x338B5CF6))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text("[PRO - COMING SOON]", color = Color(0xFFC084FC), fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text("Free tier sends instant WhatsApp alert to all guardians. Pro users can choose automated voice call or direct SMS.", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.safetravel.tracker.util.SosSettingsManager.setHoldDurationSec(context, sosHoldSec)
                        com.safetravel.tracker.util.SosSettingsManager.setSafetyCheckIntervalMin(context, sosCheckInterval)
                        com.safetravel.tracker.util.SosSettingsManager.setProDispatchMode(context, proDispatchMode)
                        val settingsMap = com.safetravel.tracker.util.SosSettingsManager.toMap(context)
                        profile?.let { p ->
                            vm.updateProfileFields(p.id, mapOf("sos_settings" to settingsMap))
                        }
                        showNotificationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SAVE SETTINGS", color = Color(0xFF020617), fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("CLOSE", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "TERMINATE SESSION?",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out from Safe Journey? Active trip tracking and guardian notifications will be paused until you sign in again.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        vm.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SIGN OUT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Global In-App Safety Check-in Wizard for Active SOS (Appears across any active screen)
    val showSafetyCheck by vm.showSosSafetyCheckWizard
    if (showSafetyCheck) {
        com.safetravel.tracker.ui.components.travel.SosSafetyCheckWizard(
            timeoutSeconds = 15,
            onReportSafe = { vm.reportSosSafetyCheck("safe") },
            onReportDanger = { vm.reportSosSafetyCheck("danger") },
            onTimeoutNoResponse = { vm.reportSosSafetyCheck("timeout_unanswered") }
        )
    }

    // Google Play Store Compliant Prominent Permission Disclosure Dialogs
    PermissionDisclosureDialog(
        showDialog = showLocationDisclosureDialog,
        permissionType = SafetyPermissionType.LOCATION,
        onConfirm = {
            showLocationDisclosureDialog = false
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onDismiss = {
            showLocationDisclosureDialog = false
            pendingTrackingTab = false
        }
    )

    PermissionDisclosureDialog(
        showDialog = showNotificationDisclosureDialog,
        permissionType = SafetyPermissionType.NOTIFICATION,
        onConfirm = {
            showNotificationDisclosureDialog = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onDismiss = {
            showNotificationDisclosureDialog = false
        }
    )

    SafetyPermissionsWizardDialog(
        showDialog = showSafetyWizardDialog,
        hasLocationPermission = hasLocationPermission,
        hasNotificationPermission = hasNotificationPermission,
        onDismiss = {
            showSafetyWizardDialog = false
            hasSeenWizard = true
            wizardPrefs.edit().putBoolean("has_seen_wizard", true).apply()
        }
    )

    // Location Guard for Location-based Quick Actions (Thana, Fire, Hospitals, Weather, etc.)
    ActionLocationRequiredWizardDialog(
        showDialog = showActionLocationWizard,
        actionTitle = pendingActionScreen ?: "সেবা",
        onGrantLocation = {
            showActionLocationWizard = false
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        },
        onContinueWithoutLocation = {
            showActionLocationWizard = false
            pendingActionScreen?.let { openActionScreen(it) }
            pendingActionScreen = null
        },
        onDismiss = {
            showActionLocationWizard = false
            pendingActionScreen = null
        }
    )
}

