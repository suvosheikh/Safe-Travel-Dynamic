package com.safetravel.tracker

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

import com.safetravel.tracker.ui.screens.MainTabbedLayout
class MainActivity : ComponentActivity() {
    var pendingTripId: String? = null
    var pendingAction: String? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("trip_id")?.let { tripId ->
            pendingTripId = tripId
        }
        pendingAction = intent.action
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.safetravel.tracker.supabase.SupabaseManager.initSession(applicationContext)
        com.safetravel.tracker.ads.AdMobManager.initialize(this)
        pendingTripId = intent?.getStringExtra("trip_id")
        pendingAction = intent?.action
        enableEdgeToEdge()
        setContent {
            SafeTravelTheme {
                val vm: SafeTravelViewModel = viewModel()
                val profile by vm.userProfile.collectAsState()
                var showSplash by remember { mutableStateOf(true) }
                val context = LocalContext.current

                val launchedAction = pendingAction ?: intent?.action
                LaunchedEffect(launchedAction) {
                    if (launchedAction == com.safetravel.tracker.services.TripTrackingService.ACTION_SOS) {
                        vm.triggerSos()
                        pendingAction = null
                        intent?.action = null
                    } else if (launchedAction == com.safetravel.tracker.services.TripTrackingService.ACTION_END_TRIP) {
                        val currentBat = com.safetravel.tracker.util.BatteryHelper.getCurrentBatteryLevel(context)
                        vm.endTrip(currentBat, 0.0)
                        pendingAction = null
                        intent?.action = null
                    }
                }

                // Post-notification permission launcher (available for contextual requests)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    android.util.Log.d("MainActivity", "Notification permission granted: $isGranted")
                }

                // Synchronize FCM Registration Token with Supabase Profiles
                LaunchedEffect(profile) {
                    if (profile != null) {
                        com.safetravel.tracker.notification.FcmTokenManager.retrieveAndSyncFcmToken(context, profile!!.id)
                    }
                }

                // Observe Global Session Events
                LaunchedEffect(Unit) {
                    com.safetravel.tracker.supabase.SupabaseManager.sessionEvents.collect { message ->
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                        vm.logout()
                    }
                }

                LaunchedEffect(Unit) {
                    com.safetravel.tracker.supabase.SupabaseManager.initSession(applicationContext)
                    vm.checkForExistingSession()
                    delay(5000) // Stay on the splash screen for 5 seconds
                    showSplash = false
                }

                val activeTrip by vm.activeTrip.collectAsState()

                LaunchedEffect(activeTrip) {
                    if (activeTrip != null && (activeTrip?.status == "ongoing" || activeTrip?.status == "sos")) {
                        val serviceIntent = Intent(context, com.safetravel.tracker.services.TripTrackingService::class.java).apply {
                            this.action = com.safetravel.tracker.services.TripTrackingService.ACTION_START
                            putExtra(com.safetravel.tracker.services.TripTrackingService.EXTRA_TRIP_ID, activeTrip?.id)
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        val serviceIntent = Intent(context, com.safetravel.tracker.services.TripTrackingService::class.java).apply {
                            this.action = com.safetravel.tracker.services.TripTrackingService.ACTION_STOP
                        }
                        context.startService(serviceIntent)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Slate900
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        if (showSplash) {
                            SplashScreen()
                        } else {
                            if (profile == null) {
                                LoginView(vm)
                            } else {
                                MainTabbedLayout(vm)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainTabbedLayoutPreview() {
    SafeTravelTheme {
        MainTabbedLayout(vm = viewModel())
    }
}

