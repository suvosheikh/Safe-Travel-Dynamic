package com.safetravel.tracker.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.safetravel.tracker.MainActivity
import com.safetravel.tracker.R
import kotlinx.coroutines.launch

class TripTrackingService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val ACTION_SOS = "ACTION_SOS"
        const val ACTION_END_TRIP = "ACTION_END_TRIP"

        const val NOTIFICATION_CHANNEL_ID = "trip_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_TRIP_ID = "EXTRA_TRIP_ID"
        var activeTripId: String? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra(EXTRA_TRIP_ID)) {
                activeTripId = it.getStringExtra(EXTRA_TRIP_ID)
            }
            when (it.action) {
                ACTION_START -> startForegroundService()
                ACTION_STOP -> stopForegroundService()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        createNotificationChannel()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open the app
        val mainActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // SOS Action
        val sosIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_SOS
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sosPendingIntent = PendingIntent.getActivity(
            this,
            1,
            sosIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // End Trip Action
        val endTripIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_END_TRIP
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val endTripPendingIntent = PendingIntent.getActivity(
            this,
            2,
            endTripIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Safe Journey Tracking Active")
            .setContentText("Your trip is being monitored and shared with guardians.")
            .setContentIntent(mainActivityPendingIntent)
            .addAction(android.R.drawable.ic_delete, "END TRIP", endTripPendingIntent)
            .addAction(android.R.drawable.ic_dialog_alert, "SOS", sosPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Trip Tracking",
                NotificationManager.IMPORTANCE_LOW // Low importance so it doesn't ring continuously
            ).apply {
                description = "Keeps location tracking active in the background"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
