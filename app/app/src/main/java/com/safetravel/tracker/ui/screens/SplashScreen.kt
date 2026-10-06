package com.safetravel.tracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.safetravel.tracker.R
import com.safetravel.tracker.ui.theme.*

import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SafeTravelTheme {
        SplashScreen()
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "Telemetry")
    
    // Rotating outer ring animation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteSpec(2500),
        label = "RingRotation"
    )
    
    // Pulse animation for inner node glow
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteSequenceSpec(1200),
        label = "GlowPulse"
    )

    // Smooth linearly advancing progress bar for the 5-second duration
    val loadingProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        loadingProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 5000,
                easing = LinearEasing
            )
        )
    }

    // Dynamic telemetry status logs synchronized with the progress percentage
    val currentProgress = loadingProgress.value
    val statusText = when {
        currentProgress < 0.25f -> "Starting Safe Travel..."
        currentProgress < 0.55f -> "Connecting to secure cloud..."
        currentProgress < 0.80f -> "Loading your safety profile..."
        else -> "Ready! Welcome to Safe Travel..."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate900
                        Color(0xFF0B111E), // Deeper space variant
                        Color(0xFF05070B)  // Absolute dark bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Futuristic low-density background grid lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 40.dp.toPx()
            val w = size.width
            val h = size.height
            val gridColor = Color(0xFF1E293B).copy(alpha = 0.15f) // subtle Slate800 lines
            
            // Draw horizontal coordinate segments
            var y = 0f
            while (y < h) {
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(w, y), strokeWidth = 1f)
                y += gridSpacing
            }
            // Draw vertical coordinate segments
            var x = 0f
            while (x < w) {
                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, h), strokeWidth = 1f)
                x += gridSpacing
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            // CENTRAL CORE ENHANCED HUB DISPLAY (NO EMOJIS, HIGH-STYLE VECTOR)
            Box(
                modifier = Modifier
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                // outer spinning dotted track
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAngle)
                ) {
                    drawCircle(
                        color = Emerald400.copy(alpha = 0.2f),
                        radius = size.width / 2,
                        style = Stroke(
                            width = 3f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    )
                    drawCircle(
                        color = PremiumGold.copy(alpha = 0.15f),
                        radius = size.width / 2.3f,
                        style = Stroke(
                            width = 1f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 10f), 0f)
                        )
                    )
                }

                // Inner pulsing backglow
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Emerald400.copy(alpha = 0.35f * glowScale),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // solid visual hub core
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        )
                        .border(1.dp, Emerald400.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.safetravel),
                        contentDescription = "SafeTravel logo",
                        tint = Emerald400,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BRAND HEADLINES
            Text(
                text = "SAFE TRAVEL",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "SMART TRAVEL & SAFETY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Slate300.copy(alpha = 0.8f),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // SMOOTH PROGRESS SECTION
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald400
                        )
                        Text(
                            text = "${(currentProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { currentProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = Emerald400,
                        trackColor = Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            // SECURE FOOTER NOTE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secured",
                    tint = PremiumGold,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SECURED & END-TO-END ENCRYPTED",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumGold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// Helper methods for clean infinite animations
private fun infiniteSpec(duration: Int): InfiniteRepeatableSpec<Float> {
    return infiniteRepeatable(
        animation = tween(duration, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
}

private fun infiniteSequenceSpec(duration: Int): InfiniteRepeatableSpec<Float> {
    return infiniteRepeatable(
        animation = tween(duration, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
}
