package com.safetravel.tracker.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.safetravel.tracker.ui.theme.HindSiliguriFontFamily

/**
 * In-feed Directory Card Ad component.
 * Blends naturally into directory lists (Thana, Hospitals, Fire Station, Blood Banks)
 * without blocking emergency UI or user navigation.
 */
@Composable
fun AdCardView(
    modifier: Modifier = Modifier,
    isPremium: Boolean = false,
    formatKey: String = "directory_card",
    sizeType: String = "banner"
) {
    val context = LocalContext.current
    val shouldShow = remember(isPremium, formatKey) {
        AdMobManager.shouldShowAd(context, formatKey, isPremium)
    }

    if (!shouldShow) return

    val unitId = remember { AdMobManager.getBannerUnitId(context) }
    var isAdFailed by remember { mutableStateOf(false) }

    if (isAdFailed) return

    val targetAdSize = remember(sizeType) {
        when (sizeType.lowercase()) {
            "medium_rectangle", "mrec", "large" -> AdSize.MEDIUM_RECTANGLE
            "large_banner", "medium" -> AdSize.LARGE_BANNER
            else -> AdSize.BANNER
        }
    }

    val minContainerHeight = remember(targetAdSize) {
        when (targetAdSize) {
            AdSize.MEDIUM_RECTANGLE -> 250.dp
            AdSize.LARGE_BANNER -> 100.dp
            else -> 50.dp
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Label & Ad Tag
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Sponsored",
                        tint = Color(0xFF06B6D4),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "স্পন্সর বিজ্ঞাপন",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF334155).copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ad",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // Embedded AdMob Banner View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minContainerHeight),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(targetAdSize)
                            this.adUnitId = unitId
                            this.adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    Log.d("AdCardView", "AdCard loaded ($formatKey, $sizeType): $unitId")
                                    isAdFailed = false
                                }

                                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                    Log.e("AdCardView", "AdCard failed ($formatKey, $unitId): code=${loadAdError.code}, msg=${loadAdError.message}")
                                    isAdFailed = true
                                }
                            }
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }
        }
    }
}
