package com.morninggrace.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.morninggrace.core.model.BroadcastConfig
import com.morninggrace.orchestrator.MorningSession
import com.morninggrace.tts.AndroidTtsEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "morning_grace_alarm"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.morninggrace.action.STOP"
        const val ACTION_PLAYBACK_STATE = "com.morninggrace.action.PLAYBACK_STATE"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val KEY_PLAYBACK_ACTIVE = "playback_active"

        const val KEY_MODULE_WEATHER = "module_weather"
        const val KEY_MODULE_BIBLE = "module_bible"
        const val KEY_BIBLE_ENGLISH = "bible_english"
        const val KEY_BIBLE_RECORDED_AUDIO = "bible_recorded_audio"
        const val KEY_MODULE_NEWS = "module_news"
        const val KEY_NEWS_FULL_ARTICLES = "news_full_articles"
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
        if (broadcastJob?.isActive == true) return START_NOT_STICKY

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        publishPlaybackState(true)

        val prefs = getSharedPreferences(AlarmReceiver.PREFS, MODE_PRIVATE)
        val config = BroadcastConfig(
            skipWeather = !prefs.getBoolean(KEY_MODULE_WEATHER, true),
            skipBible = !prefs.getBoolean(KEY_MODULE_BIBLE, true),
            includeEnglishBible = false,
            preferRecordedBible = prefs.getBoolean(KEY_BIBLE_RECORDED_AUDIO, true),
            skipNews = !prefs.getBoolean(KEY_MODULE_NEWS, true),
            newsFullArticles = prefs.getBoolean(KEY_NEWS_FULL_ARTICLES, false)
        )

        broadcastJob = serviceScope.launch {
            try {
                ttsEngine.attach(this@AlarmService)
                morningSession.start(config)
            } finally {
                publishPlaybackState(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        publishPlaybackState(false)
        serviceScope.cancel()
        ttsEngine.detach()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopSession() {
        broadcastJob?.cancel()
        broadcastJob = null
        morningSession.stop()
        publishPlaybackState(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publishPlaybackState(isPlaying: Boolean) {
        getSharedPreferences(AlarmReceiver.PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PLAYBACK_ACTIVE, isPlaying)
            .apply()
        sendBroadcast(
            Intent(ACTION_PLAYBACK_STATE)
                .setPackage(packageName)
                .putExtra(EXTRA_IS_PLAYING, isPlaying)
        )
    }

    private fun buildNotification(): Notification {
        val stopPending = PendingIntent.getService(
            this,
            1,
            Intent(this, AlarmService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPending = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(
                this,
                2,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val expanded = RemoteViews(packageName, R.layout.notification_playing).apply {
            setOnClickPendingIntent(R.id.notificationStopButton, stopPending)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("晨光正在播放")
            .setContentText("点“停止播放”即可立即停止")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openPending)
            .setCustomBigContentView(expanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "停止播放", stopPending)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "晨间播报",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "晨光播放控制"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
