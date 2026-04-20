package com.morninggrace.ai

import android.util.Log
import com.morninggrace.core.model.Language
import com.morninggrace.tts.SpeechEngine
import com.morninggrace.tts.TtsEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "MorningGrace"
private const val MAX_TURNS = 15
private const val WINDOW_SIZE = 10
private const val LISTEN_TIMEOUT_MS = 30_000L

@Singleton
class ConversationManager @Inject constructor(
    private val aiClient: AiClient,
    private val ttsEngine: TtsEngine,
    private val speechEngine: SpeechEngine
) {

    suspend fun startConversation(systemPrompt: String) {
        if (!aiClient.isConfigured()) {
            Log.d(TAG, "AI not configured — skipping conversation")
            return
        }
        if (!speechEngine.isAvailable()) {
            Log.d(TAG, "SpeechRecognizer not available — skipping conversation")
            return
        }

        Log.d(TAG, "Starting AI conversation")
        safeSpeak("播报结束，可以开始提问，说关闭结束对话。")

        val history = mutableListOf<ChatMessage>()
        var turns = 0

        while (turns < MAX_TURNS) {
            val input = speechEngine.listenForText(LISTEN_TIMEOUT_MS)
            if (input.isNullOrBlank()) {
                Log.d(TAG, "No speech input — ending conversation")
                safeSpeak("没有收到语音，对话结束。愿你今天蒙福。")
                break
            }
            Log.d(TAG, "User said: $input")

            when {
                DISMISS_WORDS.any { input.contains(it, ignoreCase = true) } -> {
                    safeSpeak("再见，愿你今天平安喜乐。")
                    break
                }
                REPLAY_WORDS.any { input.contains(it) } -> {
                    safeSpeak("重播请重新触发闹钟。对话结束。")
                    break
                }
            }

            val window = if (history.size > WINDOW_SIZE) history.takeLast(WINDOW_SIZE) else history
            val response = aiClient.chat(systemPrompt, window + ChatMessage("user", input))
            if (response == null) {
                safeSpeak("AI 暂时无法响应，对话结束。")
                break
            }

            history += ChatMessage("user", input)
            history += ChatMessage("model", response)
            turns++

            safeSpeak(response)
        }

        if (turns >= MAX_TURNS) safeSpeak("已达对话上限，再见。愿你今天蒙福。")
        Log.d(TAG, "Conversation ended after $turns turns")
    }

    private suspend fun safeSpeak(text: String) {
        runCatching { ttsEngine.speak(text, Language.ZH) }
            .onFailure { if (it is CancellationException) throw it }
    }

    companion object {
        private val DISMISS_WORDS = listOf("关闭", "停止", "结束", "再见", "dismiss")
        private val REPLAY_WORDS  = listOf("重播", "再播", "再读一遍", "重新播报")
    }
}
