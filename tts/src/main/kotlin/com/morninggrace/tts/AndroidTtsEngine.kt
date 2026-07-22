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

    /** Must be called before [speak]. Suspends until TTS engine is initialised; throws on failure. */
    suspend fun attach(context: Context) = suspendCancellableCoroutine<Unit> { cont ->
        attachedContext = context.applicationContext
        val engine = TextToSpeech(context) { status ->
            onInitResult(status)
            if (!cont.isActive) return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) cont.resume(Unit)
            else cont.resumeWithException(RuntimeException("TTS init failed, status=$status"))
        }
        tts = engine
        cont.invokeOnCancellation { engine.stop(); engine.shutdown() }
    }

    fun detach() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        attachedContext = null
        ready = false
    }

    override fun isAvailable(): Boolean = ready

    override fun stop() { tts?.stop() }

    override suspend fun speak(text: String, language: Language) {
        val engine = requireNotNull(tts) { "AndroidTtsEngine not attached" }
        require(ready) { "AndroidTtsEngine not ready" }

        val locale = when (language) {
            Language.ZH -> Locale.SIMPLIFIED_CHINESE
            Language.EN -> Locale.ENGLISH
        }
        engine.language = locale
        val speechRate = if (language == Language.ZH) {
            attachedContext
                ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                ?.getFloat(KEY_CHINESE_SPEECH_RATE, DEFAULT_CHINESE_SPEECH_RATE)
                ?: DEFAULT_CHINESE_SPEECH_RATE
        } else {
            1.0f
        }
        engine.setSpeechRate(speechRate)

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

    @Volatile private var attachedContext: Context? = null

    companion object {
        const val PREFS = "alarm_prefs"
        const val KEY_CHINESE_SPEECH_RATE = "chinese_speech_rate"
        const val DEFAULT_CHINESE_SPEECH_RATE = 0.88f
    }
}
