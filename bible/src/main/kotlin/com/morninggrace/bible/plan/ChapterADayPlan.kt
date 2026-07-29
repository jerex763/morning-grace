package com.morninggrace.bible.plan

import com.morninggrace.bible.model.BiblePassage
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Three-chapters-a-day plan starting from a user-specified book/chapter.
 * Advances up to three chapters per calendar day. The final day contains the
 * one remaining chapter, then the next day starts a new full-Bible cycle.
 */
class ChapterADayPlan(
    startBook: Int = 1,
    startChapter: Int = 1,
    private val epoch: LocalDate = LocalDate.of(2026, 1, 1)
) : BibleReadingPlan {

    override val id = "chapteraday"
    override val nameZh = "每天三章"
    override val nameEn = "Three Chapters a Day"

    private val allChapters: List<BiblePassage> = SequentialPlan.ALL_CHAPTERS

    private val startIdx: Int = allChapters
        .indexOfFirst { it.book == startBook && it.chapter == startChapter }
        .takeIf { it >= 0 } ?: 0

    override fun getTotalDays() = (allChapters.size + CHAPTERS_PER_DAY - 1) / CHAPTERS_PER_DAY

    override fun getReadingForDate(date: LocalDate): List<BiblePassage> {
        val daysSinceEpoch = ChronoUnit.DAYS.between(epoch, date).toInt()
        val dayInCycle = Math.floorMod(daysSinceEpoch, getTotalDays())
        val consumed = dayInCycle * CHAPTERS_PER_DAY
        val count = minOf(CHAPTERS_PER_DAY, allChapters.size - consumed)
        val firstIdx = (startIdx + consumed) % allChapters.size
        return List(count) { offset ->
            allChapters[(firstIdx + offset) % allChapters.size]
        }
    }

    companion object {
        const val CHAPTERS_PER_DAY = 3
    }
}
