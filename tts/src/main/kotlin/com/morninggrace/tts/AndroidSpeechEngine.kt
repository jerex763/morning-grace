package com.morninggrace.tts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.morninggrace.core.model.ConfirmationResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private const val TAG = "MorningGrace"

@Singleton
class AndroidSpeechEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechEngine {

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override suspend fun listenForConfirmation(timeoutMs: Long): ConfirmationResult {
        if (!isAvailable()) {
            Log.w(TAG, "SpeechRecognizer not available — auto-confirming")
            return ConfirmationResult.Confirmed
        }
        return withContext(Dispatchers.Main) {
            delay(500) // let the audio subsystem settle after TTS
            val timed: ConfirmationResult? = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            Log.d(TAG, "Speech results: $matches")
                            val result = classify(matches)
                            recognizer.destroy()
                            if (cont.isActive) cont.resume(result)
                        }

                        override fun onError(error: Int) {
                            Log.w(TAG, "SpeechRecognizer error code: $error")
                            recognizer.destroy()
                            if (cont.isActive) cont.resume(ConfirmationResult.Timeout)
                        }
                    })

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3_000L)
                        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
                    }
                    recognizer.startListening(intent)

                    cont.invokeOnCancellation {
                        recognizer.stopListening()
                        recognizer.destroy()
                    }
                }
            }
            timed ?: ConfirmationResult.Timeout
        }
    }

    private fun classify(matches: List<String>?): ConfirmationResult {
        if (matches.isNullOrEmpty()) return ConfirmationResult.Timeout
        val text = matches.joinToString(" ")
        return if (SKIP_WORDS.any { text.contains(it) }) ConfirmationResult.Skipped
        else ConfirmationResult.Confirmed
    }

    companion object {
        private val SKIP_WORDS = listOf("跳过", "不读", "跳")
    }
}
