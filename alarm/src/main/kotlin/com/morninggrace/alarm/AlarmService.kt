package com.morninggrace.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "morning_grace_alarm"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP"
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Morning Grace")
            .setContentText("晨间播报进行中...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Morning Grace Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Morning Grace daily alarm" }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
