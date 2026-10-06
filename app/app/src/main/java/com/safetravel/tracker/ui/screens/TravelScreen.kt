// File: /app/src/main/java/com/safetravel/tracker/ui/screens/TravelScreen.kt
package com.safetravel.tracker.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.net.Uri
import com.safetravel.tracker.util.AddressUtils
import com.safetravel.tracker.util.DateTimeUtils
import com.safetravel.tracker.util.AudioRecordHelper
import com.safetravel.tracker.ui.components.travel.*
import com.safetravel.tracker.ui.components.GlassBottomSheetContainer
import com.safetravel.tracker.ui.components.PermissionDisclosureDialog
import com.safetravel.tracker.ui.components.SafetyPermissionType
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel
import com.safetravel.tracker.viewmodel.GeocodeSuggestion
import com.safetravel.tracker.supabase.SupabaseTrip
import com.mapbox.geojson.Point
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.min

@Preview(showBackground = true)
@Composable
fun TravelScreenSetupPreview() {
    SafeTravelTheme {
        TravelScreenContent(
            activeTrip = null,
            position = null,
            currentLocationName = "My Location",
            originInput = "My Location",
            destinationInput = "",
            transportMode = 2,
            vehiclePlate = "",
            vehicleDescription = "",
            onOriginChange = {},
            onDestinationChange = {},
            onTransportModeChange = {},
            onVehiclePlateChange = {},
            onVehicleDescriptionChange = {},
            emergencyPhone = "+1 555-0144",
            mapboxToken = "pk.mock_token",
            searchSuggestions = emptyList(),
            isSearching = false,
            onTriggerSos = {},
            onEndTrip = { _, _ -> },
            onSearchPlaces = {},
            onGeocodeAddress = { _, _ -> },
            onStartTrip = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onEmergencyPhoneChange = {},
            onClearSuggestions = {},
            onUpdatePosition = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TravelScreenActivePreview() {
    SafeTravelTheme {
        TravelScreenContent(
            activeTrip = SupabaseTrip(
                id = "trip-123",
                userId = "usr-1",
                startAddress = "Start",
                endAddress = "Destination",
                status = "ongoing"
            ),
            position = Pair(23.765, 90.395),
            currentLocationName = "Current Area",
            originInput = "Start Point",
            destinationInput = "End Point",
            transportMode = 2,
            vehiclePlate = "42-1200",
            vehicleDescription = "White Toyota",
            onOriginChange = {},
            onDestinationChange = {},
            onTransportModeChange = {},
            onVehiclePlateChange = {},
            onVehicleDescriptionChange = {},
            emergencyPhone = "+1 555-0144",
            mapboxToken = "pk.mock_token",
            searchSuggestions = emptyList(),
            isSearching = false,
            onTriggerSos = {},
            onEndTrip = { _, _ -> },
            onSearchPlaces = {},
            onGeocodeAddress = { _, _ -> },
            onStartTrip = { _, _, _, _, _, _, _, _, _, _, _ -> },
            onEmergencyPhoneChange = {},
            onClearSuggestions = {},
            onUpdatePosition = { _, _ -> }
        )
    }
}

@Composable
fun TravelScreen(vm: SafeTravelViewModel) {
    LaunchedEffect(Unit) {
        vm.refreshGuardians()
        vm.requestImmediateLocation()
        vm.startContinuousLocationUpdates()
    }
    val activeTrip by vm.activeTrip.collectAsState()
    val position by vm.currentPosition.collectAsState()
    val currentLocationName by vm.currentLocationName.collectAsState()
    val emergencyPhone by vm.emergencyMobileNumberToCall.collectAsState()
    val tokenState by vm.mapboxToken.collectAsState()
    val searchSuggestions by vm.searchSuggestions.collectAsState()
    val isSearching by vm.isSearching.collectAsState()
    val guardians by vm.userGuardians.collectAsState()
    val currentSpeedKmh by vm.currentSpeedKmh.collectAsState()
    val activeRoutePath by vm.activeRoutePath.collectAsState()
    
    // Persisted UI States
    val originInput by vm.travelOriginInput.collectAsState()
    val destinationInput by vm.travelDestinationInput.collectAsState()
    val transportMode by vm.travelTransportMode.collectAsState()
    val vehiclePlate by vm.travelVehiclePlate.collectAsState()
    val vehicleDescription by vm.travelVehicleDescription.collectAsState()

    // Observe Action Feedback Messages (Errors, successes, status)
    val actionFeedbackMessage by vm.actionFeedbackMessage.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(actionFeedbackMessage) {
        actionFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            vm.actionFeedbackMessage.value = null
        }
    }

    TravelScreenContent(
        activeTrip = activeTrip,
        position = position,
        currentLocationName = currentLocationName,
        originInput = originInput,
        destinationInput = destinationInput,
        transportMode = transportMode,
        vehiclePlate = vehiclePlate,
        vehicleDescription = vehicleDescription,
        onOriginChange = { vm.travelOriginInput.value = it },
        onDestinationChange = { vm.travelDestinationInput.value = it },
        onTransportModeChange = { vm.travelTransportMode.value = it },
        onVehiclePlateChange = { vm.travelVehiclePlate.value = it },
        onVehicleDescriptionChange = { vm.travelVehicleDescription.value = it },
        emergencyPhone = emergencyPhone,
        mapboxToken = tokenState,
        searchSuggestions = searchSuggestions,
        isSearching = isSearching,
        guardians = guardians,
        onTriggerSos = { vm.triggerSos() },
        onEndTrip = { battery, dist -> vm.endTrip(battery, dist) },
        onSearchPlaces = { vm.searchPlaces(it) },
        onGeocodeAddress = { query, onResult -> vm.geocodeAddress(query, onResult) },
        onStartTrip = { origin, dest, battery, vehicle, desc, mode, shares, plannedRoute, estDist, estDuration, vehiclePhoto -> 
            vm.startTrip(origin, dest, battery, vehicle, desc, mode, shares, plannedRoute, estDist, estDuration, vehiclePhoto)
        },
        onShareTrip = { tripId, phones -> vm.shareTripWithGuardians(tripId, phones) },
        onEmergencyPhoneChange = { vm.emergencyMobileNumberToCall.value = it },
        onClearSuggestions = { vm.searchSuggestions.value = emptyList() },
        onUpdatePosition = { lat, lng -> vm.onManualLocationUpdate(lat, lng) },
        hasCenteredInitially = vm.mapHasCenteredInitially,
        onCenteredInitially = { vm.mapHasCenteredInitially = true },
        currentSpeedKmh = currentSpeedKmh,
        onSilentAlert = { vm.triggerSos() },
        onResolveSos = { vm.resolveSos() },
        onSelectMapLocation = { lat, lng -> vm.resolveAndSetDestination(context, lat, lng) },
        activeRoutePath = activeRoutePath,
        onUploadAudio = { file, tripId, dur, trigger ->
            vm.uploadAndSaveAudio(file, tripId, dur, trigger)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelScreenContent(
    activeTrip: com.safetravel.tracker.supabase.SupabaseTrip?,
    position: Pair<Double, Double>?,
    currentLocationName: String,
    originInput: String,
    destinationInput: String,
    transportMode: Int,
    vehiclePlate: String,
    vehicleDescription: String,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onTransportModeChange: (Int) -> Unit,
    onVehiclePlateChange: (String) -> Unit,
    onVehicleDescriptionChange: (String) -> Unit,
    emergencyPhone: String,
    mapboxToken: String,
    searchSuggestions: List<com.safetravel.tracker.viewmodel.GeocodeSuggestion>,
    isSearching: Boolean,
    guardians: List<com.safetravel.tracker.supabase.Guardian> = emptyList(),
    onTriggerSos: () -> Unit,
    onEndTrip: (Int, Double?) -> Unit,
    onSearchPlaces: (String) -> Unit,
    onGeocodeAddress: (String, (Double, Double, String) -> Unit) -> Unit,
    onStartTrip: (String, String, Int, String, String, Int, List<String>, List<Map<String, Double>>, Double?, Double?, String?) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onShareTrip: (String, List<String>) -> Unit = { _, _ -> },
    onEmergencyPhoneChange: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    onUpdatePosition: (Double, Double) -> Unit,
    hasCenteredInitially: Boolean = false,
    onCenteredInitially: () -> Unit = {},
    currentSpeedKmh: Int = 0,
    onSilentAlert: () -> Unit = {},
    onResolveSos: () -> Unit = {},
    onSelectMapLocation: (Double, Double) -> Unit = { _, _ -> },
    activeRoutePath: List<Map<String, Double>> = emptyList(),
    onUploadAudio: (java.io.File, String?, Int, String) -> Unit = { _, _, _, _ -> }
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // Dynamic sizing for responsiveness
    val sosNodeSize = (screenWidth * 0.5f).coerceAtMost(220.dp).coerceAtLeast(160.dp)

    var activeEngineName by remember { mutableStateOf("Map Engine Loading...") }
    var webViewLogs by remember { mutableStateOf(emptyList<String>()) }

    val context = LocalContext.current

    // Dynamic label for Emergency Phone
    var tempEmergencyPhone by remember { mutableStateOf(emergencyPhone) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { contactUri ->
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactUri.lastPathSegment),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex != -1) {
                        tempEmergencyPhone = cursor.getString(numberIndex)
                    }
                }
            }
        }
    }

    // Global Location tracking is handled in SafeTravelViewModel and MainActivity (Strictly NO mock coordinates!)
    val liveUserCoordinates = position ?: (
        if (activeTrip?.currentLat != null && activeTrip.currentLng != null && activeTrip.currentLat != 0.0) {
            Pair(activeTrip.currentLat, activeTrip.currentLng)
        } else null
    )

    // Destination and start point coordinates extracted from database or input
    val tripDestinationCoords = remember(activeTrip?.endCoords, destinationInput) {
        if (activeTrip != null) {
            activeTrip.endCoords?.let { AddressUtils.extractCoordinates(it) }
                ?: AddressUtils.extractCoordinates(destinationInput)
                ?: getCoordsForAddress(destinationInput)
        } else if (destinationInput.isNotBlank()) {
            AddressUtils.extractCoordinates(destinationInput)
                ?: getCoordsForAddress(destinationInput)
        } else null
    }

    val tripStartCoords = remember(activeTrip?.startCoords, originInput, liveUserCoordinates) {
        activeTrip?.startCoords?.let { AddressUtils.extractCoordinates(it) }
            ?: AddressUtils.extractCoordinates(originInput)
            ?: liveUserCoordinates
    }

    // Traveled breadcrumbs log from GPS updates / Supabase routePathLog with seamless historical stitching
    val liveRoutePoints: List<Point> = remember(activeRoutePath, activeTrip?.routePathLog) {
        val tripLog = activeTrip?.routePathLog ?: emptyList()
        val rawList = if (activeRoutePath.size >= tripLog.size && activeRoutePath.isNotEmpty()) {
            activeRoutePath
        } else if (tripLog.isNotEmpty()) {
            tripLog
        } else {
            activeRoutePath
        }
        rawList.mapNotNull { item ->
            val lat = item["lat"]
            val lng = item["lng"]
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                Point.fromLngLat(lng, lat)
            } else null
        }
    }

    // Selected route alternative index (0: Primary/Fastest, 1..2: Alternatives)
    var selectedRouteIndex by remember { mutableIntStateOf(0) }
    var availableRouteAlternatives by remember { mutableStateOf<List<RouteInfo>>(emptyList()) }
    var isRouteCardDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(destinationInput) {
        if (destinationInput.isNotBlank()) {
            isRouteCardDismissed = false
        }
    }

    // Locked Planned Route: When a voyage starts, the planned route is saved in database (plannedRouteLog).
    // During active voyage, we draw this exact fixed planned route (Rose Red) and NEVER reroute dynamically.
    val fixedPlannedRoutePoints: List<Point>? = remember(activeTrip?.plannedRouteLog) {
        activeTrip?.plannedRouteLog?.mapNotNull { item ->
            val lat = item["lat"]
            val lng = item["lng"]
            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                Point.fromLngLat(lng, lat)
            } else null
        }?.takeIf { it.isNotEmpty() }
    }

    // Auto-sync originInput with currentLocationName when GPS resolves
    LaunchedEffect(currentLocationName) {
        if (originInput == "Locating..." || originInput == "Unknown Location" || originInput == "My Location") {
            onOriginChange(currentLocationName)
        }
    }
    
    // Mission Control States
    var vehiclePhotoUploaded by remember { mutableStateOf(false) }
    var photoLoadingState by remember { mutableStateOf(false) }
    var currentVehiclePhotoUrl by remember { mutableStateOf<String?>(null) }
    var currentBatteryLevel by remember { mutableIntStateOf(100) }

    val vehiclePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoLoadingState = true
            scope.launch {
                val uid = activeTrip?.userId ?: com.safetravel.tracker.supabase.SupabaseManager.currentUserId ?: "anonymous"
                val result = com.safetravel.tracker.util.CloudinaryImageUploader.uploadTripVehiclePhoto(context, uri, uid)
                photoLoadingState = false
                if (result != null && result.secureUrl.isNotBlank()) {
                    vehiclePhotoUploaded = true
                    currentVehiclePhotoUrl = result.secureUrl
                    Toast.makeText(context, "Vehicle photo attached successfully.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to upload photo. Please check internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (kotlinx.coroutines.isActive) {
            currentBatteryLevel = com.safetravel.tracker.util.BatteryHelper.getCurrentBatteryLevel(context)
            kotlinx.coroutines.delay(10_000L)
        }
    }

    LaunchedEffect(activeTrip?.endBatteryLevel) {
        activeTrip?.endBatteryLevel?.let {
            currentBatteryLevel = it
        }
    }

    var showJourneySheet by remember { mutableStateOf(false) }
    var showSafetyToolkit by remember { mutableStateOf(false) }
    var showFakeCall by remember { mutableStateOf(false) }
    var showAddGuardianDialog by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(AudioRecordHelper.isRecording) }
    var showAudioDisclosureDialog by remember { mutableStateOf(false) }

    val audioRecordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tripId = activeTrip?.id
            val started = AudioRecordHelper.startRecording(context, tripId)
            isRecordingAudio = started
            if (started) {
                Toast.makeText(context, "Silent Audio Blackbox active.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission needed for Audio Blackbox", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Guardian Selection State
    val selectedGuardiansToShare = remember { mutableStateListOf<String>() }

    // Auto-select guardians with defaultNotify = true whenever guardians list updates
    LaunchedEffect(guardians) {
        if (selectedGuardiansToShare.isEmpty() && guardians.isNotEmpty()) {
            guardians.filter { it.defaultNotify == true }.forEach { g ->
                if (!selectedGuardiansToShare.contains(g.phone)) {
                    selectedGuardiansToShare.add(g.phone)
                }
            }
        }
    }

    // New setup info states
    var calculatedTime by remember { mutableStateOf("Calculating...") }
    var calculatedDistance by remember { mutableStateOf("--- km") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Sync destinationInput and restore persistent trip metrics from database immediately
    LaunchedEffect(activeTrip) {
        activeTrip?.let { trip ->
            onDestinationChange(trip.endLocation)
            val dist = trip.estimatedDistanceKm ?: trip.totalDistance
            if (dist != null && dist > 0.0) {
                calculatedDistance = com.safetravel.tracker.util.TripTelemetryCalculator.formatDistance(dist)
            }
            val duration = trip.estimatedDurationMin?.toInt()
            if (duration != null && duration > 0) {
                calculatedTime = com.safetravel.tracker.util.TripTelemetryCalculator.formatDuration(duration)
            }
        }
    }

    // Live Telemetry Recalculation: As user moves during active trip, continuously compute
    // live remaining distance and live remaining duration using TripTelemetryCalculator!
    LaunchedEffect(liveUserCoordinates, activeTrip?.id) {
        val trip = activeTrip ?: return@LaunchedEffect
        val userPos = liveUserCoordinates ?: return@LaunchedEffect
        val destCoords = tripDestinationCoords ?: return@LaunchedEffect

        val remKm = com.safetravel.tracker.util.TripTelemetryCalculator.calculateRemainingDistanceKm(
            currentLat = userPos.first,
            currentLng = userPos.second,
            endLat = destCoords.first,
            endLng = destCoords.second,
            plannedRoute = trip.plannedRouteLog
        )
        if (remKm > 0.0) {
            calculatedDistance = com.safetravel.tracker.util.TripTelemetryCalculator.formatDistance(remKm)
            val remMin = com.safetravel.tracker.util.TripTelemetryCalculator.calculateRemainingDurationMin(
                remainingDistanceKm = remKm,
                currentSpeedKmh = currentSpeedKmh,
                transportMode = trip.transportMode
            )
            calculatedTime = com.safetravel.tracker.util.TripTelemetryCalculator.formatDuration(remMin)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900),
        contentAlignment = Alignment.TopCenter
    ) {
        // ------------------------------------
        // -- UNIFIED FULL SCREEN MAP VIEW --
        // ------------------------------------
        Box(modifier = Modifier.fillMaxSize()) {
            MapboxView(
                destination = if (activeTrip != null) activeTrip.endLocation else destinationInput,
                destinationCoords = tripDestinationCoords,
                startCoords = tripStartCoords,
                liveRoutePath = liveRoutePoints,
                fixedPlannedRoute = fixedPlannedRoutePoints,
                selectedRouteIndex = selectedRouteIndex,
                onRouteSelected = { idx, _ -> selectedRouteIndex = idx },
                onRouteChange = { routes ->
                    availableRouteAlternatives = routes
                    if (selectedRouteIndex >= routes.size) {
                        selectedRouteIndex = 0
                    }
                },
                onLocationSelected = { lat, lng, addr ->
                    if (activeTrip == null) {
                        selectedRouteIndex = 0
                        isRouteCardDismissed = false
                        onDestinationChange(addr)
                        onSelectMapLocation(lat, lng)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                mapboxToken = mapboxToken,
                liveUserCoordinates = liveUserCoordinates,
                navigationRouteActive = (activeTrip != null || destinationInput.isNotBlank() || tripDestinationCoords != null),
                transportMode = transportMode,
                onTripDetailsCalculated = { time, dist ->
                    calculatedTime = time
                    calculatedDistance = dist
                },
                modifier = Modifier.fillMaxSize(),
                onEngineLoaded = { activeEngineName = it },
                onLogReceived = { log -> webViewLogs = (webViewLogs + log).takeLast(60) },
                hasCenteredInitially = hasCenteredInitially,
                onCenteredInitially = onCenteredInitially,
                bottomPadding = 72.dp,
                onToggleSafetyToolkit = { showSafetyToolkit = !showSafetyToolkit }
            )

            // Real-Time Speedometer HUD (Square on Left Side of Map)
            if (activeTrip != null) {
                SpeedometerHud(
                    speedKmh = currentSpeedKmh,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                )
            }

            // TOP HUD: Mission Control & Who Is Watching (Visible only during active trip)
            if (activeTrip != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                ) {
                    MissionControlBar(
                        vehiclePlate = vehiclePlate.ifBlank { null },
                        batteryLevel = currentBatteryLevel,
                        safetyScore = 100 // Dynamic score logic can be added later
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LIVE TRACKING ACTIVE Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Red500.copy(alpha = 0.9f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(7.dp).background(Color.White, CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LIVE ACTIVE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Who Is Watching Guardians Bar
                        WhoIsWatchingBar(
                            guardians = guardians,
                            notifiedPhones = activeTrip.notifiedGuardians ?: selectedGuardiansToShare,
                            onAddGuardianClick = { showAddGuardianDialog = true }
                        )
                    }

                    // High-Visibility Blinking CC SOS Active Badge
                    if (activeTrip.status == "sos") {
                        Spacer(modifier = Modifier.height(8.dp))
                        SosActiveBadge(
                            onCall999Click = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:999"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            },
                            onSosClick = {
                                showJourneySheet = true
                            }
                        )
                    }
                }
            }

            // ------------------------------------------------------------------
            // -- PRE-TRIP ROUTE PREVIEW CARD (Destination & Transport Selector) --
            // ------------------------------------------------------------------
            if (activeTrip == null && destinationInput.isNotBlank() && !isRouteCardDismissed) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 88.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF20B111E)),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(Color(0x5506B6D4), Color(0x2238BDF8), Color(0x22FFFFFF)))
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Destination Header + Close (✕) Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x2206B6D4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DESTINATION",
                                        color = Slate400,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = AddressUtils.formatDisplayAddress(destinationInput).ifBlank { "Selected Point" },
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Dismiss / Clear Destination Button
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDestinationChange("")
                                    isRouteCardDismissed = true
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1FFFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear / Dismiss",
                                    tint = Slate300,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // 2. 4-Mode Horizontal Selector [ Walk | Cycle | Car | Bus ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val modes = listOf(
                                Triple(0, "Walk", Icons.Default.DirectionsWalk),
                                Triple(1, "Cycle", Icons.Default.DirectionsBike),
                                Triple(2, "Car", Icons.Default.DirectionsCar),
                                Triple(3, "Bus", Icons.Default.DirectionsBus)
                            )
                            modes.forEach { (modeId, modeTitle, modeIcon) ->
                                val isSelected = (transportMode == modeId)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color(0x3306B6D4) else Color(0x1F1E293B)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF06B6D4) else Color(0x1FFFFFFF),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            if (transportMode != modeId) {
                                                onTransportModeChange(modeId)
                                                selectedRouteIndex = 0
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = modeIcon,
                                            contentDescription = modeTitle,
                                            tint = if (isSelected) Color(0xFF38BDF8) else Slate400,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = modeTitle,
                                            color = if (isSelected) Color.White else Slate400,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Time, Distance & Mode Summary
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x141E293B))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = calculatedTime,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("•", color = Slate500, fontSize = 11.sp)
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = calculatedDistance,
                                    color = Color(0xFF10B981),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = when (transportMode) {
                                    0 -> "Pedestrian / Alley"
                                    1 -> "Cycle Path"
                                    2 -> "Driving Road"
                                    3 -> "Bus Corridor"
                                    else -> "Route"
                                },
                                color = Slate400,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 4. Route Alternatives Pills (If multiple routes exist for this mode)
                        if (availableRouteAlternatives.size > 1) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(availableRouteAlternatives) { idx, route ->
                                    val isSelected = (selectedRouteIndex == idx)
                                    val summaryText = route.summary.ifBlank { if (idx == 0) "Shortest" else "Alt ${idx + 1}" }
                                    val timeStr = if (route.durationMin < 60) "${route.durationMin.toInt()}m" else "${(route.durationMin / 60).toInt()}h ${(route.durationMin % 60).toInt()}m"
                                    val distStr = String.format(java.util.Locale.US, "%.1f km", route.distanceKm)

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0x33E11D48) else Color(0x1F1E293B))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFFE11D48) else Color(0x22FFFFFF),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedRouteIndex = idx
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(if (isSelected) Color(0xFFE11D48) else Color(0xFF64748B), CircleShape)
                                            )
                                            Text(
                                                text = "$summaryText ($timeStr • $distStr)",
                                                color = if (isSelected) Color.White else Slate300,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Start Secured Journey Button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showJourneySheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "START SECURED JOURNEY",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------------------------
            // -- FLOATING ACTION BUTTON (When Card is Dismissed or Active Trip) --
            // ------------------------------------------------------------------
            if (activeTrip != null || destinationInput.isBlank() || isRouteCardDismissed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, bottom = 88.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xE60F172A))
                        .border(1.dp, Slate700, RoundedCornerShape(16.dp))
                        .clickable {
                            if (activeTrip != null) {
                                showJourneySheet = true
                            } else {
                                if (destinationInput.isNotBlank()) {
                                    isRouteCardDismissed = false
                                } else {
                                    Toast.makeText(context, "Please tap on the map to set a destination first!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (activeTrip == null) Icons.Default.Navigation else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (activeTrip == null) Emerald400 else Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (activeTrip == null) {
                                if (destinationInput.isNotBlank() && isRouteCardDismissed) "View Route Card" else "Start Journey"
                            } else "Journey Details",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ----------------------------------------------------
        // -- ADAPTIVE DUAL-MODE JOURNEY BOTTOM SHEET (GLASSMORPHIC) --
        // ----------------------------------------------------
        if (showJourneySheet) {
            ModalBottomSheet(
                onDismissRequest = { showJourneySheet = false },
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
                    if (activeTrip == null) {
                        // MODE A: SETUP JOURNEY WITH ENHANCED TRANSPORT MODE SELECTION & AIRLOCK
                        SelectTransportModeSheet(
                            transportMode = transportMode,
                            onTransportModeChange = { onTransportModeChange(it) },
                            vehiclePlate = vehiclePlate,
                            onVehiclePlateChange = { onVehiclePlateChange(it) },
                            vehicleDescription = vehicleDescription,
                            onVehicleDescriptionChange = { onVehicleDescriptionChange(it) },
                            vehiclePhotoUploaded = vehiclePhotoUploaded,
                            photoLoadingState = photoLoadingState,
                            onUploadPhotoClick = {
                                if (!photoLoadingState) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vehiclePhotoLauncher.launch("image/*")
                                }
                            },
                            destinationInput = destinationInput,
                            calculatedTime = calculatedTime,
                            calculatedDistance = calculatedDistance,
                            guardians = guardians,
                            selectedGuardiansToShare = selectedGuardiansToShare,
                            availableRoutes = availableRouteAlternatives,
                            selectedRouteIndex = selectedRouteIndex,
                            onSelectRoute = { selectedRouteIndex = it },
                            onStartVoyageClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    if (vehiclePlate.isBlank() && (transportMode == 2 || transportMode == 3)) {
                                        val errorMsg = if (transportMode == 3) "Please enter bus name!" else "Please enter vehicle plate number!"
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    onEmergencyPhoneChange(tempEmergencyPhone)
                                    sheetState.hide()
                                    showJourneySheet = false

                                    val chosenRoute = availableRouteAlternatives.getOrNull(selectedRouteIndex) ?: availableRouteAlternatives.firstOrNull()
                                    val plannedRouteList: List<Map<String, Double>> = chosenRoute?.points?.map { pt ->
                                        mapOf("lat" to pt.latitude(), "lng" to pt.longitude())
                                    } ?: emptyList()

                                    val estDistance = chosenRoute?.distanceKm ?: calculatedDistance.replace("km", "").trim().toDoubleOrNull()
                                    val estDuration = chosenRoute?.durationMin ?: Regex("(\\d+)").find(calculatedTime)?.groupValues?.get(1)?.toDoubleOrNull()

                                    val liveStartBat = com.safetravel.tracker.util.BatteryHelper.getCurrentBatteryLevel(context)
                                    currentBatteryLevel = liveStartBat

                                    onStartTrip(
                                        originInput,
                                        destinationInput,
                                        liveStartBat,
                                        vehiclePlate,
                                        vehicleDescription,
                                        transportMode,
                                        selectedGuardiansToShare.toList(),
                                        plannedRouteList,
                                        estDistance,
                                        estDuration,
                                        currentVehiclePhotoUrl
                                    )
                                    vehiclePhotoUploaded = false
                                    currentVehiclePhotoUrl = null
                                    
                                    // WhatsApp Sharing Logic
                                    if (selectedGuardiansToShare.isNotEmpty()) {
                                        delay(1500)
                                        val cleanDestName = AddressUtils.formatDisplayAddress(destinationInput)
                                        val msg = "I'm starting a journey to $cleanDestName. Track me on Safe Travel app."
                                        selectedGuardiansToShare.forEach { phone ->
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW)
                                                val url = "https://api.whatsapp.com/send?phone=$phone&text=${java.net.URLEncoder.encode(msg, "UTF-8")}"
                                                intent.data = android.net.Uri.parse(url)
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("WhatsApp", "Failed to send msg", e)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        // MODE B: ACTIVE JOURNEY DETAILS & EMERGENCY CONTROLS
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TRIP IN PROGRESS",
                                color = Red500,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val activeDisplayDest = AddressUtils.formatDisplayAddress(
                                if (activeTrip != null) activeTrip.endLocation else destinationInput
                            )
                            Text(
                                text = activeDisplayDest,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Central SOS Trigger / Active SOS State
                            if (activeTrip?.status == "sos") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp)),
                                    color = Color(0x33EF4444),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Active Alert",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "SOS EMERGENCY ACTIVE",
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "All guardians alerted with live telemetry. Audio blackbox is securing evidence.",
                                            color = Slate300,
                                            fontSize = 11.5.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Call 999
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:999"))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {}
                                                },
                                                modifier = Modifier.weight(1f).height(44.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("Call 999", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            // I Am Safe / Cancel SOS
                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onResolveSos()
                                                },
                                                modifier = Modifier.weight(1f).height(44.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("I Am Safe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                SosHoldTriggerButton(
                                    nodeSize = sosNodeSize,
                                    holdDurationSec = com.safetravel.tracker.util.SosSettingsManager.getHoldDurationSec(context),
                                    graceCountdownSec = com.safetravel.tracker.util.SosSettingsManager.getGraceCountdownSec(context),
                                    onSosConfirmed = {
                                        onTriggerSos()
                                        val hasMicPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.RECORD_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (hasMicPerm && !AudioRecordHelper.isRecording) {
                                            val started = AudioRecordHelper.startRecording(context, activeTrip?.id)
                                            if (started) isRecordingAudio = true
                                        }
                                        showJourneySheet = false
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("STARTED", color = Slate400, fontSize = 10.sp)
                                    val startTimeStr = DateTimeUtils.formatBstTime(activeTrip?.createdAt).ifBlank { "Just now" }
                                    Text(startTimeStr, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("REMAINING", color = Slate400, fontSize = 10.sp)
                                    Text(calculatedTime, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("DISTANCE", color = Slate400, fontSize = 10.sp)
                                    Text(calculatedDistance, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // UX IMPROVEMENT: Press and Hold to End Journey (3 Seconds)
                            var isPressed by remember { mutableStateOf(false) }
                            val holdProgress = remember { Animatable(0f) }
                            
                            LaunchedEffect(isPressed) {
                                if (isPressed) {
                                    val result = holdProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                                    )
                                    if (result.endReason == AnimationEndReason.Finished) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        
                                        // Calculate 100% accurate actual traveled distance from complete stitched breadcrumb log
                                        val fullTraveledList = if (activeRoutePath.size >= (activeTrip?.routePathLog?.size ?: 0) && activeRoutePath.isNotEmpty()) {
                                            activeRoutePath
                                        } else {
                                            activeTrip?.routePathLog ?: activeRoutePath
                                        }
                                        val actualTraveledKm = com.safetravel.tracker.util.TripTelemetryCalculator.calculateActualTraveledDistanceKm(fullTraveledList)
                                        val distanceValue = if (actualTraveledKm > 0.05) {
                                            actualTraveledKm
                                        } else {
                                            calculatedDistance.replace("km", "").trim().toDoubleOrNull()
                                        }
                                            
                                        if (AudioRecordHelper.isRecording) {
                                            val dur = AudioRecordHelper.getRecordingDurationSec()
                                            val file = AudioRecordHelper.stopRecording()
                                            isRecordingAudio = false
                                            if (file != null && file.exists()) {
                                                onUploadAudio(file, activeTrip?.id, dur, "trip_end")
                                            }
                                        }
                                        val liveEndBat = com.safetravel.tracker.util.BatteryHelper.getCurrentBatteryLevel(context)
                                        onEndTrip(liveEndBat, distanceValue)
                                        showJourneySheet = false
                                    }
                                } else {
                                    holdProgress.animateTo(0f, tween(300))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Slate800)
                                    .border(BorderStroke(1.dp, if(isPressed) Red500 else Slate700), RoundedCornerShape(32.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPressed = true
                                                try {
                                                    awaitRelease()
                                                } finally {
                                                    isPressed = false
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // Progress Background (The Fill)
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(holdProgress.value)
                                        .background(
                                            Brush.horizontalGradient(listOf(Red500.copy(0.4f), Red500))
                                        )
                                )

                                // Instruction Text
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (holdProgress.value > 0.95f) Icons.Default.CheckCircle else Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (holdProgress.value > 0.95f) "TRIP ENDED" else if (isPressed) "HOLDING..." else "PRESS & HOLD TO END",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // -- INSTANT SAFETY TOOLKIT OVERLAY --
        // ----------------------------------------------------
        if (showSafetyToolkit) {
            SafetyToolkitOverlay(
                isRecordingAudio = isRecordingAudio,
                onFakeCallClick = { showFakeCall = true },
                onToggleAudioRecording = {
                    if (isRecordingAudio) {
                        val dur = AudioRecordHelper.getRecordingDurationSec()
                        val file = AudioRecordHelper.stopRecording()
                        isRecordingAudio = false
                        if (file != null && file.exists()) {
                            onUploadAudio(file, activeTrip?.id, dur, "manual_toolkit")
                            Toast.makeText(context, "Audio Blackbox secured & uploading...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Audio recording saved securely to device.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            val tripId = activeTrip?.id
                            val started = AudioRecordHelper.startRecording(context, tripId)
                            isRecordingAudio = started
                            if (started) {
                                Toast.makeText(context, "Silent Audio Blackbox active.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            showAudioDisclosureDialog = true
                        }
                    }
                },
                onSilentAlertClick = {
                    onTriggerSos()
                },
                onDial999Click = {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:999")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showSafetyToolkit = false }
            )
        }

        // ----------------------------------------------------
        // -- FAKE INCOMING CALL FULL-SCREEN OVERLAY --
        // ----------------------------------------------------
        if (showFakeCall) {
            FakeCallOverlay(
                callerName = "Mom (আম্মু)",
                callerNumber = "+880 1712-345678",
                onDismiss = { showFakeCall = false }
            )
        }

        // ----------------------------------------------------
        // -- ADD / SHARE GUARDIAN QUICK DIALOG --
        // ----------------------------------------------------
        if (showAddGuardianDialog) {
            AddGuardianQuickDialog(
                guardians = guardians,
                notifiedPhones = activeTrip?.notifiedGuardians ?: selectedGuardiansToShare,
                tripId = activeTrip?.id ?: "",
                onAddGuardians = { newPhones ->
                    activeTrip?.let { trip ->
                        onShareTrip(trip.id, newPhones)
                        newPhones.forEach { p ->
                            if (!selectedGuardiansToShare.contains(p)) selectedGuardiansToShare.add(p)
                        }
                    }
                },
                onDismiss = { showAddGuardianDialog = false }
            )
        }

        // Google Play Compliant Prominent Disclosure for Audio Blackbox Recording
        PermissionDisclosureDialog(
            showDialog = showAudioDisclosureDialog,
            permissionType = SafetyPermissionType.AUDIO,
            onConfirm = {
                showAudioDisclosureDialog = false
                audioRecordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = {
                showAudioDisclosureDialog = false
            }
        )
    }
}
