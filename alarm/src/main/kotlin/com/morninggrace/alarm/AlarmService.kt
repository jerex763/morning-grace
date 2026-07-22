package com.morninggrace.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
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

private const val TAG = "MorningGrace"

@AndroidEntryPoint
class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID      = "morning_grace_alarm"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP     = "STOP"

        // SharedPrefs keys for module toggles (default: all enabled)
        const val KEY_MODULE_WEATHER = "module_weather"
        const val KEY_MODULE_BIBLE   = "module_bible"
        const val KEY_BIBLE_ENGLISH  = "bible_english"
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

        if (!startForegroundCompat()) return START_NOT_STICKY

        // Idempotent: ignore duplicate starts while a broadcast is already running.
        if (broadcastJob?.isActive == true) {
            Log.d(TAG, "AlarmService: broadcast already running, ignoring duplicate start")
            return START_NOT_STICKY
        }

        val prefs = getSharedPreferences(AlarmReceiver.PREFS, MODE_PRIVATE)
        val config = BroadcastConfig(
            skipWeather = !prefs.getBoolean(KEY_MODULE_WEATHER, true),
            skipBible   = !prefs.getBoolean(KEY_MODULE_BIBLE,   true),
            includeEnglishBible = prefs.getBoolean(KEY_BIBLE_ENGLISH, false),
            skipFinance = !prefs.getBoolean(KEY_MODULE_FINANCE, true),
            skipNews    = !prefs.getBoolean(KEY_MODULE_NEWS,    true)
        )

        broadcastJob = serviceScope.launch {
            Log.d(TAG, "AlarmService: attaching TTS")
            ttsEngine.attach(this@AlarmService)
            Log.d(TAG, "AlarmService: TTS attached, starting session")
            morningSession.start(config)
            Log.d(TAG, "AlarmService: session done")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    /**
     * Starts the foreground service with the microphone type only when RECORD_AUDIO is granted.
     * Android 14 throws if a declared FGS type lacks its prerequisite permission.
     * @return false if startup was rejected by the platform (service is stopping).
     */
    private fun startForegroundCompat(): Boolean {
        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        if (micGranted) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
            true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed: ${e.message}", e)
            stopSelf()
            false
        }
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
