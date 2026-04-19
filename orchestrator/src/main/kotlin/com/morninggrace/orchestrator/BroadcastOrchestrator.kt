package com.morninggrace.orchestrator

import android.util.Log
import com.morninggrace.bible.BibleRepository
import com.morninggrace.bible.plan.BibleReadingPlan
import com.morninggrace.core.model.Language
import com.morninggrace.core.model.LocationPrefs
import com.morninggrace.core.repository.FinanceRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.tts.TtsEngine
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private const val TAG = "MorningGrace"

class BroadcastOrchestrator @Inject constructor(
    private val ttsEngine: TtsEngine,
    private val bibleRepo: BibleRepository,
    private val readingPlan: BibleReadingPlan,
    private val weatherRepo: WeatherRepository,
    private val financeRepo: FinanceRepository,
    private val locationPrefs: LocationPrefs
) {

    var state: BroadcastState = BroadcastState.Idle
        private set

    suspend fun broadcast(date: LocalDate = LocalDate.now()) {
        // Wait up to 3s for TTS to be ready (onInit can be async on some devices)
        var waited = 0
        while (!ttsEngine.isAvailable() && waited < 30) { delay(100); waited++ }
        Log.d(TAG, "broadcast() started, ttsAvailable=${ttsEngine.isAvailable()}, waited=${waited * 100}ms")
        state = BroadcastState.Preparing
        try {
            val content = prepare(date)
            state = BroadcastState.Broadcasting(content)
            Log.d(TAG, "deliver() starting")
            deliver(content)
            Log.d(TAG, "deliver() done")
        } catch (e: Exception) {
            Log.e(TAG, "broadcast() failed", e)
            throw e
        } finally {
            state = BroadcastState.Idle
        }
    }

    fun stop() {
        state = BroadcastState.Idle
    }

    private suspend fun prepare(date: LocalDate): BroadcastContent = coroutineScope {
        // TODO: re-enable after TTS confirmed working
        // val weatherJob = async { weatherRepo.getCurrentWeather(locationPrefs.lat, locationPrefs.lon) }
        // val financeJob = async { financeRepo.getSandP500() }

        Log.d(TAG, "prepare() loading bible plan for $date")
        val passages = readingPlan.getReadingForDate(date)
        val firstPassage = passages.firstOrNull()
        Log.d(TAG, "prepare() bible passage: $firstPassage")

        val bibleZh = if (firstPassage != null) {
            Log.d(TAG, "prepare() fetching zh verses")
            bibleRepo.getVersesForPassage(firstPassage, "zh")
                .joinToString(" ") { it.text }
                .ifBlank { "今日经文暂不可用" }
        } else {
            "今日经文暂不可用"
        }

        val bibleEn = if (firstPassage != null) {
            Log.d(TAG, "prepare() fetching en verses")
            bibleRepo.getVersesForPassage(firstPassage, "en")
                .joinToString(" ") { it.text }
                .ifBlank { "Bible reading unavailable" }
        } else {
            "Bible reading unavailable"
        }
        Log.d(TAG, "prepare() bible done")

        val weather = "天气功能暂时无法获取"
        val finance = "财经功能暂时无法获取"

        BroadcastContent(
            greeting = "早安，晨光播报开始。",
            bibleZh = bibleZh,
            bibleEn = bibleEn,
            weather = weather,
            finance = finance
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
        Log.d(TAG, "speak: \"${text.take(30)}\" [$language]")
        runCatching { ttsEngine.speak(text, language) }
            .onFailure { e ->
                Log.e(TAG, "speak failed: ${e.message}")
                if (e is CancellationException) throw e
            }
    }
}
