package com.morninggrace.orchestrator

import android.util.Log
import com.morninggrace.bible.BibleRepository
import com.morninggrace.bible.audio.BibleAudioPlayer
import com.morninggrace.bible.plan.BibleReadingPlan
import com.morninggrace.bible.toChineseTitle
import com.morninggrace.core.model.BroadcastConfig
import com.morninggrace.core.model.Language
import com.morninggrace.core.model.TimeGreeting
import com.morninggrace.core.repository.LocationRepository
import com.morninggrace.core.repository.NewsRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.tts.TtsEngine
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private const val TAG = "MorningGrace"
private const val MAX_TTS_CHUNK = 2_800

class BroadcastOrchestrator @Inject constructor(
    private val ttsEngine: TtsEngine,
    private val bibleAudioPlayer: BibleAudioPlayer,
    private val bibleRepo: BibleRepository,
    private val readingPlan: BibleReadingPlan,
    private val weatherRepo: WeatherRepository,
    private val newsRepo: NewsRepository,
    private val locationRepo: LocationRepository
) {

    var state: BroadcastState = BroadcastState.Idle
        private set

    suspend fun broadcast(
        date: LocalDate = LocalDate.now(),
        config: BroadcastConfig = BroadcastConfig(),
        time: LocalTime = LocalTime.now()
    ) {
        val effectiveConfig = if (config.offline) {
            config.copy(skipWeather = true, skipNews = true)
        } else {
            config
        }
        var waited = 0
        while (!ttsEngine.isAvailable() && waited < 30) {
            delay(100)
            waited++
        }
        state = BroadcastState.Preparing
        try {
            val content = prepare(date, effectiveConfig, time)
            state = BroadcastState.Broadcasting(content)
            deliver(content, effectiveConfig)
        } catch (error: Exception) {
            Log.e(TAG, "broadcast() failed", error)
            throw error
        } finally {
            state = BroadcastState.Idle
        }
    }

    fun stop() {
        bibleAudioPlayer.stop()
        ttsEngine.stop()
        state = BroadcastState.Idle
    }

    private suspend fun prepare(
        date: LocalDate,
        config: BroadcastConfig,
        time: LocalTime
    ): BroadcastContent = coroutineScope {
        val location = locationRepo.get()
        val weatherJob = if (!config.skipWeather) {
            async { weatherRepo.getCurrentWeather(location) }
        } else {
            null
        }
        val newsJob = if (!config.skipNews) {
            async { newsRepo.getTopHeadlines(3, config.newsFullArticles) }
        } else {
            null
        }

        val passages = if (!config.skipBible) {
            readingPlan.getReadingForDate(date)
        } else {
            emptyList()
        }
        val readings = passages.map { passage ->
            PassageReading(
                book = passage.book,
                chapter = passage.chapter,
                isWholeChapter = passage.isWholeChapter(),
                titleZh = passage.toChineseTitle(),
                zh = bibleRepo.getVersesForPassage(passage, "zh")
                    .joinToString(" ") { it.text }
                    .ifBlank { "今日经文暂不可用" },
                en = if (config.includeEnglishBible) {
                    bibleRepo.getVersesForPassage(passage, "en")
                        .joinToString(" ") { it.text }
                        .ifBlank { "Bible reading unavailable" }
                } else {
                    ""
                }
            )
        }

        val weather = weatherJob?.await()?.toSpeechZh()
            ?: if (config.skipWeather) "" else "天气暂时无法获取"
        val news = newsJob?.await().orEmpty().map {
            NewsReading(
                title = it.title,
                content = if (config.newsFullArticles) {
                    it.fullContent.ifBlank { it.summary }
                } else {
                    it.summary
                }.ifBlank { "暂时无法取得这条新闻的概述。" }
            )
        }

        BroadcastContent(
            greeting = "${TimeGreeting.forTime(time)}，晨光播报开始。",
            passageName = passages.joinToString("、") { it.toChineseTitle() },
            weather = weather,
            passages = readings,
            news = news
        )
    }

    private suspend fun deliver(content: BroadcastContent, config: BroadcastConfig) {
        safeSpeak(content.greeting, Language.ZH)

        if (config.offline) {
            safeSpeak("当前没有网络，今天跳过天气和新闻。", Language.ZH)
        }

        if (!config.skipWeather) {
            safeSpeak(content.weather, Language.ZH)
        }

        // Elder-friendly flow: announce the plan once, then continue without voice confirmation.
        if (!config.skipBible && content.passages.isNotEmpty()) {
            safeSpeak("今天读经是${content.passageName}。现在开始读经。", Language.ZH)
            for (passage in content.passages) {
                safeSpeak("现在读${passage.titleZh}。", Language.ZH)
                val playedRecording = config.preferRecordedBible &&
                    passage.isWholeChapter &&
                    safePlayRecordedChapter(passage.book, passage.chapter)
                if (!playedRecording) {
                    safeSpeakLong(passage.zh, Language.ZH)
                }
                if (config.includeEnglishBible && passage.en.isNotBlank()) {
                    safeSpeakLong(passage.en, Language.EN)
                }
            }
            if (!config.skipNews) {
                safeSpeak("今日读经结束。", Language.ZH)
            }
        }

        if (!config.skipNews) {
            if (content.news.isEmpty()) {
                safeSpeak("今日新闻暂时无法获取。", Language.ZH)
            } else {
                safeSpeak(
                    if (config.newsFullArticles) {
                        "下面播报今日三条要闻全文。"
                    } else {
                        "下面播报今日三条要闻概述。"
                    },
                    Language.ZH
                )
                val ordinalNames = arrayOf("第一条", "第二条", "第三条")
                content.news.forEachIndexed { index, item ->
                    val ordinal = ordinalNames.getOrElse(index) { "下一条" }
                    safeSpeak("$ordinal，${item.title}。", Language.ZH)
                    safeSpeakLong(item.content, Language.ZH)
                }
            }
        }

        safeSpeak("晨光播报结束，愿你今天蒙福。", Language.ZH)
    }

    private suspend fun safeSpeakLong(text: String, language: Language) {
        splitForTts(text).forEach { safeSpeak(it, language) }
    }

    internal fun splitForTts(text: String): List<String> {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= MAX_TTS_CHUNK) return listOf(clean).filter { it.isNotBlank() }

        val chunks = mutableListOf<String>()
        var remaining = clean
        while (remaining.isNotBlank()) {
            if (remaining.length <= MAX_TTS_CHUNK) {
                chunks += remaining
                break
            }
            val window = remaining.take(MAX_TTS_CHUNK)
            val cut = listOf('。', '！', '？', '；', '.', '!', '?')
                .maxOfOrNull { window.lastIndexOf(it) }
                ?.takeIf { it >= MAX_TTS_CHUNK / 2 }
                ?.plus(1)
                ?: MAX_TTS_CHUNK
            chunks += remaining.take(cut).trim()
            remaining = remaining.drop(cut).trim()
        }
        return chunks
    }

    private suspend fun safeSpeak(text: String, language: Language) {
        if (text.isBlank()) return
        require(text.length <= MAX_TTS_CHUNK)
        runCatching { ttsEngine.speak(text, language) }
            .onFailure { error ->
                Log.e(TAG, "speak failed: ${error.message}")
                if (error is CancellationException) throw error
            }
    }

    private suspend fun safePlayRecordedChapter(book: Int, chapter: Int): Boolean =
        runCatching { bibleAudioPlayer.playChapter(book, chapter) }
            .onFailure { error ->
                Log.e(TAG, "recorded Bible playback failed for $book:$chapter", error)
                if (error is CancellationException) throw error
            }
            .getOrDefault(false)
}
