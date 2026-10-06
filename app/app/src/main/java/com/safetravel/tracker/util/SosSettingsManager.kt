package com.safetravel.tracker.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Dedicated Modular Manager for SOS Configuration and Timing Settings.
 * Local-First storage ensures zero-latency offline response during emergencies.
 */
object SosSettingsManager {
    private const val PREFS_NAME = "safetravel_sos_prefs"
    private const val KEY_HOLD_DURATION_SEC = "key_hold_duration_sec"
    private const val KEY_GRACE_COUNTDOWN_SEC = "key_grace_countdown_sec"
    private const val KEY_SAFETY_CHECK_INTERVAL_MIN = "key_safety_check_interval_min"
    private const val KEY_PRO_DISPATCH_MODE = "key_pro_dispatch_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getHoldDurationSec(context: Context): Int {
        return getPrefs(context).getInt(KEY_HOLD_DURATION_SEC, 2)
    }

    fun setHoldDurationSec(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_HOLD_DURATION_SEC, seconds.coerceIn(0, 10)).apply()
    }

    fun getGraceCountdownSec(context: Context): Int {
        return getPrefs(context).getInt(KEY_GRACE_COUNTDOWN_SEC, 3)
    }

    fun setGraceCountdownSec(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_GRACE_COUNTDOWN_SEC, seconds.coerceIn(1, 10)).apply()
    }

    fun getSafetyCheckIntervalMin(context: Context): Int {
        return getPrefs(context).getInt(KEY_SAFETY_CHECK_INTERVAL_MIN, 2)
    }

    fun setSafetyCheckIntervalMin(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_SAFETY_CHECK_INTERVAL_MIN, minutes.coerceIn(1, 15)).apply()
    }

    fun getProDispatchMode(context: Context): String {
        return getPrefs(context).getString(KEY_PRO_DISPATCH_MODE, "sms") ?: "sms"
    }

    fun setProDispatchMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_PRO_DISPATCH_MODE, mode).apply()
    }

    fun toMap(context: Context): Map<String, Any> {
        return mapOf(
            "hold_duration_sec" to getHoldDurationSec(context),
            "grace_countdown_sec" to getGraceCountdownSec(context),
            "safety_check_interval_min" to getSafetyCheckIntervalMin(context),
            "pro_dispatch_mode" to getProDispatchMode(context)
        )
    }

    fun syncFromMap(context: Context, map: Map<String, Any>?) {
        if (map == null) return
        (map["hold_duration_sec"] as? Number)?.let { setHoldDurationSec(context, it.toInt()) }
        (map["grace_countdown_sec"] as? Number)?.let { setGraceCountdownSec(context, it.toInt()) }
        (map["safety_check_interval_min"] as? Number)?.let { setSafetyCheckIntervalMin(context, it.toInt()) }
        (map["pro_dispatch_mode"] as? String)?.let { setProDispatchMode(context, it) }
    }
}
