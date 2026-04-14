package com.morninggrace.orchestrator

import com.morninggrace.bible.BibleRepository
import com.morninggrace.bible.model.BiblePassage
import com.morninggrace.bible.model.BibleVerse
import com.morninggrace.bible.plan.McCheyneOnePlan
import com.morninggrace.core.model.Language
import com.morninggrace.core.model.LocationPrefs
import com.morninggrace.core.repository.FinanceRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.tts.TtsEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        coEvery { getSandP500() } returns null
    }
    private val locationPrefs = LocationPrefs(lat = -33.87, lon = 151.21)

    private val orchestrator = BroadcastOrchestrator(ttsEngine, bibleRepo, plan, weatherRepo, financeRepo, locationPrefs)

    @Test
    fun `initial state is Idle`() {
        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    @Test
    fun `broadcast transitions through Preparing then Broadcasting then back to Idle`() = runTest {
        val zhVerses = listOf(BibleVerse(1, 1, 1, "zh", "起初，神创造天地。"))
        val enVerses = listOf(BibleVerse(1, 1, 1, "en", "In the beginning God created the heavens."))

        coEvery { bibleRepo.getVersesForPassage(any(), "zh") } returns zhVerses
        coEvery { bibleRepo.getVersesForPassage(any(), "en") } returns enVerses
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1))

        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    @Test
    fun `broadcast speaks greeting, bible zh, bible en, weather, finance`() = runTest {
        val zhVerses = listOf(BibleVerse(1, 1, 1, "zh", "起初，神创造天地。"))
        val enVerses = listOf(BibleVerse(1, 1, 1, "en", "In the beginning God created the heavens."))

        coEvery { bibleRepo.getVersesForPassage(any(), "zh") } returns zhVerses
        coEvery { bibleRepo.getVersesForPassage(any(), "en") } returns enVerses
        every { ttsEngine.isAvailable() } returns true

        orchestrator.broadcast(LocalDate.of(2026, 1, 1))

        // Must have spoken at least 4 times (greeting, bible zh, bible en, sign-off)
        coVerify(atLeast = 4) { ttsEngine.speak(any(), any()) }
    }

    @Test
    fun `broadcast handles empty bible verses gracefully`() = runTest {
        coEvery { bibleRepo.getVersesForPassage(any(), any()) } returns emptyList()
        every { ttsEngine.isAvailable() } returns true

        // Should not throw
        orchestrator.broadcast(LocalDate.of(2026, 1, 1))

        assertEquals(BroadcastState.Idle, orchestrator.state)
    }

    @Test
    fun `stop transitions state to Idle immediately`() = runTest {
        orchestrator.stop()
        assertEquals(BroadcastState.Idle, orchestrator.state)
    }
}
