package com.safetravel.tracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safetravel.tracker.MainActivity
import com.safetravel.tracker.supabase.SupabaseManager
import com.safetravel.tracker.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM Token: $token")
        // Store token in SharedPreferences and try to upload to Supabase if logged in
        val sharedPrefs = getSharedPreferences("safetravel_session", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("fcm_token", token).apply()

        // Sync to Supabase
        uploadTokenToSupabase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }

        // Check if message contains notification payload
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Safe Travel Alert"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "A safety update is available."
        val tripId = remoteMessage.data["trip_id"]

        sendNotification(title, body, tripId)
    }

    private fun sendNotification(title: String, body: String, tripId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (tripId != null) {
                putExtra("trip_id", tripId)
                putExtra("action", "track_trip")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "safe_travel_alerts"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Custom styled notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System standard fallback icon, or customize
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Safe Travel Safety Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time alerts when a loved one shares their live trip or triggers SOS"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun uploadTokenToSupabase(token: String) {
        val userId = SupabaseManager.currentUserId ?: return
        val authHeader = SupabaseClient.getValidAuthHeader()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (SupabaseClient.isConfigured) {
                    val dbService = SupabaseClient.dbService ?: return@launch
                    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val timestamp = df.format(Calendar.getInstance().time)
                    val payload = mapOf(
                        "user_id" to userId,
                        "fcm_token" to token,
                        "updated_at" to timestamp
                    )
                    
                    // We call the upsert endpoint we will define in SupabaseDbService
                    val response = dbService.upsertUserDevice(payload, authHeader)
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM token uploaded successfully to Supabase.")
                    } else {
                        Log.e(TAG, "Failed to upload FCM token: ${response.code()} ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading FCM token: ", e)
            }
        }
    }

    companion object {
        private const val TAG = "SafeTravelFCM"
    }
}
