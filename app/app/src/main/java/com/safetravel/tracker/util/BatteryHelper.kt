package com.safetravel.tracker.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Dedicated Hardware Battery Telemetry Utility for SafeTravel.
 * Non-blocking, ultra-fast reading of device battery capacity directly from Android hardware sensor.
 */
object BatteryHelper {

    fun getCurrentBatteryLevel(context: Context?): Int {
        if (context == null) return 100
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val capacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (capacity in 0..100) {
                return capacity
            }

            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                (level * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
            } else {
                100
            }
        } catch (e: Exception) {
            100
        }
    }
}
