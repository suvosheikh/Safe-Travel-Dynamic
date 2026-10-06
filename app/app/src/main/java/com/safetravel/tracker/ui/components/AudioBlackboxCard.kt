package com.safetravel.tracker.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.SupabaseSafetyAudioLog
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.util.AudioPlayerHelper
import com.safetravel.tracker.util.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AudioBlackboxCard(
    audioLog: SupabaseSafetyAudioLog,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isPlayingThis by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Periodically update playback progress when playing this audio
    LaunchedEffect(isPlayingThis) {
        if (isPlayingThis) {
            while (isActive && AudioPlayerHelper.isPlaying && AudioPlayerHelper.currentPlayingUrl == audioLog.audioUrl) {
                val dur = AudioPlayerHelper.durationMs.toFloat().coerceAtLeast(1f)
                val pos = AudioPlayerHelper.currentPositionMs.toFloat()
                currentProgress = (pos / dur).coerceIn(0f, 1f)
                delay(200)
            }
            if (AudioPlayerHelper.currentPlayingUrl != audioLog.audioUrl || !AudioPlayerHelper.isPlaying) {
                isPlayingThis = false
                currentProgress = 0f
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (AudioPlayerHelper.currentPlayingUrl == audioLog.audioUrl) {
                AudioPlayerHelper.stop()
            }
        }
    }

    val isTripLinked = !audioLog.tripId.isNullOrBlank()
    val formattedTime = DateTimeUtils.formatBstDateTime(audioLog.createdAt)
    val displayAddress = audioLog.recordedAddress?.takeIf { it.isNotBlank() } ?: "Safe Travel GPS Location"

    val durationText = if (audioLog.durationSec > 0) {
        val mins = audioLog.durationSec / 60
        val secs = audioLog.durationSec % 60
        String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    } else {
        "Audio Clip"
    }

    val sizeText = if (audioLog.fileSizeBytes > 0) {
        val kb = audioLog.fileSizeBytes / 1024
        if (kb >= 1024) "%.1f MB".format(kb / 1024.0) else "$kb KB"
    } else null

    val borderColor: Color = if (isPlayingThis) Cyan500.copy(alpha = 0.6f) else Slate700.copy(alpha = 0.5f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category Pill & Source Trigger Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTripLinked) Emerald500.copy(alpha = 0.15f) else Cyan500.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isTripLinked) "TRIP BOUND" else "STANDALONE SAFETY",
                            color = if (isTripLinked) Emerald400 else Cyan400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    if (audioLog.sourceTrigger == "sos_trigger") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Red500.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Red500,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("SOS TRIGGER", color = Red500, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Text(
                    text = formattedTime,
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address / Location Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Emerald400,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = displayAddress,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Player Bar: Play Button + Progress Bar + Duration + Share Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Circle Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isPlayingThis) Cyan500 else Slate700)
                        .clickable {
                            if (isPlayingThis) {
                                AudioPlayerHelper.pause()
                                isPlayingThis = false
                            } else {
                                AudioPlayerHelper.play(
                                    urlOrPath = audioLog.audioUrl,
                                    onPrepared = { isPlayingThis = true },
                                    onCompletion = {
                                        isPlayingThis = false
                                        currentProgress = 0f
                                    },
                                    onError = {
                                        isPlayingThis = false
                                        currentProgress = 0f
                                    }
                                )
                                isPlayingThis = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingThis) "Pause" else "Play",
                        tint = if (isPlayingThis) Slate900 else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track Progress and Metadata
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isPlayingThis) "Playing Audio..." else "Voice Blackbox",
                            color = if (isPlayingThis) Cyan400 else Slate300,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (sizeText != null) "$durationText • $sizeText" else durationText,
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { if (isPlayingThis) currentProgress else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Cyan400,
                        trackColor = Slate700
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Share / Export Evidence Button
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "[SafeTravel Audio Evidence]\nRecorded at: $displayAddress\nTime: $formattedTime\nListen / Download: ${audioLog.audioUrl}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Audio Evidence"))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Audio Evidence Button
                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Audio",
                            tint = Red500.copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }

    // Reusable Frosted Glassmorphism Confirmation Wizard Dialog
    GlassmorphismConfirmationWizard(
        showDialog = showDeleteDialog,
        title = "অডিও প্রমাণ মুছে ফেলা",
        message = "আপনি কি আপনার এই অডিও রেকর্ডটি মুছে ফেলতে চান?",
        warningNote = "আপনার মুছে ফেলা অডিওটি আপনার অ্যাপ ও ডিভাইস থেকে সম্পূর্ণ ডিলিট হয়ে যাবে এবং আপনি এটি আর শুনতে বা পুনরুদ্ধার করতে পারবেন না। ভবিষ্যতে কোনো অনাকাঙ্ক্ষিত ঘটনা বা আইনি অনুসন্ধানে এই অডিও প্রমাণ কাজে লাগতে পারে, তাই সম্পূর্ণ নিশ্চিত হয়ে সিদ্ধান্ত নিন।",
        warningBadgeText = "জরুরি সতর্কতা",
        confirmButtonText = "হ্যাঁ, মুছে ফেলুন",
        dismissButtonText = "না / বাতিল",
        icon = Icons.Default.DeleteForever,
        accentColor = Red500,
        onConfirm = {
            showDeleteDialog = false
            if (isPlayingThis) {
                AudioPlayerHelper.stop()
                isPlayingThis = false
            }
            onDelete?.invoke()
        },
        onDismiss = { showDeleteDialog = false }
    )
}
