package com.safetravel.tracker.ui.components.travel

import androidx.compose.animation.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.ui.theme.*

/**
 * Top Mission Control Bar for Active Journeys
 */
@Composable
fun MissionControlBar(
    vehiclePlate: String?,
    batteryLevel: Int,
    safetyScore: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color(0xE60F172A), // Translucent Slate 900
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Vehicle Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, null, tint = Emerald400, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = vehiclePlate ?: "Safe Ride",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Safety Score
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (safetyScore > 80) Emerald400 else Red500, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Safety: $safetyScore%",
                    color = if (safetyScore > 80) Emerald400 else Red500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Battery
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        batteryLevel > 80 -> Icons.Default.BatteryFull
                        batteryLevel > 20 -> Icons.Default.BatteryChargingFull
                        else -> Icons.Default.BatteryAlert
                    },
                    contentDescription = null,
                    tint = if (batteryLevel > 20) Slate300 else Red500,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$batteryLevel%",
                    color = Slate300,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Pre-Journey "Airlock" Security Check UI
 */
@Composable
fun PreJourneySecurityCheck(
    vehiclePlate: String,
    onPlateChange: (String) -> Unit,
    onTakePhoto: () -> Unit,
    hasPhoto: Boolean,
    isPhotoLoading: Boolean,
    plateLabel: String = "Vehicle Plate",
    platePlaceholder: String = "e.g. 42-1200"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Slate800.copy(alpha = 0.5f))
            .border(1.dp, Slate700.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "TRIP SAFETY CHECK",
            color = Emerald400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Vehicle Plate Input
            OutlinedTextField(
                value = vehiclePlate,
                onValueChange = onPlateChange,
                label = { Text(plateLabel, fontSize = 12.sp) },
                placeholder = { Text(platePlaceholder, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Slate700,
                    focusedBorderColor = Emerald400,
                    focusedLabelColor = Emerald400
                )
            )

            // Evidence Photo Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (hasPhoto) Emerald400.copy(alpha = 0.2f) else Slate700)
                    .clickable { onTakePhoto() },
                contentAlignment = Alignment.Center
            ) {
                if (isPhotoLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Emerald400, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = if (hasPhoto) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (hasPhoto) Emerald400 else Color.White
                    )
                }
            }
        }
    }
}

/**
 * Square Real-Time Speedometer HUD for Map
 */
@Composable
fun SpeedometerHud(
    speedKmh: Int,
    modifier: Modifier = Modifier
) {
    val isOverSpeed = speedKmh >= 75
    val isWarning = speedKmh in 50..74

    val speedColor = when {
        isOverSpeed -> Color(0xFFEF4444) // Vibrant red
        isWarning -> Color(0xFFF59E0B)   // Amber/yellow
        else -> Color.White
    }

    val borderColor = when {
        isOverSpeed -> Color(0xFFEF4444).copy(alpha = 0.85f)
        isWarning -> Color(0xFFF59E0B).copy(alpha = 0.7f)
        else -> Color(0xFF334155)
    }

    Box(
        modifier = modifier
            .size(62.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE60F172A))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$speedKmh",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = speedColor,
                lineHeight = 22.sp
            )
            Text(
                text = "km/h",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOverSpeed) Color(0xFFEF4444) else Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Who Is Watching Guardians Bar on Tracking Screen
 */
@Composable
fun WhoIsWatchingBar(
    guardians: List<com.safetravel.tracker.supabase.Guardian>,
    notifiedPhones: List<String>,
    onAddGuardianClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeWatchers = guardians.filter { notifiedPhones.contains(it.phone) }

    Surface(
        modifier = modifier,
        color = Color(0xCC0F172A),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Dot & Label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Watching:",
                    color = Slate300,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Watching Guardian Avatars
            if (activeWatchers.isNotEmpty()) {
                activeWatchers.take(3).forEach { guardian ->
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = guardian.name.firstOrNull()?.toString()?.uppercase() ?: "G",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Online Green Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .padding(1.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                    }
                }
                if (activeWatchers.size > 3) {
                    Text(
                        text = "+${activeWatchers.size - 3}",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "No watchers yet",
                    color = Slate400,
                    fontSize = 10.sp
                )
            }

            // Add Guardian (+) Button
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0x1F06B6D4))
                    .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.6f), CircleShape)
                    .clickable { onAddGuardianClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Guardian",
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

