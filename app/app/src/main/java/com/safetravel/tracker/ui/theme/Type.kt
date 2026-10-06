package com.safetravel.tracker.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.R

// =========================================================================
// 1. ENGLISH FONT FAMILY: SF PRO (Apple Design System)
// =========================================================================
val SfProFontFamily = FontFamily(
    Font(R.font.sf_pro_regular, FontWeight.Normal),
    Font(R.font.sf_pro_medium, FontWeight.Medium),
    Font(R.font.sf_pro_bold, FontWeight.SemiBold),
    Font(R.font.sf_pro_bold, FontWeight.Bold),
    Font(R.font.sf_pro_bold, FontWeight.ExtraBold),
    Font(R.font.sf_pro_bold, FontWeight.Black)
)

// Backward-compatibility aliases so existing references resolve seamlessly
val InterFontFamily = SfProFontFamily
val EnglishFontFamily = SfProFontFamily

// =========================================================================
// 2. BANGLA FONT FAMILY: HIND SILIGURI (Google Fonts)
// =========================================================================
val HindSiliguriFontFamily = FontFamily(
    Font(R.font.hind_siliguri_regular, FontWeight.Normal),
    Font(R.font.hind_siliguri_medium, FontWeight.Medium),
    Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Bold),
    Font(R.font.hind_siliguri_bold, FontWeight.ExtraBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Black)
)

// =========================================================================
// 3. SMART FONT DETECTION HELPERS
// =========================================================================
/**
 * Detects whether the input string contains any Bengali Unicode character (\u0980..\u09FF).
 */
fun isBangla(text: String?): Boolean {
    if (text == null) return false
    return text.any { it in '\u0980'..'\u09FF' }
}

/**
 * Returns [HindSiliguriFontFamily] if text contains Bengali characters,
 * otherwise returns [SfProFontFamily] for English.
 */
fun getAppFontFamily(text: String? = null): FontFamily {
    return if (isBangla(text)) HindSiliguriFontFamily else SfProFontFamily
}

@get:JvmName("resolveAppFontFamily")
val String?.appFontFamily: FontFamily
    get() = getAppFontFamily(this)

// =========================================================================
// 4. RESPONSIVE FONT SCALING HELPERS
// =========================================================================
/**
 * Responsive font calculator based on screen width.
 * Prevents text bloat on compact screens and scales gracefully on wide screens.
 */
@Composable
fun responsiveSp(baseSp: Float): TextUnit {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val scale = when {
        screenWidth < 360 -> 0.90f // Compact devices / split-screen
        screenWidth > 420 -> 1.05f // Large phones / foldables / tablets
        else -> 1.0f // Standard modern phones (360dp - 420dp)
    }
    return (baseSp * scale).sp
}

// =========================================================================
// 5. REFINED, SLEEK MATERIAL 3 TYPOGRAPHY (SF Pro as default)
// Compact and crisp typography to eliminate oversized/bloated appearance
// =========================================================================
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SfProFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 9.5.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.4.sp
    )
)

// =========================================================================
// 6. BANGLA TYPOGRAPHY (Hind Siliguri preset with proportional scaling)
// =========================================================================
val BanglaTypography = Typography(
    displayLarge = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 36.sp),
    displayMedium = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 30.sp),
    displaySmall = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 26.sp),
    headlineLarge = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp),
    headlineMedium = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    headlineSmall = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 21.sp),
    titleLarge = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp),
    titleMedium = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, lineHeight = 19.sp),
    titleSmall = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 17.sp),
    bodyLarge = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp),
    bodyMedium = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    bodySmall = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Normal, fontSize = 10.5.sp, lineHeight = 15.sp),
    labelLarge = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, lineHeight = 14.sp),
    labelSmall = TextStyle(fontFamily = HindSiliguriFontFamily, fontWeight = FontWeight.Medium, fontSize = 9.sp, lineHeight = 12.sp)
)

// =========================================================================
// 7. AUTO-SWITCHING & OPTICALLY BALANCED COMPOSE TEXT COMPOSABLE
// =========================================================================
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current
) {
    val hasBangla = isBangla(text)
    val effectiveFont = fontFamily ?: if (hasBangla) HindSiliguriFontFamily else SfProFontFamily

    // Optical size harmonization:
    // Hind Siliguri vertical glyph box is naturally ~10-15% taller than SF Pro.
    // If explicit fontSize is supplied, we normalize Bangla gently by ~0.92f so
    // English and Bangla look perfectly equal and sleek in visual height.
    val adjustedFontSize = if (fontSize != TextUnit.Unspecified && hasBangla) {
        (fontSize.value * 0.92f).sp
    } else {
        fontSize
    }

    val adjustedLineHeight = if (lineHeight != TextUnit.Unspecified) {
        lineHeight
    } else if (hasBangla && adjustedFontSize != TextUnit.Unspecified) {
        (adjustedFontSize.value * 1.35f).sp
    } else {
        TextUnit.Unspecified
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = adjustedFontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = effectiveFont,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = adjustedLineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style
    )
}
