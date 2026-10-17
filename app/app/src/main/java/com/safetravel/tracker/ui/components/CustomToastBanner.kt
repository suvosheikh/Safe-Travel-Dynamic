package com.safetravel.tracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.ui.theme.*
import kotlinx.coroutines.delay

enum class ToastType(
    val defaultTitle: String,
    val color: Color,
    val icon: ImageVector
) {
    SUCCESS(
        defaultTitle = "Success",
        color = Emerald500,
        icon = Icons.Outlined.CheckCircle
    ),
    WARNING(
        defaultTitle = "Connection Alert",
        color = Color(0xFFF59E0B),
        icon = Icons.Outlined.WarningAmber
    ),
    INFO(
        defaultTitle = "Notice",
        color = Cyan500,
        icon = Icons.Outlined.Info
    ),
    ERROR(
        defaultTitle = "Something went wrong",
        color = Red500,
        icon = Icons.Outlined.ErrorOutline
    )
}

data class ToastBannerData(
    val message: String,
    val title: String? = null,
    val type: ToastType = ToastType.INFO,
    val durationMs: Long = 3500L,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Frosted Glass Pill Capsule Toast Banner.
 * Matches the frosted glass aesthetic, gradient depth, and border lighting of the bottom bar:
 * - Rounded capsule container (RoundedCornerShape(26.dp))
 * - Vertical frosted glass gradient: Color(0xCC0F172A) to Color(0xD9090D16)
 * - Subtle status-tinted horizontal accent glow
 * - Top-down luminous reflective border gradient
 * - High-contrast shadow (elevation = 18.dp, spotColor = Black 70%)
 * - Outlined circular icon badge with soft translucent backing
 * - Crisp bold title, readable secondary message, and minimal dismiss 'X'
 */
@Composable
fun CustomToastBanner(
    data: ToastBannerData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveTitle = data.title ?: data.type.defaultTitle
    val capsuleShape = RoundedCornerShape(26.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 18.dp,
                shape = capsuleShape,
                ambientColor = data.type.color.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.70f)
            )
            .clip(capsuleShape)
            // Frosted Glass Vertical Gradient Layer (matching Bottom Bar)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xCC0F172A),
                        Color(0xD9090D16)
                    )
                )
            )
            // Subtle status-tinted accent wash
            .background(
                Brush.horizontalGradient(
                    listOf(
                        data.type.color.copy(alpha = 0.16f),
                        Color.Transparent,
                        data.type.color.copy(alpha = 0.08f)
                    )
                )
            )
            // Luminous Reflective Border (matching Bottom Bar)
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        data.type.color.copy(alpha = 0.60f),
                        data.type.color.copy(alpha = 0.20f),
                        Color(0x1AFFFFFF)
                    )
                ),
                shape = capsuleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Frosted Circular Outline Icon Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(data.type.color.copy(alpha = 0.16f))
                    .border(
                        width = 1.2.dp,
                        color = data.type.color.copy(alpha = 0.45f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.type.icon,
                    contentDescription = null,
                    tint = data.type.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content: Title and descriptive message
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = effectiveTitle,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = data.message,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Dismiss Button (Right side)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss notification",
                    tint = data.type.color.copy(alpha = 0.75f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Animated Host for the CustomToastBanner.
 * Automatically slides in from bottom, floats gracefully right above the bottom navigation bar,
 * auto-dismisses after duration, and slides back down on dismiss.
 */
@Composable
fun CustomToastHost(
    toastData: ToastBannerData?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(toastData) {
        if (toastData != null) {
            delay(toastData.durationMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = toastData != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(220)
        ) + fadeOut(animationSpec = tween(180)),
        modifier = modifier
    ) {
        if (toastData != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 84.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                CustomToastBanner(
                    data = toastData,
                    onDismiss = onDismiss
                )
            }
        }
    }
}
