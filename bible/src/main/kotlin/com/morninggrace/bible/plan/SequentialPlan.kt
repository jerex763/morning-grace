package com.morninggrace.bible.plan

import com.morninggrace.bible.model.BiblePassage
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Sequential plan: one chapter per day, Genesis → Revelation, looping.
 * epoch = 2026-01-01 maps to Genesis 1 (index 0).
 */
class SequentialPlan(
    private val epoch: LocalDate = LocalDate.of(2026, 1, 1)
) : BibleReadingPlan {

    override val id = "sequential"
    override val nameZh = "顺序读经"
    override val nameEn = "Sequential Plan"

    override fun getTotalDays() = ALL_CHAPTERS.size

    override fun getReadingForDate(date: LocalDate): List<BiblePassage> {
        val daysSinceEpoch = ChronoUnit.DAYS.between(epoch, date).toInt()
        val idx = ((daysSinceEpoch % ALL_CHAPTERS.size) + ALL_CHAPTERS.size) % ALL_CHAPTERS.size
        return listOf(ALL_CHAPTERS[idx])
    }

    companion object {
        // Protestant 66-book canon chapter counts (OT 39 books, NT 27 books)
        private val BOOK_CHAPTER_COUNTS = listOf(
            // OT (books 1–39)
            50, 40, 27, 36, 34, 24, 21, 4, 31, 24,
            22, 25, 29, 36, 10, 13, 10, 42, 150, 31,
            12, 8, 66, 52, 5, 48, 12, 14, 3, 9,
            1, 4, 7, 3, 3, 3, 2, 14, 4,
            // NT (books 40–66)
            28, 16, 24, 21, 28, 16, 16, 13, 6, 6,
            4, 4, 5, 3, 6, 4, 3, 1, 13, 5,
            5, 3, 5, 1, 1, 1, 22
        )

        val ALL_CHAPTERS: List<BiblePassage> = buildList {
            BOOK_CHAPTER_COUNTS.forEachIndexed { bookIdx, chapters ->
                val book = bookIdx + 1
                for (ch in 1..chapters) add(BiblePassage(book, ch, 1, -1))
            }
        }
    }
}
