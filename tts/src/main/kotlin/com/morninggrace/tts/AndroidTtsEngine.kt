package com.morninggrace.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.morninggrace.core.model.Language
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AndroidTtsEngine @Inject constructor() : TtsEngine {

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    /** Called by the Android TextToSpeech.OnInitListener. */
    fun onInitResult(status: Int) {
        // Only allow SUCCESS to set ready=true. Ignore subsequent ERROR callbacks
        // (Android TTS fires onInit twice on some devices/emulators).
        // Only detach() should reset ready to false.
        if (status == TextToSpeech.SUCCESS) ready = true
        android.util.Log.d("MorningGrace", "TTS onInit: status=$status ready=$ready (SUCCESS=${TextToSpeech.SUCCESS})")
    }

    /** Must be called before [speak]. Suspends until TTS engine is initialised. */
    suspend fun attach(context: Context) = suspendCancellableCoroutine<Unit> { cont ->
        tts = TextToSpeech(context) { status ->
            onInitResult(status)
            if (cont.isActive) cont.resume(Unit)
        }
    }

    fun detach() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    override fun isAvailable(): Boolean = ready

    override suspend fun speak(text: String, language: Language) {
        val engine = requireNotNull(tts) { "AndroidTtsEngine not attached" }
        require(ready) { "AndroidTtsEngine not ready" }

        val locale = Locale.ENGLISH // TODO: restore ZH after TTS voice pack confirmed
        engine.language = locale

        val utteranceId = UUID.randomUUID().toString()

        suspendCancellableCoroutine<Unit> { cont ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String) {}
                override fun onDone(id: String) {
                    if (id == utteranceId && cont.isActive) cont.resume(Unit)
                }
                override fun onError(id: String, errorCode: Int) {
                    if (id == utteranceId && cont.isActive)
                        cont.resumeWithException(RuntimeException("TTS error on utterance $id, code $errorCode"))
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String) {
                    if (id == utteranceId && cont.isActive)
                        cont.resumeWithException(RuntimeException("TTS error on utterance $id"))
                }
            })

            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR && cont.isActive) {
                cont.resumeWithException(RuntimeException("TTS speak() returned ERROR"))
            }

            cont.invokeOnCancellation { engine.stop() }
        }
    }
}
