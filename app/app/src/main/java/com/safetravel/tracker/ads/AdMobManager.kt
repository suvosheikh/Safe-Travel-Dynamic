package com.safetravel.tracker.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.safetravel.tracker.supabase.SupabaseManager
import java.util.concurrent.atomic.AtomicBoolean

object AdMobManager {
    private const val TAG = "AdMobManager"
    private val isInitialized = AtomicBoolean(false)

    // Google Official Test Ad Unit IDs (used as rock-solid fallbacks)
    const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    /**
     * Initializes Google Mobile Ads SDK safely in the background.
     */
    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                MobileAds.initialize(context.applicationContext) { initStatus ->
                    Log.d(TAG, "AdMob MobileAds initialized successfully: ${initStatus.adapterStatusMap.keys}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "AdMob initialization error: ${e.message}")
            }
        }
    }

    /**
     * Determines whether an ad format should be rendered, honoring:
     * 1. Global master ads switch
     * 2. Pro subscriber 100% ad-free exemption
     * 3. Specific format toggle in Supabase app_remote_configs
     */
    fun shouldShowAd(context: Context? = null, format: String, isPremium: Boolean): Boolean {
        val masterSwitch = SupabaseManager.getRemoteConfig("ads_master_switch", "true").equals("true", ignoreCase = true)
        if (!masterSwitch) return false

        val nonProOnly = SupabaseManager.getRemoteConfig("ads_non_pro_only", "true").equals("true", ignoreCase = true)
        if (nonProOnly && isPremium) return false

        return when (format.lowercase()) {
            "banner" -> SupabaseManager.getRemoteConfig("ads_banner_enabled", "true").equals("true", ignoreCase = true)
            "directory_card" -> SupabaseManager.getRemoteConfig("ads_directory_card_enabled", "true").equals("true", ignoreCase = true)
            "history_card" -> SupabaseManager.getRemoteConfig("ads_history_card_enabled", "true").equals("true", ignoreCase = true)
            "alert_card" -> SupabaseManager.getRemoteConfig("ads_alert_card_enabled", "true").equals("true", ignoreCase = true)
            "rewarded" -> SupabaseManager.getRemoteConfig("ads_rewarded_enabled", "true").equals("true", ignoreCase = true)
            "interstitial" -> SupabaseManager.getRemoteConfig("ads_interstitial_enabled", "false").equals("true", ignoreCase = true)
            else -> false
        }
    }

    fun getDirectoryCardInterval(context: Context? = null): Int {
        val str = SupabaseManager.getRemoteConfig("ads_directory_card_interval", "4")
        return (str.toIntOrNull() ?: 4).coerceAtLeast(1)
    }

    fun getHistoryCardInterval(context: Context? = null): Int {
        val str = SupabaseManager.getRemoteConfig("ads_history_card_interval", "3")
        return (str.toIntOrNull() ?: 3).coerceAtLeast(1)
    }

    fun getHistoryCardSize(context: Context? = null): String {
        val str = SupabaseManager.getRemoteConfig("ads_history_card_size", "medium_rectangle")
        return if (str.isBlank()) "medium_rectangle" else str
    }

    fun getAlertCardInterval(context: Context? = null): Int {
        val str = SupabaseManager.getRemoteConfig("ads_alert_card_interval", "3")
        return (str.toIntOrNull() ?: 3).coerceAtLeast(1)
    }

    fun getAlertCardSize(context: Context? = null): String {
        val str = SupabaseManager.getRemoteConfig("ads_alert_card_size", "large_banner")
        return if (str.isBlank()) "large_banner" else str
    }

    fun findActivity(context: Context?): Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun getBannerUnitId(context: Context? = null): String {
        val id = SupabaseManager.getRemoteConfig("admob_banner_unit_id", TEST_BANNER_UNIT_ID)
        return if (id.isBlank()) TEST_BANNER_UNIT_ID else id
    }

    fun getRewardedUnitId(context: Context? = null): String {
        val id = SupabaseManager.getRemoteConfig("admob_rewarded_unit_id", TEST_REWARDED_UNIT_ID)
        return if (id.isBlank()) TEST_REWARDED_UNIT_ID else id
    }

    fun getRewardCreditsAmount(context: Context? = null): Int {
        val str = SupabaseManager.getRemoteConfig("ads_reward_credits_amount", "1")
        return str.toIntOrNull() ?: 1
    }

    fun getInterstitialUnitId(context: Context? = null): String {
        val id = SupabaseManager.getRemoteConfig("admob_interstitial_unit_id", TEST_INTERSTITIAL_UNIT_ID)
        return if (id.isBlank()) TEST_INTERSTITIAL_UNIT_ID else id
    }

    /**
     * Loads and displays a Rewarded Video Ad.
     * When completed, awards credits to the user.
     */
    fun loadAndShowRewardedAd(
        activity: Activity,
        onLoading: () -> Unit = {},
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onFailedOrDismissed: (reason: String?) -> Unit
    ) {
        val unitId = getRewardedUnitId(activity)
        val rewardCredits = getRewardCreditsAmount(activity)
        onLoading()

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            unitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    var rewardGranted = false

                    rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            if (!rewardGranted) {
                                onFailedOrDismissed("dismissed_early")
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                            onFailedOrDismissed(adError.message)
                        }
                    }

                    rewardedAd.show(activity) { _ ->
                        rewardGranted = true
                        onRewardEarned(rewardCredits)
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    onFailedOrDismissed(loadAdError.message)
                }
            }
        )
    }

    /**
     * Loads and displays an Interstitial Ad (only for safe post-trip summary, never active emergency).
     */
    fun loadAndShowInterstitialAd(
        activity: Activity,
        isPremium: Boolean,
        onDismissed: () -> Unit
    ) {
        if (!shouldShowAd(activity, "interstitial", isPremium)) {
            onDismissed()
            return
        }

        val unitId = getInterstitialUnitId(activity)
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            unitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            onDismissed()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                            onDismissed()
                        }
                    }
                    interstitialAd.show(activity)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${loadAdError.message}")
                    onDismissed()
                }
            }
        )
    }
}
