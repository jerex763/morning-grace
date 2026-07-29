package com.morninggrace.bible.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface BibleAudioPlayer {
    suspend fun playChapter(book: Int, chapter: Int): Boolean
    fun stop()
}

@Singleton
class AndroidBibleAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val library: BibleAudioLibrary
) : BibleAudioPlayer {
    private val lock = Any()
    private var current: MediaPlayer? = null

    override suspend fun playChapter(book: Int, chapter: Int): Boolean {
        val file = library.findChapter(book, chapter) ?: return false
        return suspendCancellableCoroutine { continuation ->
            val player = MediaPlayer()
            val finished = AtomicBoolean(false)
            val audioManager = context.getSystemService(AudioManager::class.java)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            lateinit var focusRequest: AudioFocusRequest

            fun finish(result: Boolean) {
                if (!finished.compareAndSet(false, true)) return
                synchronized(lock) {
                    if (current === player) current = null
                }
                audioManager.abandonAudioFocusRequest(focusRequest)
                runCatching { player.release() }
                if (continuation.isActive) continuation.resume(result)
            }

            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { change ->
                    when (change) {
                        AudioManager.AUDIOFOCUS_LOSS -> finish(false)
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                            runCatching { player.setVolume(0.25f, 0.25f) }
                        AudioManager.AUDIOFOCUS_GAIN ->
                            runCatching { player.setVolume(1.0f, 1.0f) }
                    }
                }
                .build()

            continuation.invokeOnCancellation {
                if (!finished.compareAndSet(false, true)) return@invokeOnCancellation
                synchronized(lock) {
                    if (current === player) current = null
                }
                audioManager.abandonAudioFocusRequest(focusRequest)
                runCatching { player.stop() }
                runCatching { player.release() }
            }

            try {
                synchronized(lock) {
                    current?.let { previous ->
                        runCatching { previous.stop() }
                        runCatching { previous.release() }
                    }
                    current = player
                }
                player.setAudioAttributes(
                    attributes
                )
                player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                player.setDataSource(file.absolutePath)
                player.setOnPreparedListener {
                    if (audioManager.requestAudioFocus(focusRequest) ==
                        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                    ) {
                        it.setVolume(1.0f, 1.0f)
                        it.start()
                    } else {
                        finish(false)
                    }
                }
                player.setOnCompletionListener { finish(true) }
                player.setOnErrorListener { _, _, _ ->
                    finish(false)
                    true
                }
                player.prepareAsync()
            } catch (_: Exception) {
                finish(false)
            }
        }
    }

    override fun stop() {
        val player = synchronized(lock) {
            current.also { current = null }
        }
        player?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
    }
}
