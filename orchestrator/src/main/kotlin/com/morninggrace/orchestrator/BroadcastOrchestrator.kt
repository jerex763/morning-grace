package com.morninggrace.orchestrator

import com.morninggrace.bible.BibleRepository
import com.morninggrace.bible.plan.BibleReadingPlan
import com.morninggrace.core.model.Language
import com.morninggrace.tts.TtsEngine
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class BroadcastOrchestrator @Inject constructor(
    private val ttsEngine: TtsEngine,
    private val bibleRepo: BibleRepository,
    private val readingPlan: BibleReadingPlan
) {

    var state: BroadcastState = BroadcastState.Idle
        private set

    suspend fun broadcast(date: LocalDate = LocalDate.now()) {
        state = BroadcastState.Preparing
        try {
            val content = prepare(date)
            state = BroadcastState.Broadcasting(content)
            deliver(content)
        } finally {
            state = BroadcastState.Idle
        }
    }

    fun stop() {
        state = BroadcastState.Idle
    }

    private suspend fun prepare(date: LocalDate): BroadcastContent {
        val passages = readingPlan.getReadingForDate(date)
        val firstPassage = passages.firstOrNull()

        val bibleZh = if (firstPassage != null) {
            bibleRepo.getVersesForPassage(firstPassage, "zh")
                .joinToString(" ") { it.text }
                .ifBlank { "今日经文暂不可用" }
        } else {
            "今日经文暂不可用"
        }

        val bibleEn = if (firstPassage != null) {
            bibleRepo.getVersesForPassage(firstPassage, "en")
                .joinToString(" ") { it.text }
                .ifBlank { "Bible reading unavailable" }
        } else {
            "Bible reading unavailable"
        }

        return BroadcastContent(
            greeting = "早安，晨光播报开始。",
            bibleZh = bibleZh,
            bibleEn = bibleEn,
            weather = "天气功能即将推出。",
            finance = "财经功能即将推出。"
        )
    }

    private suspend fun deliver(content: BroadcastContent) {
        safeSpeak(content.greeting, Language.ZH)
        safeSpeak(content.weather, Language.ZH)
        safeSpeak("今日读经：", Language.ZH)
        safeSpeak(content.bibleZh, Language.ZH)
        safeSpeak(content.bibleEn, Language.EN)
        safeSpeak(content.finance, Language.ZH)
        safeSpeak("晨光播报结束，愿你今天蒙福。", Language.ZH)
    }

    private suspend fun safeSpeak(text: String, language: Language) {
        if (text.isBlank()) return
        runCatching { ttsEngine.speak(text, language) }
            .onFailure { e ->
                if (e is CancellationException) throw e
            }
    }
}
