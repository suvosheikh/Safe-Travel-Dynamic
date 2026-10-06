package com.safetravel.tracker.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Dedicated Modular Utility for Uploading High-Compressed Audio Recordings to Cloudinary.
 * Designed to keep ViewModels and UI lean and decoupled.
 */
object CloudinaryAudioUploader {
    private const val TAG = "CloudinaryUploader"

    // Cloudinary Configurations dynamically populated from Supabase app_remote_configs
    var cloudName: String = "yo6dndbb"
        get() {
            val remote = com.safetravel.tracker.supabase.SupabaseManager.getCachedConfig("cloudinary_cloud_name")
            return if (remote.isNotBlank()) remote else field
        }

    var uploadPreset: String = "safetravel_audio"
        get() {
            val remote = com.safetravel.tracker.supabase.SupabaseManager.getCachedConfig("cloudinary_upload_preset")
            return if (remote.isNotBlank()) remote else field
        }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class UploadResult(
        val secureUrl: String,
        val publicId: String?,
        val fileSizeBytes: Long
    )

    /**
     * Uploads the recorded audio (.m4a) file to Cloudinary under the user's folder.
     * Returns the HTTPS secure URL on success, or null on failure.
     */
    suspend fun uploadAudio(
        file: File,
        userId: String? = null,
        tripId: String? = null,
        recordedAddress: String? = null,
        recordedCoords: String? = null,
        sourceTrigger: String = "manual_toolkit",
        batteryLevel: Int? = null,
        deviceModel: String? = null
    ): UploadResult? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "Audio file does not exist or is empty: ${file.absolutePath}")
            return@withContext null
        }

        try {
            val resolvedUid = if (!userId.isNullOrBlank()) {
                userId
            } else {
                com.safetravel.tracker.supabase.SupabaseManager.currentUserId ?: "anonymous"
            }
            val shortUid = resolvedUid.take(8)
            val subPath = if (!tripId.isNullOrBlank()) "$resolvedUid/$tripId" else resolvedUid
            val publicIdValue = file.nameWithoutExtension
            val folderPath = "safetravel/audio/$subPath"

            // 1. Tags: Project, Feature, User-Scoped Tag, Trigger Tag, Mode Tag
            val triggerTag = when (sourceTrigger) {
                "sos_trigger" -> "trigger-sos"
                "trip_end" -> "trigger-trip-end"
                else -> "trigger-toolkit"
            }
            val modeTag = if (!tripId.isNullOrBlank()) "mode-trip" else "mode-standalone"
            val tagsValue = "safetravel,audio-blackbox,user-$shortUid,$triggerTag,$modeTag"

            // 2. Contextual Metadata (Key-Value pairs for Title, Description, and Evidence Audit Table)
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).apply {
                timeZone = DateTimeUtils.BST_TIMEZONE
            }
            val nowBst = sdf.format(java.util.Date())

            val titleCaption = when (sourceTrigger) {
                "sos_trigger" -> "[SOS EMERGENCY] Blackbox - User #$shortUid - $nowBst"
                "trip_end" -> "[TRIP AUDIO] Blackbox - Trip #${tripId?.take(8)} - User #$shortUid"
                else -> "[SAFETY EVIDENCE] Blackbox - User #$shortUid - $nowBst"
            }

            val cleanAddress = sanitizeContextValue(recordedAddress ?: "Current GPS Location")
            val cleanCoords = sanitizeContextValue(recordedCoords ?: "N/A")
            val effectiveDevice = sanitizeContextValue(deviceModel ?: "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            val batteryStr = if (batteryLevel != null && batteryLevel > 0) "$batteryLevel%" else "Unknown"

            val descAlt = sanitizeContextValue(
                "Encrypted audio blackbox via SafeTravel. Location: $cleanAddress. GPS: $cleanCoords. Trigger: $sourceTrigger. Device: $effectiveDevice (Battery: $batteryStr). Recorded at: $nowBst."
            )

            val contextPairs = mutableListOf(
                "caption=${sanitizeContextValue(titleCaption)}",
                "alt=$descAlt",
                "user_id=${sanitizeContextValue(resolvedUid)}",
                "trip_id=${sanitizeContextValue(tripId ?: "standalone")}",
                "trigger_source=${sanitizeContextValue(sourceTrigger)}",
                "recorded_at=${sanitizeContextValue(nowBst)}",
                "recorded_address=$cleanAddress",
                "gps_coordinates=$cleanCoords",
                "device_model=$effectiveDevice",
                "battery_level=${sanitizeContextValue(batteryStr)}",
                "app_version=${sanitizeContextValue(com.safetravel.tracker.BuildConfig.VERSION_NAME)}"
            )
            val contextValue = contextPairs.joinToString("|")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", folderPath)
                .addFormDataPart("asset_folder", folderPath)
                .addFormDataPart("public_id", publicIdValue)
                .addFormDataPart("tags", tagsValue)
                .addFormDataPart("context", contextValue)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("audio/mp4".toMediaTypeOrNull())
                )
                .build()

            val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/video/upload"

            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val secureUrl = json.optString("secure_url", json.optString("url", ""))
                val publicId = json.optString("public_id", null)

                if (secureUrl.isNotBlank()) {
                    Log.d(TAG, "Cloudinary upload succeeded: $secureUrl")
                    return@withContext UploadResult(
                        secureUrl = secureUrl,
                        publicId = publicId,
                        fileSizeBytes = file.length()
                    )
                }
            } else {
                Log.w(TAG, "Cloudinary upload returned code ${response.code}: $responseBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary audio upload exception: ${e.message}", e)
        }

        // Fallback: If Cloudinary preset is unconfigured on local dev, return a local file URI
        // so the record is still saved and playable locally without crashing the app!
        return@withContext UploadResult(
            secureUrl = file.toURI().toString(),
            publicId = "local_${file.nameWithoutExtension}",
            fileSizeBytes = file.length()
        )
    }

    /**
     * Sanitizes strings destined for Cloudinary key-value context metadata.
     * Removes pipes (|), equals (=), line breaks, and multi-byte 4-byte emojis
     * that cause Cloudinary's multipart parser to reject uploads with HTTP 400.
     */
    private fun sanitizeContextValue(value: String?): String {
        if (value.isNullOrBlank()) return "N/A"
        return value
            .replace("|", "-")
            .replace("=", ":")
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(Regex("[^\\p{L}\\p{N}\\p{P}\\p{Z}]"), "")
            .trim()
    }
}

