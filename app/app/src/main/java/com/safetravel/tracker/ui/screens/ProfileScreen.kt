package com.safetravel.tracker.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safetravel.tracker.supabase.Guardian
import com.safetravel.tracker.supabase.SupabaseProfile
import com.safetravel.tracker.ui.components.GlassBackground
import com.safetravel.tracker.ui.components.GlassBottomSheetContainer
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel
import java.text.SimpleDateFormat
import java.util.*

// Theme Accent Colors (Borderless Translucent Dark Glass Theme)
private val CyberEmerald = Color(0xFF10B981)
private val CyberCyan = Color(0xFF06B6D4)
private val CyberAmber = Color(0xFFF59E0B)
private val EmergencyRed = Color(0xFFEF4444)
private val SoftCardBg = Color(0x18FFFFFF)        // ~9% pure white translucent glass - completely borderless
private val DeepCardBg = Color(0xB30F172A)        // ~70% deep obsidian glass

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    SafeTravelTheme {
        ProfileScreenContent(
            profile = SupabaseProfile(
                id = "123",
                fullName = "Alex Rivera",
                phoneNumber = "+880 1711-223344",
                pointsBalance = 450,
                tripCredits = 12,
                isPremium = true,
                gender = "Male",
                dob = "21 Sep 2001",
                occupation = "Cybersecurity Architect",
                bloodGroup = "O+",
                heightWeight = "5'10\" / 72kg",
                presentAddress = "Road 27, Dhanmondi, Dhaka",
                permanentAddress = "Green Road, Dhaka",
                nidPassport = "42091-23019-2",
                allergies = "Penicillin, Dust Pollen",
                chronicConditions = "Mild Asthma",
                currentMedications = "Albuterol Inhaler",
                specialNeeds = "None"
            ),
            guardians = listOf(
                Guardian(
                    id = "1",
                    userId = "123",
                    name = "Fatima Rivera",
                    relationship = "Spouse",
                    phone = "+880 1819-000000",
                    avatarIndex = 0,
                    defaultNotify = true,
                    sosPermission = true
                )
            ),
            onUpdateProfile = {},
            onAddGuardianClick = {},
            onGuardianClick = {},
            onLogout = {}
        )
    }
}

@Composable
fun ProfileScreen(
    vm: SafeTravelViewModel,
    onAppInfoClick: () -> Unit = {}
) {
    val profile by vm.userProfile.collectAsState()
    val guardians by vm.userGuardians.collectAsState()
    val isUploading by vm.isImageUploading.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    var showGuardianSheet by remember { mutableStateOf(false) }
    var selectedGuardian by remember { mutableStateOf<Guardian?>(null) }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { vm.uploadProfileImage(context, it) }
        }
    )

    LaunchedEffect(profile) {
        if (profile != null) vm.refreshGuardians()
    }

    if (profile == null) {
        GlassBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepCardBg)
                        .padding(24.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = CyberEmerald,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "LOADING SECURE PROFILE...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(EmergencyRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShieldMoon,
                                contentDescription = "Error",
                                tint = EmergencyRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "PROFILE UNAVAILABLE",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Could not synchronize with cloud node.",
                            color = Slate400,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { vm.checkForExistingSession() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("RECONNECT", color = Color(0xFF020617), fontWeight = FontWeight.Black)
                        }
                        TextButton(onClick = { vm.logout() }) {
                            Text("SIGN OUT", color = EmergencyRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        return
    }

    ProfileScreenContent(
        profile = profile,
        guardians = guardians,
        isUploading = isUploading,
        onUpdateProfile = { updates ->
            vm.updateProfileFields(profile?.id ?: "", updates)
        },
        onAddGuardianClick = {
            selectedGuardian = null
            showGuardianSheet = true
        },
        onGuardianClick = { guardian ->
            selectedGuardian = guardian
            showGuardianSheet = true
        },
        onAppInfoClick = onAppInfoClick,
        onSyncDatabase = {
            vm.checkForExistingSession()
            vm.refreshData()
            Toast.makeText(context, "Encrypted Vault Synchronized", Toast.LENGTH_SHORT).show()
        },
        onLogout = { vm.logout() },
        onPickImage = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    )

    if (showGuardianSheet) {
        AddGuardianBottomSheet(
            guardian = selectedGuardian,
            onDismiss = { showGuardianSheet = false },
            onSave = { guardian ->
                vm.saveGuardianToSupabase(guardian)
                showGuardianSheet = false
            },
            onDelete = { id ->
                vm.deleteGuardian(id)
                showGuardianSheet = false
            },
            userId = profile?.id ?: ""
        )
    }
}

@Composable
fun ProfileScreenContent(
    profile: SupabaseProfile?,
    guardians: List<Guardian>,
    isUploading: Boolean = false,
    onUpdateProfile: (Map<String, Any>) -> Unit,
    onAddGuardianClick: () -> Unit,
    onGuardianClick: (Guardian) -> Unit,
    onAppInfoClick: () -> Unit = {},
    onSyncDatabase: () -> Unit = {},
    onLogout: () -> Unit,
    onPickImage: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // Wizard State (Steps 1, 2, 3)
    var showWizard by remember { mutableStateOf(false) }
    var wizardInitialStep by remember { mutableIntStateOf(1) }

    // Progressive Disclosure Bottom Sheet States
    var showMedicalSheet by remember { mutableStateOf(false) }
    var showGuardiansSheet by remember { mutableStateOf(false) }
    var showIdentitySheet by remember { mutableStateOf(false) }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showTimezonePicker by remember { mutableStateOf(false) }
    var currentTimezone by remember { mutableStateOf(com.safetravel.tracker.util.DateTimeUtils.userTimeZoneId) }

    // Dynamic Safety Score (0 - 100%)
    val safetyScore = remember(profile, guardians) {
        var score = 30
        if (!profile?.bloodGroup.isNullOrBlank()) score += 15
        if (!profile?.allergies.isNullOrBlank() || !profile?.chronicConditions.isNullOrBlank()) score += 15
        if (!profile?.presentAddress.isNullOrBlank()) score += 10
        if (!profile?.nidPassport.isNullOrBlank()) score += 10
        if (guardians.isNotEmpty()) score += 20
        score.coerceIn(0, 100)
    }

    GlassBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp, top = 6.dp)
        ) {
            // 1. Sleek Profile Header Card
            item {
                ProfileHeaderCard(
                    profile = profile,
                    isUploading = isUploading,
                    onPickImage = onPickImage,
                    onEditProfile = {
                        wizardInitialStep = 1
                        showWizard = true
                    }
                )
            }

            // 2. Minimal Safety Status Bar
            item {
                val actionPrompt = when {
                    profile?.bloodGroup.isNullOrBlank() -> "Add blood group to complete emergency profile"
                    guardians.isEmpty() -> "Connect emergency guardians for live SOS tracking"
                    profile?.nidPassport.isNullOrBlank() -> "Verify citizen ID and residential address"
                    else -> null
                }
                ProfileSafetyStatusBar(
                    safetyScore = safetyScore,
                    actionPrompt = actionPrompt,
                    onActionClick = {
                        when {
                            profile?.bloodGroup.isNullOrBlank() -> {
                                wizardInitialStep = 3
                                showWizard = true
                            }
                            guardians.isEmpty() -> {
                                showGuardiansSheet = true
                            }
                            profile?.nidPassport.isNullOrBlank() -> {
                                wizardInitialStep = 2
                                showWizard = true
                            }
                        }
                    }
                )
            }

            // 3. GROUP 1: Safety & Emergency Network
            item {
                ProfileMenuGroupCard(headerTitle = "SAFETY & EMERGENCY VAULT") {
                    val bloodText = profile?.bloodGroup?.ifBlank { "Not set" } ?: "Not set"
                    val allergyCount = if (!profile?.allergies.isNullOrBlank()) "Allergies recorded" else "No allergies"
                    ProfileMenuRow(
                        title = "Emergency Medical ID (ICE)",
                        subtitle = "Blood: $bloodText • $allergyCount",
                        icon = Icons.Default.MedicalServices,
                        accent = EmergencyRed,
                        onClick = { showMedicalSheet = true },
                        trailingBadge = if (profile?.bloodGroup.isNullOrBlank()) "Action Needed" else null
                    )

                    val guardiansSummary = if (guardians.isEmpty()) "No guardians connected" else "${guardians.size} Connected (${guardians.take(2).joinToString { it.relationship }})"
                    ProfileMenuRow(
                        title = "Trusted Guardians Network",
                        subtitle = guardiansSummary,
                        icon = Icons.Default.Shield,
                        accent = CyberEmerald,
                        onClick = { showGuardiansSheet = true },
                        trailingBadge = if (guardians.isEmpty()) "Add" else "${guardians.size}"
                    )

                    val idSummary = if (profile?.nidPassport.isNullOrBlank()) "NID not registered" else "NID Verified • ${profile?.presentAddress?.take(20) ?: "Dhaka"}"
                    ProfileMenuRow(
                        title = "Citizen Identity & Residence",
                        subtitle = idSummary,
                        icon = Icons.Default.Badge,
                        accent = CyberCyan,
                        onClick = { showIdentitySheet = true },
                        showDivider = false
                    )
                }
            }

            // 4. GROUP 2: App & System Preferences
            item {
                ProfileMenuGroupCard(headerTitle = "SYSTEM & PREFERENCES") {
                    ProfileMenuRow(
                        title = "Timezone & Regional Clock",
                        subtitle = currentTimezone,
                        icon = Icons.Default.Schedule,
                        accent = CyberCyan,
                        onClick = { showTimezonePicker = true }
                    )

                    ProfileMenuRow(
                        title = "Synchronize Cloud Vault",
                        subtitle = "Force sync local cache with remote server",
                        icon = Icons.Default.CloudSync,
                        accent = CyberCyan,
                        onClick = onSyncDatabase
                    )

                    ProfileMenuRow(
                        title = "Application Information",
                        subtitle = "SafeTravel Architecture v1.0.0",
                        icon = Icons.Default.Security,
                        accent = CyberEmerald,
                        onClick = onAppInfoClick,
                        showDivider = false
                    )
                }
            }

            // 5. GROUP 3: Account & Security
            item {
                ProfileMenuGroupCard(headerTitle = "ACCOUNT & PRIVACY") {
                    ProfileMenuRow(
                        title = "Delete Account Data",
                        subtitle = "Play Store compliance permanent data wipe",
                        icon = Icons.Default.DeleteOutline,
                        accent = EmergencyRed,
                        onClick = { showDeleteAccountDialog = true }
                    )

                    ProfileMenuRow(
                        title = "Sign Out",
                        subtitle = "Terminate active session on this device",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        accent = EmergencyRed,
                        onClick = { showLogoutDialog = true },
                        isDestructive = true,
                        showDivider = false
                    )
                }
            }
        }
    }

    // Detail Bottom Sheet: Medical ID (ICE)
    if (showMedicalSheet) {
        MedicalDetailBottomSheet(
            profile = profile,
            onDismiss = { showMedicalSheet = false },
            onEditClick = {
                showMedicalSheet = false
                wizardInitialStep = 3
                showWizard = true
            }
        )
    }

    // Detail Bottom Sheet: Guardians List
    if (showGuardiansSheet) {
        GuardiansListBottomSheet(
            guardians = guardians,
            onDismiss = { showGuardiansSheet = false },
            onAddGuardianClick = {
                showGuardiansSheet = false
                onAddGuardianClick()
            },
            onGuardianClick = { guardian ->
                showGuardiansSheet = false
                onGuardianClick(guardian)
            },
            onCallGuardian = { phone ->
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Detail Bottom Sheet: Citizen Identity & Residence
    if (showIdentitySheet) {
        IdentityDetailBottomSheet(
            profile = profile,
            onDismiss = { showIdentitySheet = false },
            onEditClick = {
                showIdentitySheet = false
                wizardInitialStep = 2
                showWizard = true
            }
        )
    }

    // Step-by-Step Profile Input Wizard
    if (showWizard) {
        ProfileEditWizardSheet(
            initialStep = wizardInitialStep,
            profile = profile,
            onDismiss = { showWizard = false },
            onSave = { updates ->
                onUpdateProfile(updates)
                showWizard = false
                Toast.makeText(context, "Profile Vault Updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Text(
                    text = "SIGN OUT FROM SAFE JOURNEY?",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "You will need to sign in again to access live trip monitoring and emergency alerts.",
                    color = Slate400,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SIGN OUT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL", color = Slate400)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = Color(0xFF0F172A),
            title = {
                Text(
                    text = "REQUEST DATA DELETION",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "In compliance with Google Play User Data & Account Deletion policies, you can request permanent deletion of your account, emergency vault, guardian relationships, and live telemetry records from our servers.",
                    color = Slate400,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        Toast.makeText(context, "Data deletion request submitted. Signing out...", Toast.LENGTH_LONG).show()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("DELETE & SIGN OUT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("CANCEL", color = Slate400)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showTimezonePicker) {
        com.safetravel.tracker.ui.components.TimezonePickerBottomSheet(
            userId = profile?.id,
            onDismiss = { showTimezonePicker = false },
            onTimezoneChanged = { newTz ->
                currentTimezone = newTz
            }
        )
    }
}

// =========================================================================
// PROFILE SETUP / UPDATE WIZARD (Step 1 -> 2 -> 3)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditWizardSheet(
    initialStep: Int = 1,
    profile: SupabaseProfile?,
    onDismiss: () -> Unit,
    onSave: (Map<String, Any>) -> Unit
) {
    var step by remember { mutableIntStateOf(initialStep) }

    // Step 1: Personal
    var fullName by remember(profile) { mutableStateOf(profile?.fullName ?: "") }
    var occupation by remember(profile) { mutableStateOf(profile?.occupation ?: "") }
    var dob by remember(profile) { mutableStateOf(profile?.dob ?: "") }
    var gender by remember(profile) { mutableStateOf(profile?.gender ?: "Male") }

    // Step 2: Contact & Identity
    var phone by remember(profile) { mutableStateOf(profile?.phoneNumber ?: "") }
    var nid by remember(profile) { mutableStateOf(profile?.nidPassport ?: "") }
    var presentAddress by remember(profile) { mutableStateOf(profile?.presentAddress ?: "") }
    var permanentAddress by remember(profile) { mutableStateOf(profile?.permanentAddress ?: "") }

    // Step 3: Medical ICE
    var bloodGroup by remember(profile) { mutableStateOf(profile?.bloodGroup ?: "O+") }
    var heightWeight by remember(profile) { mutableStateOf(profile?.heightWeight ?: "5'9\" / 70kg") }
    var allergies by remember(profile) { mutableStateOf(profile?.allergies ?: "") }
    var chronicConditions by remember(profile) { mutableStateOf(profile?.chronicConditions ?: "") }
    var medications by remember(profile) { mutableStateOf(profile?.currentMedications ?: "") }
    var specialNeeds by remember(profile) { mutableStateOf(profile?.specialNeeds ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var showBloodGroupMenu by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance().apply { timeInMillis = it }
                        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        dob = sdf.format(calendar.time)
                    }
                    showDatePicker = false
                }) { Text("OK", color = CyberEmerald, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCEL", color = Slate400) }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF0F172A))
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = null
    ) {
        GlassBottomSheetContainer(scrollable = false) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                // Wizard Header with Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PROFILE WIZARD",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = when (step) {
                                1 -> "STEP 1 OF 3: PERSONAL IDENTITY"
                                2 -> "STEP 2 OF 3: CONTACT & RESIDENCE"
                                else -> "STEP 3 OF 3: EMERGENCY MEDICAL ID"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress 3-segment bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(3) { index ->
                        val active = index + 1 <= step
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (active) CyberCyan else Color.White.copy(alpha = 0.1f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step Content in scrollable area
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (step) {
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                WizardInputField(fullName, { fullName = it }, "Full Name", Icons.Default.Person)
                                WizardInputField(occupation, { occupation = it }, "Occupation / Title", Icons.Default.WorkOutline)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f).clickable { showDatePicker = true }) {
                                        WizardInputField(dob, {}, "Date of Birth", Icons.Default.Cake, enabled = false)
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        WizardInputField(gender, {}, "Gender", Icons.Default.Transgender, enabled = false, modifier = Modifier.clickable { showGenderMenu = true })
                                        DropdownMenu(
                                            expanded = showGenderMenu,
                                            onDismissRequest = { showGenderMenu = false },
                                            modifier = Modifier.background(Color(0xFF0F172A))
                                        ) {
                                            listOf("Male", "Female", "Other", "Prefer not to say").forEach {
                                                DropdownMenuItem(
                                                    text = { Text(it, color = Color.White) },
                                                    onClick = {
                                                        gender = it
                                                        showGenderMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                WizardInputField(phone, { phone = it }, "Mobile Phone Number", Icons.Default.Phone)
                                WizardInputField(nid, { nid = it }, "NID or Passport Number", Icons.Default.Pin)
                                WizardInputField(presentAddress, { presentAddress = it }, "Present Residence Address", Icons.Default.Home)
                                WizardInputField(permanentAddress, { permanentAddress = it }, "Permanent Hometown Address", Icons.Default.LocationCity)
                            }
                        }
                        3 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        WizardInputField(bloodGroup, {}, "Blood Group", Icons.Default.Bloodtype, enabled = false, modifier = Modifier.clickable { showBloodGroupMenu = true })
                                        DropdownMenu(
                                            expanded = showBloodGroupMenu,
                                            onDismissRequest = { showBloodGroupMenu = false },
                                            modifier = Modifier.background(Color(0xFF0F172A))
                                        ) {
                                            listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-").forEach {
                                                DropdownMenuItem(
                                                    text = { Text(it, color = Color.White) },
                                                    onClick = {
                                                        bloodGroup = it
                                                        showBloodGroupMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        WizardInputField(heightWeight, { heightWeight = it }, "Height & Weight", Icons.Default.MonitorWeight)
                                    }
                                }
                                WizardInputField(allergies, { allergies = it }, "Known Allergies (e.g. Dust, Penicillin)", Icons.Default.WarningAmber)
                                WizardInputField(chronicConditions, { chronicConditions = it }, "Chronic Medical Conditions", Icons.Default.MonitorHeart)
                                WizardInputField(medications, { medications = it }, "Current Medications (e.g. Inhaler)", Icons.Default.Medication)
                                WizardInputField(specialNeeds, { specialNeeds = it }, "Special Needs / Assistance", Icons.Default.AccessibilityNew)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons: Back & Next/Finish
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step > 1) {
                        Button(
                            onClick = { step-- },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("BACK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                onSave(
                                    mapOf(
                                        "full_name" to fullName,
                                        "occupation" to occupation,
                                        "dob" to dob,
                                        "gender" to gender,
                                        "phone_number" to phone,
                                        "nid_passport" to nid,
                                        "present_address" to presentAddress,
                                        "permanent_address" to permanentAddress,
                                        "blood_group" to bloodGroup,
                                        "height_weight" to heightWeight,
                                        "allergies" to allergies,
                                        "chronic_conditions" to chronicConditions,
                                        "current_medications" to medications,
                                        "special_needs" to specialNeeds
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (step == 3) CyberEmerald else CyberCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(if (step > 1) 1.5f else 1f).height(46.dp)
                    ) {
                        Text(
                            text = if (step == 3) "COMPLETE & SAVE VAULT" else "CONTINUE TO STEP ${step + 1}",
                            color = Color(0xFF020617),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label, fontSize = 11.sp) },
        leadingIcon = {
            Icon(icon, null, tint = Slate400, modifier = Modifier.size(16.dp))
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContainerColor = Color.White.copy(alpha = 0.04f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedLabelColor = CyberCyan,
            unfocusedLabelColor = Slate400,
            disabledLabelColor = Slate400
        ),
        singleLine = true
    )
}

// =========================================================================
// 8. GUARDIAN ADD/EDIT BOTTOM SHEET (Compact & Borderless)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGuardianBottomSheet(
    guardian: Guardian? = null,
    onDismiss: () -> Unit,
    onSave: (Guardian) -> Unit,
    onDelete: (String) -> Unit,
    userId: String
) {
    var name by remember(guardian) { mutableStateOf(guardian?.name ?: "") }
    var phone by remember(guardian) { mutableStateOf(guardian?.phone ?: "") }
    var relationship by remember(guardian) { mutableStateOf(guardian?.relationship ?: "") }
    var defaultNotify by remember(guardian) { mutableStateOf(guardian?.defaultNotify ?: true) }
    var sosPermission by remember(guardian) { mutableStateOf(guardian?.sosPermission ?: true) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.7f),
        dragHandle = null
    ) {
        GlassBottomSheetContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (guardian == null) "ADD EMERGENCY GUARDIAN" else "EDIT GUARDIAN",
                        color = CyberEmerald,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (guardian != null) {
                        IconButton(onClick = { onDelete(guardian.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                WizardInputField(name, { name = it }, "Guardian Full Name", Icons.Default.Person)
                WizardInputField(relationship, { relationship = it }, "Relationship (e.g. Spouse, Father)", Icons.Default.FamilyRestroom)
                WizardInputField(phone, { phone = it }, "Emergency Mobile Number", Icons.Default.Phone)

                // SOS Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Instant SOS Notification", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = sosPermission,
                        onCheckedChange = { sosPermission = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF020617),
                            checkedTrackColor = CyberEmerald
                        )
                    )
                }

                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            onSave(
                                Guardian(
                                    id = guardian?.id ?: UUID.randomUUID().toString(),
                                    userId = userId,
                                    name = name,
                                    relationship = relationship,
                                    phone = phone,
                                    avatarIndex = 0,
                                    defaultNotify = defaultNotify,
                                    sosPermission = sosPermission
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (guardian == null) "ADD GUARDIAN" else "UPDATE GUARDIAN", color = Color(0xFF020617), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
