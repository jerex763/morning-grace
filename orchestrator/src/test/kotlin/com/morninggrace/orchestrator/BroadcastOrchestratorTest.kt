package com.morninggrace.orchestrator

import com.morninggrace.bible.BibleRepository
import com.morninggrace.bible.audio.BibleAudioPlayer
import com.morninggrace.bible.model.BibleVerse
import com.morninggrace.bible.plan.McCheyneOnePlan
import com.morninggrace.core.model.BroadcastConfig
import com.morninggrace.core.model.Language
import com.morninggrace.core.model.LocationPrefs
import com.morninggrace.core.model.NewsHeadline
import com.morninggrace.core.repository.LocationRepository
import com.morninggrace.core.repository.NewsRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.tts.TtsEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BroadcastOrchestratorTest {

    private val ttsEngine = mockk<TtsEngine>(relaxed = true) {
        every { isAvailable() } returns true
    }
    private val bibleAudioPlayer = mockk<BibleAudioPlayer>(relaxed = true) {
        coEvery { playChapter(any(), any()) } returns false
    }
    private val bibleRepo = mockk<BibleRepository> {
        coEvery { getVersesForPassage(any(), "zh") } returns listOf(
            BibleVerse(1, 1, 1, "zh", "起初，神创造天地。")
        )
        coEvery { getVersesForPassage(any(), "en") } returns emptyList()
    }
    private val weatherRepo = mockk<WeatherRepository> {
        coEvery { getCurrentWeather(any(), any()) } returns null
    }
    private val newsRepo = mockk<NewsRepository> {
        coEvery { getTopHeadlines(any(), any()) } returns emptyList()
    }
    private val locationRepo = mockk<LocationRepository> {
        every { get() } returns LocationPrefs(-33.87, 151.21)
    }
    private val orchestrator = BroadcastOrchestrator(
        ttsEngine,
        bibleAudioPlayer,
        bibleRepo,
        McCheyneOnePlan(),
        weatherRepo,
        newsRepo,
        locationRepo
    )

    @Test
    fun `broadcast reads Bible automatically without confirmation`() = runTest {
        orchestrator.broadcast(
            LocalDate.of(2026, 1, 1),
            BroadcastConfig(skipWeather = true, skipNews = true)
        )

        coVerify(exactly = 4) {
            ttsEngine.speak(match { it.startsWith("现在读") }, Language.ZH)
        }
        coVerify(exactly = 4) {
            ttsEngine.speak("起初，神创造天地。", Language.ZH)
        }
        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    @Test
    fun `three news items speak official summaries by default`() = runTest {
        coEvery { newsRepo.getTopHeadlines(3, false) } returns (1..3).map {
            NewsHeadline(title = "标题$it", summary = "概述$it", fullContent = "全文$it")
        }

        orchestrator.broadcast(
            LocalDate.of(2026, 1, 1),
            BroadcastConfig(skipWeather = true, skipBible = true)
        )

        (1..3).forEach { index ->
            coVerify { ttsEngine.speak(match { it.contains("标题$index") }, Language.ZH) }
            coVerify { ttsEngine.speak("概述$index", Language.ZH) }
            coVerify(exactly = 0) { ttsEngine.speak("全文$index", Language.ZH) }
        }
    }

    @Test
    fun `full news mode speaks article content`() = runTest {
        coEvery { newsRepo.getTopHeadlines(3, true) } returns listOf(
            NewsHeadline(title = "标题", summary = "概述", fullContent = "完整正文")
        )

        orchestrator.broadcast(
            LocalDate.of(2026, 1, 1),
            BroadcastConfig(
                skipWeather = true,
                skipBible = true,
                newsFullArticles = true
            )
        )

        coVerify { ttsEngine.speak("完整正文", Language.ZH) }
        coVerify(exactly = 0) { ttsEngine.speak("概述", Language.ZH) }
    }

    @Test
    fun `recorded chapters replace Chinese TTS`() = runTest {
        coEvery { bibleAudioPlayer.playChapter(any(), any()) } returns true

        orchestrator.broadcast(
            LocalDate.of(2026, 1, 1),
            BroadcastConfig(skipWeather = true, skipNews = true)
        )

        coVerify(exactly = 4) { bibleAudioPlayer.playChapter(any(), any()) }
        coVerify(exactly = 0) {
            ttsEngine.speak("起初，神创造天地。", Language.ZH)
        }
    }

    @Test
    fun `long content is split below TTS limit at punctuation`() {
        val text = buildString {
            repeat(1_500) { append("这是一句话。") }
        }

        val chunks = orchestrator.splitForTts(text)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 2_800 })
        assertEquals(text, chunks.joinToString(""))
    }

    @Test
    fun `stop stops both audio engines`() {
        orchestrator.stop()

        io.mockk.verify { bibleAudioPlayer.stop() }
        io.mockk.verify { ttsEngine.stop() }
        assertEquals(BroadcastState.Idle, orchestrator.state)
    }
}
