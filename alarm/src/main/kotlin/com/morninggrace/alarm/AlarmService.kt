package com.morninggrace.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
        const val KEY_TTS_AVAILABLE = "tts_available"

        const val KEY_MODULE_WEATHER = "module_weather"
        const val KEY_MODULE_BIBLE = "module_bible"
        const val KEY_BIBLE_ENGLISH = "bible_english"
        const val KEY_BIBLE_RECORDED_AUDIO = "bible_recorded_audio"
        const val KEY_MODULE_NEWS = "module_news"
        const val KEY_NEWS_FULL_ARTICLES = "news_full_articles"
        private const val MAX_WAKE_LOCK_MILLIS = 2 * 60 * 60 * 1_000L
    }

    @Inject lateinit var morningSession: MorningSession
    @Inject lateinit var ttsEngine: AndroidTtsEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var broadcastJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )
        publishPlaybackState(true)

        val prefs = getSharedPreferences(AlarmReceiver.PREFS, MODE_PRIVATE)
        val weatherEnabled = prefs.getBoolean(KEY_MODULE_WEATHER, true)
        val newsEnabled = prefs.getBoolean(KEY_MODULE_NEWS, true)
        val offline = !hasInternetNetwork() && (weatherEnabled || newsEnabled)
        val config = BroadcastConfig(
            skipWeather = !weatherEnabled,
            skipBible = !prefs.getBoolean(KEY_MODULE_BIBLE, true),
            includeEnglishBible = false,
            preferRecordedBible = prefs.getBoolean(KEY_BIBLE_RECORDED_AUDIO, true),
            skipNews = !newsEnabled,
            newsFullArticles = prefs.getBoolean(KEY_NEWS_FULL_ARTICLES, false),
            offline = offline
        )

        broadcastJob = serviceScope.launch {
            try {
                acquireWakeLock()
                val ttsAvailable = ttsEngine.attach(this@AlarmService)
                prefs.edit().putBoolean(KEY_TTS_AVAILABLE, ttsAvailable).apply()
                morningSession.start(config)
            } finally {
                releaseWakeLock()
                publishPlaybackState(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun hasInternetNetwork(): Boolean {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroy() {
        releaseWakeLock()
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

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:MorningBroadcast")
            .apply {
                setReferenceCounted(false)
                acquire(MAX_WAKE_LOCK_MILLIS)
            }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
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
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_description))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openPending)
            .setCustomBigContentView(expanded)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_stop),
                stopPending
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

}
