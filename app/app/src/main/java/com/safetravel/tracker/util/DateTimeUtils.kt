package com.safetravel.tracker.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for formatting all app dates and times in Bangladesh Standard Time (BST / UTC+06:00).
 */
object DateTimeUtils {
    // Bangladesh Standard Time is permanently GMT+06:00 (UTC+6)
    val BST_TIMEZONE: TimeZone = TimeZone.getTimeZone("GMT+06:00")
    private val UTC_TIMEZONE: TimeZone = TimeZone.getTimeZone("UTC")

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
                    timeZone = if (trimmed.endsWith("Z") || trimmed.contains("+00") || trimmed.contains("T")) UTC_TIMEZONE else BST_TIMEZONE
                }
                val date = sdf.parse(trimmed)
                if (date != null) return date
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Formats an ISO timestamp in BST (UTC+6) with a custom pattern.
     * Default: "dd MMM yyyy, hh:mm a" (e.g. "18 Sep 2026, 11:35 PM")
     */
    fun formatBstDateTime(isoTimestamp: String?, pattern: String = "dd MMM yyyy, hh:mm a"): String {
        if (isoTimestamp.isNullOrBlank()) return ""
        val date = parseIsoToDate(isoTimestamp) ?: return isoTimestamp.take(16).replace("T", " ")
        val outSdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = BST_TIMEZONE
        }
        return outSdf.format(date)
    }

    /**
     * Formats an ISO timestamp as short date + time in BST: e.g. "18 Sep, 11:35 PM"
     */
    fun formatBstShort(isoTimestamp: String?): String {
        return formatBstDateTime(isoTimestamp, "dd MMM, hh:mm a")
    }

    /**
     * Formats an ISO timestamp as date only in BST: e.g. "18 Sep 2026"
     */
    fun formatBstDate(isoTimestamp: String?): String {
        return formatBstDateTime(isoTimestamp, "dd MMM yyyy")
    }

    /**
     * Formats an ISO timestamp as time only in BST: e.g. "11:35 PM"
     */
    fun formatBstTime(isoTimestamp: String?): String {
        return formatBstDateTime(isoTimestamp, "hh:mm a")
    }

    /**
     * Formats as numeric 24-hour timestamp in BST: e.g. "2026-09-18 23:35"
     */
    fun formatBstNumeric(isoTimestamp: String?): String {
        return formatBstDateTime(isoTimestamp, "yyyy-MM-dd HH:mm")
    }
}
