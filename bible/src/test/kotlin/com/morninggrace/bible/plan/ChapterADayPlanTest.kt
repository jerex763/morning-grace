package com.morninggrace.bible.plan

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChapterADayPlanTest {

    @Test
    fun `starts at configured book and chapter`() {
        val plan = ChapterADayPlan(startBook = 19, startChapter = 1) // Psalms
        val readings = plan.getReadingForDate(LocalDate.of(2026, 1, 1))
        val p = readings.first()
        assertEquals(3, readings.size)
        assertEquals(19, p.book)
        assertEquals(1, p.chapter)
    }

    @Test
    fun `advances three chapters per day`() {
        val plan = ChapterADayPlan(startBook = 40, startChapter = 1) // Matthew
        val p = plan.getReadingForDate(LocalDate.of(2026, 1, 2)).first()
        assertEquals(40, p.book)
        assertEquals(4, p.chapter)
    }

    @Test
    fun `wraps to next book when chapters exhausted`() {
        // Day 10 reads Matthew 28, then continues into Mark 1 and 2.
        val plan = ChapterADayPlan(startBook = 40, startChapter = 1)
        val epoch = LocalDate.of(2026, 1, 1)
        val day10 = epoch.plusDays(9)
        val p = plan.getReadingForDate(day10)[1]
        assertEquals(41, p.book)
        assertEquals(1, p.chapter)
    }

    @Test
    fun `total days covers the whole Bible three chapters at a time`() {
        assertEquals(397, ChapterADayPlan().getTotalDays())
    }
}
