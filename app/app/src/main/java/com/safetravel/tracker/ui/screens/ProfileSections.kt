package com.safetravel.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safetravel.tracker.supabase.Guardian
import com.safetravel.tracker.supabase.SupabaseProfile
import com.safetravel.tracker.ui.components.GlassBottomSheetContainer
import com.safetravel.tracker.ui.theme.*

// Theme Accent Colors
private val CyberEmerald = Color(0xFF10B981)
private val CyberCyan = Color(0xFF06B6D4)
private val CyberAmber = Color(0xFFF59E0B)
private val EmergencyRed = Color(0xFFEF4444)
private val SoftCardBg = Color(0x18FFFFFF)        // ~9% translucent white glass
private val HairlineBorder = Color(0x1FFFFFFF)    // Subtle 8-12% white outline

// =========================================================================
// 1. SLEEK PROFILE HEADER CARD
// =========================================================================
@Composable
fun ProfileHeaderCard(
    profile: SupabaseProfile?,
    isUploading: Boolean,
    onPickImage: () -> Unit,
    onEditProfile: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Avatar (70dp) with Camera Badge
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.clickable { onPickImage() }
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.35f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile?.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = profile?.avatarUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initial = profile?.fullName?.trim()?.firstOrNull()?.uppercase() ?: "U"
                        Text(
                            text = initial,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan
                        )
                    }

                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = CyberEmerald,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                // Camera Trigger
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(CyberEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUploading) Icons.Default.CloudSync else Icons.Default.PhotoCamera,
                        contentDescription = "Upload Photo",
                        tint = Color(0xFF020617),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Name, Phone & Membership Badge
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = profile?.fullName?.ifBlank { "Traveler Name" } ?: "Traveler Name",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Identity",
                        tint = CyberEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = profile?.phoneNumber?.ifBlank { "No phone registered" } ?: "No phone registered",
                    fontSize = 12.sp,
                    color = Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isPremium = profile?.isPremium == true
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isPremium) CyberAmber.copy(alpha = 0.16f) else CyberEmerald.copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPremium) "ELITE MEMBER" else "STANDARD TIER",
                            color = if (isPremium) CyberAmber else CyberEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${profile?.pointsBalance ?: 0} PTS",
                            color = Slate300,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Edit Action
            IconButton(
                onClick = onEditProfile,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = Slate200,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// =========================================================================
// 2. MINIMAL SAFETY READINESS STRIP
// =========================================================================
@Composable
fun ProfileSafetyStatusBar(
    safetyScore: Int,
    actionPrompt: String?,
    onActionClick: () -> Unit
) {
    val statusColor = when {
        safetyScore >= 80 -> CyberEmerald
        safetyScore >= 50 -> CyberAmber
        else -> EmergencyRed
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftCardBg),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SAFETY READINESS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "$safetyScore%",
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { safetyScore / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = Color.White.copy(alpha = 0.08f)
            )

            if (!actionPrompt.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onActionClick() }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = actionPrompt,
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 3. GROUPED MENU CARD CONTAINER
// =========================================================================
@Composable
fun ProfileMenuGroupCard(
    headerTitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        if (!headerTitle.isNullOrBlank()) {
            Text(
                text = headerTitle,
                color = Slate400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
            )
        }
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SoftCardBg),
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

// =========================================================================
// 4. GROUPED MENU ROW ITEM (iOS / Uber / Life360 Style)
// =========================================================================
@Composable
fun ProfileMenuRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    trailingBadge: String? = null,
    isDestructive: Boolean = false,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = if (isDestructive) EmergencyRed else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Slate400,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!trailingBadge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trailingBadge,
                            color = accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 60.dp, end = 14.dp),
                color = Color.White.copy(alpha = 0.05f)
            )
        }
    }
}

// =========================================================================
// 5. DETAIL BOTTOM SHEETS (Progressive Disclosure)
// =========================================================================

// --- A. MEDICAL ID (ICE) BOTTOM SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalDetailBottomSheet(
    profile: SupabaseProfile?,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit
) {
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(EmergencyRed.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MedicalServices, null, tint = EmergencyRed, modifier = Modifier.size(15.dp))
                        }
                        Text(
                            text = "EMERGENCY MEDICAL ID (ICE)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }

                // Vitals Row (Blood & Height/Weight)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmergencyRed.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Bloodtype, null, tint = EmergencyRed, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = profile?.bloodGroup?.ifBlank { "NOT SET" } ?: "NOT SET",
                                    color = EmergencyRed,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text("BLOOD GROUP", color = Slate400, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("HEIGHT / WEIGHT", color = Slate400, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile?.heightWeight?.ifBlank { "Not recorded" } ?: "Not recorded",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Medical Conditions Breakdown
                DetailInfoItem("Known Allergies", profile?.allergies, Icons.Default.WarningAmber, EmergencyRed)
                DetailInfoItem("Chronic Conditions", profile?.chronicConditions, Icons.Default.MonitorHeart, CyberAmber)
                DetailInfoItem("Current Medications", profile?.currentMedications, Icons.Default.Medication, CyberCyan)
                DetailInfoItem("Special Needs / Assistance", profile?.specialNeeds, Icons.Default.Info, Slate300)

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onEditClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color(0xFF020617), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EDIT MEDICAL INFORMATION", color = Color(0xFF020617), fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

// --- B. GUARDIANS NETWORK BOTTOM SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardiansListBottomSheet(
    guardians: List<Guardian>,
    onDismiss: () -> Unit,
    onAddGuardianClick: () -> Unit,
    onGuardianClick: (Guardian) -> Unit,
    onCallGuardian: (String) -> Unit
) {
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CyberEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Shield, null, tint = CyberEmerald, modifier = Modifier.size(15.dp))
                        }
                        Text(
                            text = "TRUSTED GUARDIANS",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${guardians.size} ACTIVE",
                                color = CyberEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }

                if (guardians.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.GroupAdd, null, tint = CyberEmerald, modifier = Modifier.size(32.dp))
                            Text("No Guardians Connected", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Add trusted contacts to receive emergency live GPS telemetry and distress messages during SOS alerts.",
                                color = Slate400,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        guardians.forEach { guardian ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(12.dp))
                                    .clickable {
                                        onDismiss()
                                        onGuardianClick(guardian)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(CyberEmerald.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initial = guardian.relationship.trim().firstOrNull()?.uppercase() ?: "G"
                                        Text(initial, color = CyberEmerald, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                    }
                                    Column {
                                        Text(guardian.name.ifBlank { "Guardian" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(guardian.relationship.ifBlank { "Guardian" }.uppercase(), color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(guardian.phone.ifBlank { "No number" }, color = Slate400, fontSize = 10.sp)
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onCallGuardian(guardian.phone) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(CyberEmerald.copy(alpha = 0.16f))
                                ) {
                                    Icon(Icons.Default.Call, null, tint = CyberEmerald, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onAddGuardianClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFF020617), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ADD NEW GUARDIAN", color = Color(0xFF020617), fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

// --- C. CITIZEN IDENTITY BOTTOM SHEET ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityDetailBottomSheet(
    profile: SupabaseProfile?,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit
) {
    var isNidVisible by remember { mutableStateOf(false) }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Badge, null, tint = CyberCyan, modifier = Modifier.size(15.dp))
                        }
                        Text(
                            text = "CITIZEN IDENTITY & ADDRESS",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }

                // NID Row with toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Pin, null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Column {
                            Text("NID / PASSPORT NUMBER", color = Slate400, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            val nidRaw = profile?.nidPassport.orEmpty()
                            val displayNid = when {
                                nidRaw.isBlank() -> "Not Registered"
                                isNidVisible -> nidRaw
                                else -> if (nidRaw.length > 5) nidRaw.take(4) + "-•••••-" + nidRaw.takeLast(2) else "••••••"
                            }
                            Text(displayNid, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!profile?.nidPassport.isNullOrBlank()) {
                        IconButton(onClick = { isNidVisible = !isNidVisible }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (isNidVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle",
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Personal Details (DOB & Gender)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("DATE OF BIRTH", color = Slate400, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(profile?.dob ?: "Not Set", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("GENDER", color = Slate400, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(profile?.gender ?: "Not Set", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Address Breakdown
                DetailInfoItem("Residential Address", profile?.presentAddress, Icons.Default.Home, CyberCyan)
                if (!profile?.permanentAddress.isNullOrBlank()) {
                    DetailInfoItem("Permanent Hometown Address", profile.permanentAddress, Icons.Default.LocationCity, Slate300)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        onDismiss()
                        onEditClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color(0xFF020617), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EDIT CITIZEN IDENTITY", color = Color(0xFF020617), fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailInfoItem(
    label: String,
    value: String?,
    icon: ImageVector,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Column {
            Text(label.uppercase(), color = Slate400, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value?.ifBlank { "Not provided" } ?: "Not provided",
                color = if (value.isNullOrBlank()) Slate500 else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
