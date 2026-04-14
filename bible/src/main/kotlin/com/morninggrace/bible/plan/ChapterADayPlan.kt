package com.morninggrace.bible.plan

import com.morninggrace.bible.model.BiblePassage
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Chapter-a-day plan starting from a user-specified book/chapter.
 * Advances one chapter per calendar day from the epoch. Wraps book→book, then loops.
 */
class ChapterADayPlan(
    startBook: Int = 1,
    startChapter: Int = 1,
    private val epoch: LocalDate = LocalDate.of(2026, 1, 1)
) : BibleReadingPlan {

    override val id = "chapteraday"
    override val nameZh = "每天一章"
    override val nameEn = "Chapter a Day"

    private val allChapters: List<BiblePassage> = SequentialPlan.ALL_CHAPTERS

    private val startIdx: Int = allChapters
        .indexOfFirst { it.book == startBook && it.chapter == startChapter }
        .takeIf { it >= 0 } ?: 0

    override fun getTotalDays() = allChapters.size

    override fun getReadingForDate(date: LocalDate): List<BiblePassage> {
        val daysSinceEpoch = ChronoUnit.DAYS.between(epoch, date).toInt()
        val idx = ((startIdx + daysSinceEpoch) % allChapters.size + allChapters.size) % allChapters.size
        return listOf(allChapters[idx])
    }
}
