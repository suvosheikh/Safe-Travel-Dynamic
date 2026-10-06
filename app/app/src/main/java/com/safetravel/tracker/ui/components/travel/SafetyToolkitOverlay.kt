package com.safetravel.tracker.ui.components.travel

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.ui.theme.*

@Composable
fun SafetyToolkitOverlay(
    isRecordingAudio: Boolean,
    onFakeCallClick: () -> Unit,
    onToggleAudioRecording: () -> Unit,
    onSilentAlertClick: () -> Unit,
    onDial999Click: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating Frosted Card on the right
        Surface(
            modifier = Modifier
                .padding(end = 16.dp)
                .width(280.dp)
                .clickable(enabled = false) {}, // prevent closing when clicking inside
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF00A0F1D), // Translucent obsidian glass
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Safety Toolkit",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Fake Incoming Call
                SafetyToolItem(
                    icon = Icons.Default.PhoneCallback,
                    iconTint = Color(0xFF3B82F6),
                    iconBg = Color(0x1F3B82F6),
                    title = "Fake Incoming Call",
                    subtitle = "Trigger mock incoming call",
                    onClick = {
                        onDismiss()
                        onFakeCallClick()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Silent Audio Recording
                SafetyToolItem(
                    icon = if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                    iconTint = if (isRecordingAudio) Color(0xFFEF4444) else Color(0xFFF59E0B),
                    iconBg = if (isRecordingAudio) Color(0x33EF4444) else Color(0x1FF59E0B),
                    title = if (isRecordingAudio) "Stop Audio Blackbox" else "Silent Audio Record",
                    subtitle = if (isRecordingAudio) "Recording now... Tap to stop" else "Record cabin audio secretly",
                    badge = if (isRecordingAudio) "REC" else null,
                    badgeColor = Color(0xFFEF4444),
                    onClick = {
                        onToggleAudioRecording()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Emergency SOS
                SafetyToolItem(
                    icon = Icons.Default.Warning,
                    iconTint = Color(0xFFEF4444),
                    iconBg = Color(0x1FEF4444),
                    title = "Trigger Emergency SOS",
                    subtitle = "Alert all guardians with live tracking",
                    onClick = {
                        onSilentAlertClick()
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Quick 999 Dial
                SafetyToolItem(
                    icon = Icons.Default.LocalPolice,
                    iconTint = Color(0xFFEF4444),
                    iconBg = Color(0x1FEF4444),
                    title = "Quick 999 Hotline",
                    subtitle = "National Emergency Call",
                    onClick = {
                        onDismiss()
                        onDial999Click()
                    }
                )
            }
        }
    }
}

@Composable
private fun SafetyToolItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = Color(0xFF10B981),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.25f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
