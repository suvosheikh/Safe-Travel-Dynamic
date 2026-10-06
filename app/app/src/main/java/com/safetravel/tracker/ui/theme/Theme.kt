package com.safetravel.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safetravel.tracker.ui.components.GlassColors

@Composable
fun SafeTravelTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Color(0xFF3B82F6), // Accent Blue
        background = GlassColors.BaseIvory, // Warm Cream
        surface = Color.White.copy(alpha = 0.65f), // Glassmorphic translucent surface
        onBackground = GlassColors.TextPrimary, // Dark readable text
        onSurface = GlassColors.TextPrimary, // Dark readable text
        error = Color(0xFFEF4444),
        onPrimary = Color.White
    )
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
