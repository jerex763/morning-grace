package com.morninggrace.bible.plan

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class McCheyneOnePlanTest {

    private val plan = McCheyneOnePlan()

    @Test
    fun `day 1 matches the classic MCheyne calendar`() {
        assertEquals(
            listOf("1:1", "40:1", "15:1", "44:1"),
            refs(LocalDate.of(2026, 1, 1))
        )
    }

    @Test
    fun `golden dates match the verified calendar`() {
        assertEquals(
            listOf("3:14", "19:17", "20:28", "53:2"),
            refs(LocalDate.of(2026, 4, 10))
        )
        assertEquals(
            listOf("7:2", "44:6", "24:15", "41:1"),
            refs(LocalDate.of(2026, 7, 19))
        )
        assertEquals(
            listOf("14:36", "66:22", "39:4", "43:21"),
            refs(LocalDate.of(2026, 12, 31))
        )
    }

    @Test
    fun `leap year day 366 wraps to day 1`() {
        assertEquals(
            refs(LocalDate.of(2024, 1, 1)),
            refs(LocalDate.of(2024, 12, 31))
        )
    }

    @Test
    fun `total days is 365`() {
        assertEquals(365, plan.getTotalDays())
    }

    @Test
    fun `calendar has all 365 non-empty days`() {
        val start = LocalDate.of(2026, 1, 1)
        assertEquals(365, (0L..364L).map { plan.getReadingForDate(start.plusDays(it)) }.size)
        (0L..364L).forEach { day ->
            check(plan.getReadingForDate(start.plusDays(day)).isNotEmpty())
        }
    }

    private fun refs(date: LocalDate): List<String> =
        plan.getReadingForDate(date).map { passage ->
            buildString {
                append("${passage.book}:${passage.chapter}")
                if (!passage.isWholeChapter()) {
                    append(":${passage.verseStart}-${passage.verseEnd}")
                }
            }
        }
}
