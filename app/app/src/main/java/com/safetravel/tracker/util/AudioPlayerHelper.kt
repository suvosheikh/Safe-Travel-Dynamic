package com.safetravel.tracker.util

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

/**
 * Dedicated Modular Utility for Playing Audio Blackbox Recordings.
 * Handles both local file playback and streaming from Cloudinary HTTPS URLs.
 */
object AudioPlayerHelper {
    private const val TAG = "AudioPlayerHelper"
    private var mediaPlayer: MediaPlayer? = null

    var currentPlayingUrl: String? = null
        private set

    val isPlaying: Boolean
        get() = try { mediaPlayer?.isPlaying == true } catch (e: Exception) { false }

    val currentPositionMs: Int
        get() = try { mediaPlayer?.currentPosition ?: 0 } catch (e: Exception) { 0 }

    val durationMs: Int
        get() = try { mediaPlayer?.duration ?: 0 } catch (e: Exception) { 0 }

    fun play(
        urlOrPath: String,
        onPrepared: (() -> Unit)? = null,
        onCompletion: (() -> Unit)? = null,
        onError: (() -> Unit)? = null
    ) {
        try {
            if (currentPlayingUrl == urlOrPath && mediaPlayer != null) {
                if (!isPlaying) {
                    mediaPlayer?.start()
                }
                return
            }

            stop()

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(urlOrPath)
                setOnPreparedListener { mp ->
                    mp.start()
                    currentPlayingUrl = urlOrPath
                    onPrepared?.invoke()
                    Log.d(TAG, "Audio playback started: $urlOrPath")
                }
                setOnCompletionListener {
                    currentPlayingUrl = null
                    onCompletion?.invoke()
                    Log.d(TAG, "Audio playback completed")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    currentPlayingUrl = null
                    onError?.invoke()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio playback", e)
            stop()
            onError?.invoke()
        }
    }

    fun pause() {
        try {
            if (isPlaying) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio player", e)
        } finally {
            mediaPlayer = null
            currentPlayingUrl = null
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio", e)
        }
    }
}
