package com.morninggrace.orchestrator

import android.content.Context
import android.content.SharedPreferences
import com.morninggrace.bible.plan.ChapterADayPlan
import com.morninggrace.bible.plan.McCheyneOnePlan
import com.morninggrace.bible.plan.SequentialPlan
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DynamicBibleReadingPlanTest {

    private val prefs = mockk<SharedPreferences>()
    private val context = mockk<Context> {
        every { getSharedPreferences(any(), any()) } returns prefs
    }

    private val mcCheyne    = McCheyneOnePlan()
    private val sequential  = SequentialPlan()
    private val chapterADay = ChapterADayPlan()

    private val plan = DynamicBibleReadingPlan(context, mcCheyne, sequential, chapterADay)

    private val date = LocalDate.of(2026, 1, 1)

    @Before
    fun setUp() {
        // Default: no preference set → falls back to McCheyne
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns null
        every { prefs.getInt(any(), any()) } answers { secondArg() }
        every { prefs.contains(any()) } returns false
    }

    @Test fun `defaults to McCheyne when no preference set`() {
        assertEquals(DynamicBibleReadingPlan.ID_MCCHEYNE, plan.id)
    }

    @Test fun `selects sequential plan`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_SEQUENTIAL
        assertEquals(DynamicBibleReadingPlan.ID_SEQUENTIAL, plan.id)
    }

    @Test fun `selects chapter-a-day plan`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
        assertEquals(DynamicBibleReadingPlan.ID_CHAPTER_A_DAY, plan.id)
    }

    @Test fun `unknown preference falls back to McCheyne`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns "bogus_plan"
        assertEquals(DynamicBibleReadingPlan.ID_MCCHEYNE, plan.id)
    }

    @Test fun `McCheyne returns 4 passages per day`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_MCCHEYNE
        assertEquals(4, plan.getReadingForDate(date).size)
    }

    @Test fun `McCheyne exposes calendar day as default progress`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_MCCHEYNE
        assertEquals(219, plan.getCurrentDay(LocalDate.of(2026, 8, 7)))
    }

    @Test fun `sequential returns 1 passage per day`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_SEQUENTIAL
        assertEquals(1, plan.getReadingForDate(date).size)
    }

    @Test fun `chapter-a-day returns 3 passages per day`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
        assertEquals(3, plan.getReadingForDate(date).size)
    }

    @Test fun `chapter-a-day uses saved starting book and chapter`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
        every { prefs.getInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_BOOK, 1) } returns 19
        every { prefs.getInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_CHAPTER, 1) } returns 23

        val readings = plan.getReadingForDate(date)
        assertEquals(3, readings.size)
        val passage = readings.first()
        assertEquals(19, passage.book)
        assertEquals(23, passage.chapter)
    }

    @Test fun `delegates nameZh to active plan`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_SEQUENTIAL
        assertEquals(sequential.nameZh, plan.nameZh)
    }

    @Test fun `plan switches dynamically without recreating`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_SEQUENTIAL
        val passagesSequential = plan.getReadingForDate(date)

        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_MCCHEYNE
        val passagesMcCheyne = plan.getReadingForDate(date)

        assertEquals(1, passagesSequential.size)
        assertEquals(4, passagesMcCheyne.size)
    }
}
