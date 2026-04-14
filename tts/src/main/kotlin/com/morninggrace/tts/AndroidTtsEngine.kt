package com.morninggrace.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.morninggrace.core.model.Language
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidTtsEngine @Inject constructor() : TtsEngine {

    private var tts: TextToSpeech? = null
    private var ready = false

    /** Called by the Android TextToSpeech.OnInitListener. */
    fun onInitResult(status: Int) {
        ready = (status == TextToSpeech.SUCCESS)
    }

    /** Must be called before [speak]. Attach to a Context and wait. */
    fun attach(context: Context) {
        tts = TextToSpeech(context) { status -> onInitResult(status) }
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

        val locale = if (language == Language.ZH) Locale.CHINESE else Locale.ENGLISH
        engine.language = locale

        val utteranceId = UUID.randomUUID().toString()

        suspendCancellableCoroutine<Unit> { cont ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String) {}
                override fun onDone(id: String) {
                    if (id == utteranceId) cont.resume(Unit)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String) {
                    if (id == utteranceId) cont.resumeWithException(RuntimeException("TTS error on utterance $id"))
                }
            })

            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.ERROR) {
                cont.resumeWithException(RuntimeException("TTS speak() returned ERROR"))
            }

            cont.invokeOnCancellation { engine.stop() }
        }
    }
}
