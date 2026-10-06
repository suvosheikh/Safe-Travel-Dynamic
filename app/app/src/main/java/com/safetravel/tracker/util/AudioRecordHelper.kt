package com.safetravel.tracker.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Dedicated Modular Utility for High-Compressed Silent Audio Blackbox Recording.
 * Optimizes voice compression (AAC 32kbps / 22.05kHz) to achieve crystal-clear speech
 * at only ~240 KB per minute, with dual storage (device storage + Cloudinary sync).
 */
object AudioRecordHelper {
    private const val TAG = "AudioRecordHelper"
    private var mediaRecorder: MediaRecorder? = null

    var isRecording: Boolean = false
        private set

    var currentRecordingFile: File? = null
        private set

    var recordingStartTimeMs: Long = 0L
        private set

    var activeTripIdForRecording: String? = null
        private set

    fun getRecordingDurationSec(): Int {
        if (!isRecording || recordingStartTimeMs == 0L) return 0
        return ((System.currentTimeMillis() - recordingStartTimeMs) / 1000L).toInt().coerceAtLeast(1)
    }

    /**
     * Starts high-compressed silent audio recording.
     * Can be invoked with an active trip ID or standalone (null / "standalone").
     */
    fun startRecording(context: Context, tripId: String? = null): Boolean {
        if (isRecording) return true
        try {
            val recordDir = File(context.filesDir, "safety_recordings")
            if (!recordDir.exists()) {
                recordDir.mkdirs()
            }
            val tag = if (!tripId.isNullOrBlank() && tripId != "trip") tripId.take(8) else "standalone"
            val fileName = "blackbox_${tag}_${System.currentTimeMillis()}.m4a"
            val file = File(recordDir, fileName)
            currentRecordingFile = file
            activeTripIdForRecording = tripId
            recordingStartTimeMs = System.currentTimeMillis()

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                // 32 kbps voice-optimized bitrate (High compression, crystal clear speech, ~240 KB / min)
                setAudioEncodingBitRate(32000)
                setAudioSamplingRate(22050)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
            Log.d(TAG, "Silent audio recording started: ${file.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            recordingStartTimeMs = 0L
            return false
        }
    }

    /**
     * Stops the active audio recording, releases mic hardware, and returns the recorded file.
     */
    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Stop failed or zero length audio", e)
                }
                release()
            }
            mediaRecorder = null
            isRecording = false
            val file = currentRecordingFile
            Log.d(TAG, "Silent recording stopped: ${file?.length()} bytes, duration: ${getRecordingDurationSec()}s")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            null
        }
    }
}
