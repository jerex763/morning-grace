package com.morninggrace.orchestrator

import android.util.Log
import com.morninggrace.bible.BibleRepository
import com.morninggrace.bible.plan.BibleReadingPlan
import com.morninggrace.bible.toChineseTitle
import com.morninggrace.core.model.ConfirmationResult
import com.morninggrace.core.model.Language
import com.morninggrace.core.repository.FinanceRepository
import com.morninggrace.core.repository.LocationRepository
import com.morninggrace.core.repository.NewsRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.tts.SpeechEngine
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
    private val newsRepo: NewsRepository,
    private val locationRepo: LocationRepository,
    private val speechEngine: SpeechEngine
) {

    var state: BroadcastState = BroadcastState.Idle
        private set

    suspend fun broadcast(date: LocalDate = LocalDate.now(), skipBible: Boolean = false) {
        var waited = 0
        while (!ttsEngine.isAvailable() && waited < 30) { delay(100); waited++ }
        Log.d(TAG, "broadcast() started, ttsAvailable=${ttsEngine.isAvailable()}, waited=${waited * 100}ms")
        state = BroadcastState.Preparing
        try {
            val content = prepare(date, skipBible)
            state = BroadcastState.Broadcasting(content)

            Log.d(TAG, "deliver() starting")
            deliver(content, skipBible)
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

    private suspend fun prepare(date: LocalDate, skipBible: Boolean): BroadcastContent = coroutineScope {
        val location = locationRepo.get()
        val weatherJob = async { weatherRepo.getCurrentWeather(location.lat, location.lon) }
        val financeJob = async { financeRepo.getMarketData() }
        val newsJob    = async { newsRepo.getTopHeadlines(3) }

        Log.d(TAG, "prepare() loading bible plan for $date")
        val passage = if (!skipBible) readingPlan.getReadingForDate(date).firstOrNull() else null
        Log.d(TAG, "prepare() bible passage: $passage (skipBible=$skipBible)")

        val passageName = passage?.toChineseTitle() ?: ""

        val bibleZh = if (passage != null) {
            bibleRepo.getVersesForPassage(passage, "zh")
                .joinToString(" ") { it.text }
                .ifBlank { "今日经文暂不可用" }
        } else if (skipBible) "" else "今日经文暂不可用"

        val bibleEn = if (passage != null) {
            bibleRepo.getVersesForPassage(passage, "en")
                .joinToString(" ") { it.text }
                .ifBlank { "Bible reading unavailable" }
        } else if (skipBible) "" else "Bible reading unavailable"

        Log.d(TAG, "prepare() bible done")

        val weather = weatherJob.await()?.toSpeechZh() ?: "天气暂时无法获取"

        val marketList = financeJob.await()
        val marketSummary = if (marketList.isEmpty()) "市场数据暂时无法获取"
        else marketList.joinToString("；") { it.toSpeechZh() }

        val newsList = newsJob.await()
        val newsSummary = if (newsList.isEmpty()) "新闻暂时无法获取"
        else newsList.joinToString("。") { it.title }

        Log.d(TAG, "prepare() weather=$weather")
        Log.d(TAG, "prepare() market=$marketSummary")
        Log.d(TAG, "prepare() news=$newsSummary")

        BroadcastContent(
            greeting = "早安，晨光播报开始。",
            passageName = passageName,
            weather = weather,
            bibleZh = bibleZh,
            bibleEn = bibleEn,
            marketSummary = marketSummary,
            newsSummary = newsSummary
        )
    }

    private suspend fun deliver(content: BroadcastContent, skipBible: Boolean = false) {
        safeSpeak(content.greeting, Language.ZH)
        safeSpeak(content.weather, Language.ZH)

        val actuallySkipBible = when {
            skipBible -> true
            content.passageName.isBlank() -> false
            else -> {
                safeSpeak("今天读经是${content.passageName}，请确认或跳过", Language.ZH)
                val result = speechEngine.listenForConfirmation(8_000L)
                Log.d(TAG, "Voice confirmation: $result")
                result == ConfirmationResult.Skipped
            }
        }

        if (!actuallySkipBible && content.bibleZh.isNotBlank()) {
            safeSpeak(content.bibleZh, Language.ZH)
            safeSpeak(content.bibleEn, Language.EN)
            safeSpeak("今日读经结束，接下来是财经播报。", Language.ZH)
        }
        safeSpeak("今日市场行情：", Language.ZH)
        safeSpeak(content.marketSummary, Language.ZH)
        safeSpeak("今日财经头条：", Language.ZH)
        safeSpeak(content.newsSummary, Language.ZH)
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
