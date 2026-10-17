package com.safetravel.tracker.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for formatting all app dates and times based on user's timezone preference.
 * Default pattern across the entire app: "dd MMM yyyy hh:mm a" (e.g. "10 Oct 2026 10:30 AM").
 */
object DateTimeUtils {
    private const val TAG = "DateTimeUtils"
    private const val PREFS_NAME = "safe_travel_datetime_prefs"
    private const val KEY_TIMEZONE = "user_timezone_id"

    const val STANDARD_PATTERN = "dd MMM yyyy hh:mm a" // e.g. "10 Oct 2026 10:30 AM"

    // Default Bangladesh Standard Time is Asia/Dhaka (GMT+06:00)
    val BST_TIMEZONE: TimeZone = TimeZone.getTimeZone("GMT+06:00")
    private val UTC_TIMEZONE: TimeZone = TimeZone.getTimeZone("UTC")

    var userTimeZoneId: String = "Asia/Dhaka"
        private set

    private var _userTimeZone: TimeZone = TimeZone.getTimeZone("GMT+06:00")
    val userTimeZone: TimeZone
        get() = _userTimeZone

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedTz = prefs.getString(KEY_TIMEZONE, "Asia/Dhaka") ?: "Asia/Dhaka"
            setUserTimeZoneDirect(savedTz)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DateTimeUtils", e)
        }
    }

    fun setUserTimeZone(context: Context, timeZoneId: String) {
        setUserTimeZoneDirect(timeZoneId)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_TIMEZONE, timeZoneId).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save timezone preference", e)
        }
    }

    fun setUserTimeZoneDirect(timeZoneId: String) {
        userTimeZoneId = timeZoneId
        _userTimeZone = try {
            TimeZone.getTimeZone(timeZoneId)
        } catch (_: Exception) {
            BST_TIMEZONE
        }
    }

    /**
     * Parses an ISO timestamp (e.g. from Supabase "2026-09-18T17:35:00.000Z") as UTC
     * and converts it to a standard Date instance.
     */
    fun parseIsoToDate(isoTimestamp: String?): Date? {
        if (isoTimestamp.isNullOrBlank()) return null
        val trimmed = isoTimestamp.trim()

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = if (trimmed.endsWith("Z") || trimmed.contains("+00") || trimmed.contains("T")) UTC_TIMEZONE else _userTimeZone
                }
                val date = sdf.parse(trimmed)
                if (date != null) return date
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Formats an ISO timestamp or date using the user's selected TimeZone.
     * Default pattern: "dd MMM yyyy hh:mm a" (e.g. "10 Oct 2026 10:30 AM")
     */
    fun formatAppDateTime(isoTimestamp: String?, pattern: String = STANDARD_PATTERN): String {
        if (isoTimestamp.isNullOrBlank()) return ""
        val date = parseIsoToDate(isoTimestamp) ?: return isoTimestamp.take(16).replace("T", " ")
        val outSdf = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = _userTimeZone
        }
        return outSdf.format(date)
    }

    /**
     * Formats a Date object using user's selected TimeZone.
     */
    fun formatDate(date: Date?, pattern: String = STANDARD_PATTERN): String {
        if (date == null) return ""
        val outSdf = SimpleDateFormat(pattern, Locale.US).apply {
            timeZone = _userTimeZone
        }
        return outSdf.format(date)
    }

    /**
     * Backwards-compatible alias: all existing call sites calling formatBstDateTime
     * will automatically benefit from user-selected timezone and standard "10 Oct 2026 10:30 AM" formatting.
     */
    fun formatBstDateTime(isoTimestamp: String?, pattern: String = STANDARD_PATTERN): String {
        return formatAppDateTime(isoTimestamp, pattern)
    }

    /**
     * Formats an ISO timestamp as short date + time: e.g. "10 Oct, 10:30 AM"
     */
    fun formatBstShort(isoTimestamp: String?): String {
        return formatAppDateTime(isoTimestamp, "dd MMM, hh:mm a")
    }

    /**
     * Formats an ISO timestamp as date only: e.g. "10 Oct 2026"
     */
    fun formatBstDate(isoTimestamp: String?): String {
        return formatAppDateTime(isoTimestamp, "dd MMM yyyy")
    }

    /**
     * Formats an ISO timestamp as time only: e.g. "10:30 AM"
     */
    fun formatBstTime(isoTimestamp: String?): String {
        return formatAppDateTime(isoTimestamp, "hh:mm a")
    }

    /**
     * Formats as numeric 24-hour timestamp: e.g. "2026-10-10 10:30"
     */
    fun formatBstNumeric(isoTimestamp: String?): String {
        return formatAppDateTime(isoTimestamp, "yyyy-MM-dd HH:mm")
    }
}
