package com.morninggrace.bible.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class SequentialPlanTest {

    private val plan = SequentialPlan()

    @Test
    fun `day 1 returns 1 passage`() {
        val passages = plan.getReadingForDate(LocalDate.of(2026, 1, 1))
        assertEquals(1, passages.size)
    }

    @Test
    fun `day 1 is Genesis 1`() {
        val p = plan.getReadingForDate(LocalDate.of(2026, 1, 1)).first()
        assertEquals(1, p.book)
        assertEquals(1, p.chapter)
    }

    @Test
    fun `after 1189 days wraps back to Genesis 1`() {
        val epoch = LocalDate.of(2026, 1, 1)
        val day1190 = epoch.plusDays(1189)
        val p = plan.getReadingForDate(day1190).first()
        assertEquals(1, p.book)
        assertEquals(1, p.chapter)
    }

    @Test
    fun `sequential days advance chapters`() {
        val epoch = LocalDate.of(2026, 1, 1)
        val p1 = plan.getReadingForDate(epoch).first()
        val p2 = plan.getReadingForDate(epoch.plusDays(1)).first()
        assertFalse(p1 == p2)
    }
}
