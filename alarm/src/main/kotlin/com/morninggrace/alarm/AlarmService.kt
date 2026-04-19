package com.morninggrace.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.morninggrace.core.model.BroadcastConfig
import com.morninggrace.orchestrator.MorningSession
import com.morninggrace.tts.AndroidTtsEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID      = "morning_grace_alarm"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP     = "STOP"

        // SharedPrefs keys for module toggles (default: all enabled)
        const val KEY_MODULE_WEATHER = "module_weather"
        const val KEY_MODULE_BIBLE   = "module_bible"
        const val KEY_MODULE_FINANCE = "module_finance"
        const val KEY_MODULE_NEWS    = "module_news"
    }

    @Inject lateinit var morningSession: MorningSession
    @Inject lateinit var ttsEngine: AndroidTtsEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var broadcastJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSession()
            return START_NOT_STICKY
        }

        val prefs = getSharedPreferences(AlarmReceiver.PREFS, MODE_PRIVATE)
        val config = BroadcastConfig(
            skipWeather = !prefs.getBoolean(KEY_MODULE_WEATHER, true),
            skipBible   = !prefs.getBoolean(KEY_MODULE_BIBLE,   true),
            skipFinance = !prefs.getBoolean(KEY_MODULE_FINANCE, true),
            skipNews    = !prefs.getBoolean(KEY_MODULE_NEWS,    true)
        )

        startForeground(NOTIFICATION_ID, buildNotification())

        broadcastJob = serviceScope.launch {
            android.util.Log.d("MorningGrace", "AlarmService: attaching TTS")
            ttsEngine.attach(this@AlarmService)
            android.util.Log.d("MorningGrace", "AlarmService: TTS attached, starting session")
            morningSession.start(config)
            android.util.Log.d("MorningGrace", "AlarmService: session done")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        ttsEngine.detach()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopSession() {
        broadcastJob?.cancel()
        broadcastJob = null
        morningSession.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, AlarmService::class.java).apply { action = ACTION_STOP }
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
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
