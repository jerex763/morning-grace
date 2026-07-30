package com.morninggrace.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    @Volatile private var engineName = "未知"
    @Volatile private var languageStatus = TextToSpeech.LANG_NOT_SUPPORTED

    /** Called by the Android TextToSpeech.OnInitListener. */
    fun onInitResult(status: Int) {
        // Only allow SUCCESS to set ready=true. Ignore subsequent ERROR callbacks
        // (Android TTS fires onInit twice on some devices/emulators).
        // Only detach() should reset ready to false.
        if (status == TextToSpeech.SUCCESS) ready = true
        android.util.Log.d("MorningGrace", "TTS onInit: status=$status ready=$ready (SUCCESS=${TextToSpeech.SUCCESS})")
    }

    /**
     * Initialises the system voice. Returns false instead of throwing so callers
     * can still continue with imported recordings on devices without Chinese TTS.
     */
    suspend fun attach(context: Context): Boolean = suspendCancellableCoroutine { cont ->
        attachedContext = context.applicationContext
        val engine = TextToSpeech(context) { status ->
            // Some vendor engines invoke their callback unusually early. Posting the
            // initialization work guarantees that `tts` has been assigned first.
            Handler(Looper.getMainLooper()).post {
                onInitResult(status)
                if (!cont.isActive) return@post
                val initialized = if (status == TextToSpeech.SUCCESS) {
                    val current = tts
                    engineName = current?.defaultEngine ?: "未知"
                    current?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    languageStatus = current?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                        ?: TextToSpeech.LANG_NOT_SUPPORTED
                    languageStatus >= TextToSpeech.LANG_AVAILABLE
                } else {
                    languageStatus = TextToSpeech.LANG_NOT_SUPPORTED
                    false
                }
                ready = initialized
                android.util.Log.d(
                    "MorningGrace",
                    "TTS engine=$engineName languageStatus=$languageStatus ready=$ready"
                )
                cont.resume(initialized)
            }
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

    fun diagnosticSummary(): String =
        "引擎：$engineName；中文状态：$languageStatus；就绪：${if (ready) "是" else "否"}"

    override fun isAvailable(): Boolean = ready

    override fun stop() { tts?.stop() }

    override suspend fun speak(text: String, language: Language) {
        val engine = requireNotNull(tts) { "AndroidTtsEngine not attached" }
        require(ready) { "AndroidTtsEngine not ready" }

        val locale = when (language) {
            Language.ZH -> Locale.SIMPLIFIED_CHINESE
            Language.EN -> Locale.ENGLISH
        }
        val languageStatus = engine.setLanguage(locale)
        require(languageStatus >= TextToSpeech.LANG_AVAILABLE) {
            "TTS language unavailable: $locale"
        }
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
                        cont.resumeWithException(
                            RuntimeException(
                                "系统语音错误码 $errorCode（$engineName，中文状态 $languageStatus）"
                            )
                        )
                }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String) {
                    if (id == utteranceId && cont.isActive)
                        cont.resumeWithException(
                            RuntimeException(
                                "系统语音播放失败（$engineName，中文状态 $languageStatus）"
                            )
                        )
                }
            })

            val params = Bundle().apply {
                // Recorded Bible files are mastered more quietly than most system voices.
                // Slightly attenuate TTS so transitions do not jump in perceived loudness.
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, TTS_VOLUME)
            }
            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (result == TextToSpeech.ERROR && cont.isActive) {
                cont.resumeWithException(
                    RuntimeException(
                        "系统拒绝开始朗读（$engineName，中文状态 $languageStatus）"
                    )
                )
            }

            cont.invokeOnCancellation { engine.stop() }
        }
    }

    @Volatile private var attachedContext: Context? = null

    companion object {
        const val PREFS = "alarm_prefs"
        const val KEY_CHINESE_SPEECH_RATE = "chinese_speech_rate"
        const val DEFAULT_CHINESE_SPEECH_RATE = 0.88f
        const val TTS_VOLUME = 0.82f
    }
}
