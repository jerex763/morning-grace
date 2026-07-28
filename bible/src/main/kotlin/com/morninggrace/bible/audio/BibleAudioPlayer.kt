package com.morninggrace.bible.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
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
    private val library: BibleAudioLibrary
) : BibleAudioPlayer {
    private val lock = Any()
    private var current: MediaPlayer? = null

    override suspend fun playChapter(book: Int, chapter: Int): Boolean {
        val file = library.findChapter(book, chapter) ?: return false
        return suspendCancellableCoroutine { continuation ->
            val player = MediaPlayer()

            fun finish(result: Boolean) {
                synchronized(lock) {
                    if (current === player) current = null
                }
                runCatching { player.release() }
                if (continuation.isActive) continuation.resume(result)
            }

            continuation.invokeOnCancellation {
                synchronized(lock) {
                    if (current === player) current = null
                }
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
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                player.setDataSource(file.absolutePath)
                player.setOnPreparedListener { it.start() }
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
