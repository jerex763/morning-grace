package com.morninggrace.bible.plan

import com.morninggrace.bible.model.BiblePassage
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Three-chapters-a-day plan starting from a user-specified book/chapter.
 * Advances three chapters per calendar day. Wraps book→book, then loops.
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
        val firstIdx = ((startIdx + daysSinceEpoch * CHAPTERS_PER_DAY) % allChapters.size + allChapters.size) % allChapters.size
        return List(CHAPTERS_PER_DAY) { offset ->
            allChapters[(firstIdx + offset) % allChapters.size]
        }
    }

    companion object {
        const val CHAPTERS_PER_DAY = 3
    }
}
