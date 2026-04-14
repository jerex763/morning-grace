package com.morninggrace.bible.plan

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChapterADayPlanTest {

    @Test
    fun `starts at configured book and chapter`() {
        val plan = ChapterADayPlan(startBook = 19, startChapter = 1) // Psalms
        val p = plan.getReadingForDate(LocalDate.of(2026, 1, 1)).first()
        assertEquals(19, p.book)
        assertEquals(1, p.chapter)
    }

    @Test
    fun `advances one chapter per day`() {
        val plan = ChapterADayPlan(startBook = 40, startChapter = 1) // Matthew
        val p = plan.getReadingForDate(LocalDate.of(2026, 1, 2)).first()
        assertEquals(40, p.book)
        assertEquals(2, p.chapter)
    }

    @Test
    fun `wraps to next book when chapters exhausted`() {
        // Matthew has 28 chapters; day 29 from epoch should be Mark ch 1 (book 41)
        val plan = ChapterADayPlan(startBook = 40, startChapter = 1)
        val epoch = LocalDate.of(2026, 1, 1)
        val day29 = epoch.plusDays(28)
        val p = plan.getReadingForDate(day29).first()
        assertEquals(41, p.book)
        assertEquals(1, p.chapter)
    }
}
