package com.morninggrace.orchestrator

import com.morninggrace.ai.ConversationManager
import com.morninggrace.bible.BibleRepository
import com.morninggrace.bible.model.BibleVerse
import com.morninggrace.bible.plan.McCheyneOnePlan
import com.morninggrace.core.model.LocationPrefs
import com.morninggrace.core.repository.FinanceRepository
import com.morninggrace.core.repository.LocationRepository
import com.morninggrace.core.repository.NewsRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.core.model.BroadcastConfig
import com.morninggrace.core.model.ConfirmationResult
import com.morninggrace.tts.SpeechEngine
import com.morninggrace.tts.TtsEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BroadcastOrchestratorTest {

    private val ttsEngine = mockk<TtsEngine>(relaxed = true)
    private val bibleRepo = mockk<BibleRepository>()
    private val plan = McCheyneOnePlan()
    private val weatherRepo = mockk<WeatherRepository> {
        coEvery { getCurrentWeather(any(), any()) } returns null
    }
    private val financeRepo = mockk<FinanceRepository> {
        coEvery { getMarketData() } returns emptyList()
    }
    private val newsRepo = mockk<NewsRepository> {
        coEvery { getTopHeadlines(any()) } returns emptyList()
    }
    private val locationRepo = mockk<LocationRepository> {
        every { get() } returns LocationPrefs(lat = -33.87, lon = 151.21)
    }
    private val speechEngine = mockk<SpeechEngine> {
        coEvery { listenForConfirmation(any()) } returns ConfirmationResult.Confirmed
        every { isAvailable() } returns true
    }
    private val conversationManager = mockk<ConversationManager>(relaxed = true)

    private val orchestrator = BroadcastOrchestrator(
        ttsEngine, bibleRepo, plan, weatherRepo, financeRepo, newsRepo, locationRepo,
        speechEngine, conversationManager
    )

    @Test
    fun `initial state is Idle`() {
        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    @Test
    fun `broadcast transitions through Preparing then back to Idle`() = runTest {
        coEvery { bibleRepo.getVersesForPassage(any(), "zh") } returns listOf(
            BibleVerse(1, 1, 1, "zh", "起初，神创造天地。")
        )
        coEvery { bibleRepo.getVersesForPassage(any(), "en") } returns listOf(
            BibleVerse(1, 1, 1, "en", "In the beginning God created the heavens.")
        )
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1))

        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    @Test
    fun `broadcast speaks at least 4 times`() = runTest {
        coEvery { bibleRepo.getVersesForPassage(any(), "zh") } returns listOf(
            BibleVerse(1, 1, 1, "zh", "起初，神创造天地。")
        )
        coEvery { bibleRepo.getVersesForPassage(any(), "en") } returns listOf(
            BibleVerse(1, 1, 1, "en", "In the beginning God created the heavens.")
        )
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1))

        coVerify(atLeast = 4) { ttsEngine.speak(any(), any()) }
    }

    @Test
    fun `broadcast handles empty bible verses gracefully`() = runTest {
        coEvery { bibleRepo.getVersesForPassage(any(), any()) } returns emptyList()
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1))

        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    @Test
    fun `stop transitions state to Idle`() {
        orchestrator.stop()
        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    @Test
    fun `skipBible config skips bible fetch and speak`() = runTest {
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1), BroadcastConfig(skipBible = true))

        coVerify(exactly = 0) { bibleRepo.getVersesForPassage(any(), any()) }
        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    @Test
    fun `all modules disabled still speaks greeting and farewell`() = runTest {
        every { ttsEngine.isAvailable() } returns true
        val config = BroadcastConfig(skipWeather = true, skipBible = true, skipFinance = true, skipNews = true)

        orchestrator.broadcast(LocalDate.of(2026, 1, 1), config)

        // greeting + farewell = at least 2 speaks
        coVerify(atLeast = 2) { ttsEngine.speak(any(), any()) }
        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    // ── weather module ──────────────────────────────────────────────────────

    @Test
    fun `skipWeather does not speak weather`() = runTest {
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1), BroadcastConfig(skipBible = true, skipWeather = true))

        coVerify(exactly = 0) { ttsEngine.speak(match { it.contains("天气") }, any()) }
    }

    @Test
    fun `weather enabled speaks weather fallback when unavailable`() = runTest {
        every { ttsEngine.isAvailable() } returns true

        // weatherRepo returns null by default → fallback "天气暂时无法获取"
        orchestrator.broadcast(LocalDate.of(2026, 1, 1), BroadcastConfig(skipBible = true))

        coVerify(atLeast = 1) { ttsEngine.speak(match { it.contains("天气") }, any()) }
    }

    // ── finance module ──────────────────────────────────────────────────────

    @Test
    fun `skipFinance does not speak market intro`() = runTest {
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1), BroadcastConfig(skipBible = true, skipFinance = true))

        coVerify(exactly = 0) { ttsEngine.speak(match { it.contains("市场行情") }, any()) }
    }

    @Test
    fun `finance enabled speaks market intro`() = runTest {
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1), BroadcastConfig(skipBible = true))

        coVerify(atLeast = 1) { ttsEngine.speak(match { it.contains("市场行情") }, any()) }
    }

    // ── news module ─────────────────────────────────────────────────────────

    @Test
    fun `skipNews does not speak headlines intro`() = runTest {
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1), BroadcastConfig(skipBible = true, skipNews = true))

        coVerify(exactly = 0) { ttsEngine.speak(match { it.contains("财经头条") }, any()) }
    }

    @Test
    fun `news enabled speaks headlines intro`() = runTest {
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1), BroadcastConfig(skipBible = true))

        coVerify(atLeast = 1) { ttsEngine.speak(match { it.contains("财经头条") }, any()) }
    }

    // ── bible voice confirmation ────────────────────────────────────────────

    @Test
    fun `voice skip suppresses bible reading`() = runTest {
        coEvery { bibleRepo.getVersesForPassage(any(), "zh") } returns listOf(
            BibleVerse(1, 1, 1, "zh", "起初，神创造天地。")
        )
        coEvery { bibleRepo.getVersesForPassage(any(), "en") } returns listOf(
            BibleVerse(1, 1, 1, "en", "In the beginning God created the heavens.")
        )
        coEvery { speechEngine.listenForConfirmation(any()) } returns ConfirmationResult.Skipped
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1))

        coVerify(exactly = 0) { ttsEngine.speak(match { it.contains("起初") }, any()) }
    }

    @Test
    fun `voice confirm reads bible`() = runTest {
        coEvery { bibleRepo.getVersesForPassage(any(), "zh") } returns listOf(
            BibleVerse(1, 1, 1, "zh", "起初，神创造天地。")
        )
        coEvery { bibleRepo.getVersesForPassage(any(), "en") } returns listOf(
            BibleVerse(1, 1, 1, "en", "In the beginning God created the heavens.")
        )
        coEvery { speechEngine.listenForConfirmation(any()) } returns ConfirmationResult.Confirmed
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1))

        coVerify(atLeast = 1) { ttsEngine.speak(match { it.contains("起初") }, any()) }
    }
}
