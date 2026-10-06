package com.safetravel.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Glassmorphic Premium Dark/Neon Colors (Matching Weather Design Mockup!)
object GlassColors {
    val BaseIvory = Color(0xFF080710) // Rich deep midnight space indigo/black base
    val GlowMint = Color(0xFFEC4899) // Vibrant neon magenta/pink glow
    val GlowPeach = Color(0xFF8B5CF6) // Vibrant royal violet/purple glow
    val GlowAzure = Color(0xFF2563EB) // Electric vibrant blue glow
    
    // Card colors - translucent dark-glass with white/magenta neon highlight borders!
    val CardBackground = Color(0x1F111827) // ~12% Opacity Dark Slate Glass - extremely premium!
    val CardBackgroundSelected = Color(0x361E293B) // ~21% Opacity selected dark glass
    val BorderLight = Color(0x40FFFFFF) // Polished white edge glow highlighting top-left
    val BorderDark = Color(0x1A8B5CF6) // Translucent violet edge glow highlighting bottom-right
    
    // Text colors - Crisp pure whites and soft silvers
    val TextPrimary = Color(0xFFFFFFFF) // High-contrast crisp white text
    val TextSecondary = Color(0xFFCBD5E1) // Soft silver-grey text
    val TextMuted = Color(0xFF94A3B8) // Muted slate-grey text
    
    // Accent gradients
    val PrimaryOrange = Color(0xFFD946EF) // Magenta/Pink accent
    val PrimaryTeal = Color(0xFF3B82F6) // Royal blue accent
    
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFD946EF), Color(0xFF3B82F6))
    )
    val AccentGreenBg = Color(0x2A10B981)
    val AccentGreenText = Color(0xFF34D399)
}

/**
 * A beautiful animated/static ambient pastel gradient background that perfectly mimics
 * the soft organic glows of the reference image.
 */
@Composable
fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // We animate the position of the radial glows slightly to give an organic, high-end feel
    val infiniteTransition = rememberInfiniteTransition(label = "backgroundGlow")
    
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height

                // 1. Draw rich base diagonal linear gradient (deep midnight space obsidian!)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F0C24), // Ultra deep space midnight indigo (top-left)
                            Color(0xFF070514), // Deepest obsidian black (center)
                            Color(0xFF0B091B)  // Dark deep violet (bottom-right)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(width, height)
                    )
                )

                // 2. Glow 1: Top Left (Vibrant Magenta/Pink) - pulsing ambient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassColors.GlowMint.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(width * 0.1f, height * 0.2f),
                        radius = (width * 0.95f) * pulseAnim
                    ),
                    radius = (width * 0.95f) * pulseAnim,
                    center = Offset(width * 0.1f, height * 0.2f)
                )

                // 3. Glow 2: Center Right (Electric Royal Blue) - vibrant pulsing glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassColors.GlowAzure.copy(alpha = 0.38f), Color.Transparent),
                        center = Offset(width * 0.85f, height * 0.45f),
                        radius = (width * 0.9f) * (2f - pulseAnim)
                    ),
                    radius = (width * 0.9f) * (2f - pulseAnim),
                    center = Offset(width * 0.85f, height * 0.45f)
                )

                // 4. Glow 3: Bottom Left (Royal Purple/Violet) - pulsing accent
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassColors.GlowPeach.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(width * 0.2f, height * 0.8f),
                        radius = (width * 0.8f) * pulseAnim
                    ),
                    radius = (width * 0.8f) * pulseAnim,
                    center = Offset(width * 0.2f, height * 0.8f)
                )

                // 5. Glow 4: Bottom Right (Subtle Neon Magenta Accent) - pulsing accent
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlassColors.GlowMint.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(width * 0.8f, height * 0.9f),
                        radius = (width * 0.6f) * (2f - pulseAnim)
                    ),
                    radius = (width * 0.6f) * (2f - pulseAnim),
                    center = Offset(width * 0.8f, height * 0.9f)
                )
            }
    ) {
        content()
    }
}

/**
 * A modifier to easily apply premium glassmorphic style to any container.
 */
fun Modifier.glassmorphic(
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.2.dp, // Fine, precise border for maximum elegance
    shadowElevation: Dp = 8.dp // Floating depth
): Modifier {
    val base = this
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            clip = false,
            ambientColor = Color(0xFF02020A).copy(alpha = 0.35f), // Rich dark diffused shadow
            spotColor = Color(0xFF02020A).copy(alpha = 0.45f)
        )
        .background(
            color = GlassColors.CardBackground,
            shape = shape
        )
    
    return if (borderWidth > 0.dp) {
        base.border(
            width = borderWidth,
            brush = Brush.linearGradient(
                colors = listOf(
                    GlassColors.BorderLight, // Brilliant top-left reflecting light
                    GlassColors.BorderDark   // Translucent bottom-right fading out
                )
            ),
            shape = shape
        ).clip(shape)
    } else {
        base.clip(shape)
    }
}

/**
 * A custom reusable Glass Card with built-in styling, perfectly matching the reference image.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val innerModifier = if (onClick != null) {
        modifier
            .glassmorphic(shape = shape)
            .background(GlassColors.CardBackground)
    } else {
        modifier.glassmorphic(shape = shape)
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = innerModifier
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    } else {
        Column(
            modifier = innerModifier.padding(16.dp)
        ) {
            content()
        }
    }
}

/**
 * Reusable Glassmorphism Container for Bottom Sheets across the app.
 * Provides luxury cybernetic frosted dark glass, luminous border stroke, ambient glows, and clean edge-to-edge support.
 * Caps maximum sheet height at [maxHeightFraction] (default 70% of screen height, leaving ~30% gap at top)
 * and smoothly scrolls internal content without expanding past the top boundary.
 */
@Composable
fun GlassBottomSheetContainer(
    modifier: Modifier = Modifier,
    topStartRadius: Dp = 32.dp,
    topEndRadius: Dp = 32.dp,
    maxHeightFraction: Float = 0.70f,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxSheetHeight = (configuration.screenHeightDp * maxHeightFraction).dp
    val sheetShape = RoundedCornerShape(topStart = topStartRadius, topEnd = topEndRadius)
    val contentScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .clip(sheetShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD90F172A), // ~85% Translucent Frosted Slate (blurred underlying visibility)
                        Color(0xE60B0F19), // ~90% Cyber Midnight
                        Color(0xF0060911)  // Deep Navy base
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x9938BDF8), // Glowing Cyan/White top rim highlight
                        Color(0x408B5CF6), // Violet fade
                        Color(0x20334155)  // Subtle base
                    )
                ),
                shape = sheetShape
            )
            .drawBehind {
                // Soft luminous ambient backlight for high-tech glass effect (Cyan/Blue instead of green)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2238BDF8), Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.4f
                    ),
                    radius = size.width * 0.4f,
                    center = Offset(size.width * 0.5f, 0f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cyan/Blue Cyber Drag Handle (Sticky/pinned at top of sheet)
            GlassDragHandle()

            if (scrollable) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(contentScrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}

@Composable
fun GlassDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(vertical = 12.dp)
            .width(48.dp)
            .height(4.5.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x6638BDF8),
                        Color(0xFF38BDF8),
                        Color(0x6638BDF8)
                    )
                )
            )
    )
}

@Composable
fun DetailHeaderBanner(
    imageUrl: String?,
    title: String,
    badgeText: String = "VERIFIED SECURE",
    icon: ImageVector = Icons.Default.Shield,
    accentColor: Color = Color(0xFF38BDF8),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xAA0A0F1D))
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF090D16)
                            )
                        )
                    )
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
                                center = Offset(size.width * 0.5f, size.height * 0.5f),
                                radius = size.width * 0.5f
                            ),
                            radius = size.width * 0.5f,
                            center = Offset(size.width * 0.5f, size.height * 0.5f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = badgeText,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

