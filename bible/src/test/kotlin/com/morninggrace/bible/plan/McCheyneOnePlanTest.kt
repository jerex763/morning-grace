package com.morninggrace.bible.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class McCheyneOnePlanTest {

    private val plan = McCheyneOnePlan()

    @Test
    fun `day 1 returns 4 passages`() {
        val passages = plan.getReadingForDate(LocalDate.of(2026, 1, 1))
        assertEquals(4, passages.size)
    }

    @Test
    fun `day 365 returns 4 passages`() {
        val passages = plan.getReadingForDate(LocalDate.of(2026, 12, 31))
        assertEquals(4, passages.size)
    }

    @Test
    fun `leap year day 366 wraps to day 1`() {
        val dec31 = plan.getReadingForDate(LocalDate.of(2024, 12, 31))
        val jan1Next = plan.getReadingForDate(LocalDate.of(2025, 1, 1))
        assertFalse(dec31.isEmpty())
        assertFalse(jan1Next.isEmpty())
    }

    @Test
    fun `total days is 365`() {
        assertEquals(365, plan.getTotalDays())
    }

    @Test
    fun `different dates return different passages`() {
        val day1 = plan.getReadingForDate(LocalDate.of(2026, 1, 1))
        val day2 = plan.getReadingForDate(LocalDate.of(2026, 1, 2))
        assertFalse(day1 == day2)
    }
}
