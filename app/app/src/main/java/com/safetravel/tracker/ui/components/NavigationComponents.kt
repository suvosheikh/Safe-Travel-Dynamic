package com.safetravel.tracker.ui.components
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.ui.components.GlassColors

@Composable
fun SidebarSectionHeader(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF06B6D4))
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun SidebarMenuItem(
    icon: ImageVector,
    iconTint: Color = Color(0xFF06B6D4),
    label: String,
    subtitle: String? = null,
    badgeCount: Int? = null,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF10B981),
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val effectiveBadge = badgeText ?: badgeCount?.takeIf { it > 0 }?.toString()
    val bgAnim by animateColorAsState(
        targetValue = if (isSelected) Color(0x1F06B6D4) else Color.Transparent,
        label = "menuBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgAnim)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box with translucent glowing background
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) iconTint.copy(alpha = 0.22f) else iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFFE2E8F0)
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 13.sp,
                    maxLines = 1
                )
            }
        }

        if (!effectiveBadge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = effectiveBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isSelected) iconTint.copy(alpha = 0.8f) else Color(0x33FFFFFF),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
fun BarItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selectedColor: Color = Color(0xFF06B6D4),
    unselectedColor: Color = Color(0xFF94A3B8),
    hasBadge: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillBg by animateColorAsState(
        targetValue = if (selected) selectedColor.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "pillBg"
    )
    val animatedIconTint by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        animationSpec = tween(durationMillis = 200),
        label = "iconTint"
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 46.dp else 32.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "pillWidth"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedIconTint,
                modifier = Modifier.size(20.dp)
            )

            if (hasBadge && !selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-3).dp, y = 2.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = animatedIconTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

