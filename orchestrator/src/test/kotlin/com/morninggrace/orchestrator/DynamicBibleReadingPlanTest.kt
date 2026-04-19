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

    @Test fun `sequential returns 1 passage per day`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_SEQUENTIAL
        assertEquals(1, plan.getReadingForDate(date).size)
    }

    @Test fun `chapter-a-day returns 1 passage per day`() {
        every { prefs.getString(DynamicBibleReadingPlan.KEY, any()) } returns DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
        assertEquals(1, plan.getReadingForDate(date).size)
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
