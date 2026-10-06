package com.safetravel.tracker.ui.components.travel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.safetravel.tracker.ui.theme.Red500
import com.safetravel.tracker.ui.theme.Slate400
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Intelligent SOS Trigger Node featuring:
 * 1. Hold-to-Trigger circular progress ring (default 2s, configurable).
 * 2. 3-Second activation grace window with cancel option to prevent accidental false alarms.
 */
@Composable
fun SosHoldTriggerButton(
    nodeSize: Dp = 180.dp,
    holdDurationSec: Int = 2,
    graceCountdownSec: Int = 3,
    onSosConfirmed: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Hold progress (0f to 1f)
    val holdProgress = remember { Animatable(0f) }
    var isPressed by remember { mutableStateOf(false) }
    var showGraceDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse_1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse_2"
    )

    // Handle press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            val targetMs = (holdDurationSec * 1000).coerceAtLeast(100)
            val result = holdProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = targetMs, easing = LinearEasing)
            )
            if (result.endReason == AnimationEndReason.Finished) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isPressed = false
                showGraceDialog = true
            }
        } else {
            holdProgress.animateTo(0f, tween(250))
        }
    }

    Box(
        modifier = Modifier.size(nodeSize),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing energy rings
        Box(
            modifier = Modifier
                .scale(pulseScale2)
                .size(nodeSize * 0.5f)
                .clip(CircleShape)
                .background(Red500.copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .scale(pulseScale1)
                .size(nodeSize * 0.6f)
                .clip(CircleShape)
                .background(Red500.copy(alpha = 0.25f))
        )

        // Circular Hold Progress Track Canvas
        Canvas(modifier = Modifier.size(nodeSize * 0.72f)) {
            val strokeWidth = 5.dp.toPx()
            // Inactive track
            drawCircle(
                color = Color(0x33FFFFFF),
                style = Stroke(width = strokeWidth)
            )
            // Active progress arc
            if (holdProgress.value > 0f) {
                drawArc(
                    color = Color(0xFFEF4444),
                    startAngle = -90f,
                    sweepAngle = holdProgress.value * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth + 2f, cap = StrokeCap.Round)
                )
            }
        }

        // Central Action Button
        Box(
            modifier = Modifier
                .size(nodeSize * 0.6f)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Red500, Color(0xFF991B1B))))
                .border(BorderStroke(4.dp, Color.White.copy(alpha = 0.5f)), CircleShape)
                .pointerInput(holdDurationSec) {
                    detectTapGestures(
                        onPress = {
                            if (holdDurationSec <= 0) {
                                // Instant trigger mode if user set 0 sec
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showGraceDialog = true
                            } else {
                                isPressed = true
                                val released = tryAwaitRelease()
                                isPressed = false
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "SOS Warning",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "SOS",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                if (holdDurationSec > 0) {
                    Text(
                        text = if (isPressed) "HOLD..." else "HOLD ${holdDurationSec}S",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ----------------------------------------------------
    // -- 3-SECOND GRACE PERIOD ACTIVATION COUNTDOWN MODAL --
    // ----------------------------------------------------
    if (showGraceDialog) {
        Dialog(
            onDismissRequest = { /* Prevent dismiss by clicking outside */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            var remainingSeconds by remember { mutableIntStateOf(graceCountdownSec) }

            LaunchedEffect(Unit) {
                while (remainingSeconds > 0) {
                    delay(1000L)
                    remainingSeconds -= 1
                }
                showGraceDialog = false
                onSosConfirmed()
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(24.dp)),
                color = Color(0xFA0F172A),
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33EF4444))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ACTIVATING EMERGENCY SOS",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Countdown Ring
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { remainingSeconds.toFloat() / graceCountdownSec.toFloat() },
                            modifier = Modifier.size(90.dp),
                            color = Color(0xFFEF4444),
                            strokeWidth = 6.dp,
                            trackColor = Color(0x33FFFFFF)
                        )
                        Text(
                            text = "$remainingSeconds",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Alerting all guardians and capturing evidence...",
                        color = Slate400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Cancel Button
                    Button(
                        onClick = {
                            showGraceDialog = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x2E64748B)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Cancel (Accidental Tap)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
