package com.safetravel.tracker.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Clean, safe AdMob Banner View for Jetpack Compose.
 * Automatically checks:
 * 1. Database master ads switch
 * 2. Pro subscriber 100% ad-free exemption
 * 3. Banner ads enabled toggle
 */
@Composable
fun AdBannerView(
    modifier: Modifier = Modifier,
    isPremium: Boolean = false
) {
    val context = LocalContext.current
    val shouldShow = remember(isPremium) {
        AdMobManager.shouldShowAd(context, "banner", isPremium)
    }

    if (!shouldShow) return

    val unitId = remember { AdMobManager.getBannerUnitId(context) }
    var isAdFailed by remember { mutableStateOf(false) }

    if (isAdFailed) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.6f))
            .defaultMinSize(minHeight = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = unitId
                    this.adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d("AdBannerView", "AdMob banner successfully loaded: $unitId")
                            isAdFailed = false
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            Log.e("AdBannerView", "AdMob banner failed to load ($unitId): code=${loadAdError.code}, msg=${loadAdError.message}")
                            isAdFailed = true
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
