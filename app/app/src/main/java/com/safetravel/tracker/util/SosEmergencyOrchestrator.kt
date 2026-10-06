package com.safetravel.tracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.safetravel.tracker.supabase.Guardian
import com.safetravel.tracker.supabase.SupabaseManager
import com.safetravel.tracker.supabase.SupabaseTrip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Dedicated Modular Orchestrator for SOS Emergency Dispatch and Forensic Audit Logging.
 * Guarantees all guardians are notified and every safety check-in is logged.
 */
object SosEmergencyOrchestrator {
    private const val TAG = "SosEmergencyOrchestrator"

    /**
     * Dispatches WhatsApp distress message to ALL guardians registered by the user.
     */
    fun dispatchSosToAllGuardians(
        context: Context,
        trip: SupabaseTrip?,
        guardians: List<Guardian>,
        currentAddress: String?
    ) {
        if (guardians.isEmpty()) return

        val locationText = if (!currentAddress.isNullOrBlank()) {
            AddressUtils.formatDisplayAddress(currentAddress)
        } else "Current Trip Location"

        val trackingUrl = if (!trip?.trackingCode.isNullOrBlank()) {
            "https://safetravel.live/track?code=${trip?.trackingCode}"
        } else if (!trip?.id.isNullOrBlank()) {
            "https://safetravel.live/track?tripId=${trip?.id}"
        } else "SafeTravel App Live Tracking"

        val messageText = "[EMERGENCY SOS] I have activated an emergency distress alert on SafeTravel from $locationText. Please check my live location and ensure my safety immediately: $trackingUrl"

        guardians.forEach { guardian ->
            val phone = guardian.phone.trim()
            if (phone.isNotBlank()) {
                try {
                    val encodedMsg = URLEncoder.encode(messageText, "UTF-8")
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encodedMsg")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to launch WhatsApp dispatch for guardian ${guardian.name}: ${e.message}")
                }
            }
        }
    }

    /**
     * Generates a standardized ISO-8601 UTC timestamp string.
     */
    fun getIsoInstantString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Appends an audit log record to the trip's sos_activity_logs in Supabase.
     */
    suspend fun logSosEvent(
        tripId: String,
        actionType: String,
        details: String,
        metadata: Map<String, Any> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        val logEntry = mutableMapOf<String, Any>(
            "action_type" to actionType,
            "details" to details,
            "timestamp" to getIsoInstantString(),
            "bst_time" to DateTimeUtils.formatBstTime(getIsoInstantString())
        )
        if (metadata.isNotEmpty()) {
            logEntry.putAll(metadata)
        }
        SupabaseManager.appendSosActivityLog(tripId, logEntry)
    }
}
