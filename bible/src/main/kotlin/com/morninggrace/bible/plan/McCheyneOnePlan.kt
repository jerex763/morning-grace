package com.morninggrace.bible.plan

import com.morninggrace.bible.model.BiblePassage
import java.time.LocalDate
import javax.inject.Inject

/**
 * McCheyne One-Year reading plan.
 * 4 passages per day (2 OT streams + 2 NT streams).
 * Day index is 1-based (Jan 1 = day 1). Leap year day 366 wraps to day 1.
 */
class McCheyneOnePlan @Inject constructor() : BibleReadingPlan {

    override val id = "mccheyneone"
    override val nameZh = "麦大卫一年读经"
    override val nameEn = "McCheyne One-Year Plan"

    override fun getTotalDays() = 365

    override fun getReadingForDate(date: LocalDate): List<BiblePassage> {
        val dayOfYear = date.dayOfYear
        val day = if (dayOfYear > 365) 1 else dayOfYear
        val idx = day - 1
        return listOf(
            STREAM_OT1[idx % STREAM_OT1.size],
            STREAM_OT2[idx % STREAM_OT2.size],
            STREAM_NT1[idx % STREAM_NT1.size],
            STREAM_NT2[idx % STREAM_NT2.size],
        )
    }

    companion object {
        // OT Stream 1: Genesis(1) to Esther(17)
        val STREAM_OT1: List<BiblePassage> = buildList {
            val books = listOf(1 to 50, 2 to 40, 3 to 27, 4 to 36, 5 to 34,
                6 to 24, 7 to 21, 8 to 4, 9 to 31, 10 to 24,
                11 to 22, 12 to 25, 13 to 29, 14 to 36, 15 to 10, 16 to 13, 17 to 10)
            for ((book, chapters) in books)
                for (ch in 1..chapters)
                    add(BiblePassage(book, ch, 1, -1))
        }

        // OT Stream 2: Job(18) to Malachi(39)
        val STREAM_OT2: List<BiblePassage> = buildList {
            val books = listOf(18 to 42, 19 to 150, 20 to 31, 21 to 12, 22 to 8,
                23 to 66, 24 to 52, 25 to 5, 26 to 48, 27 to 14,
                28 to 3, 29 to 9, 30 to 4, 31 to 7, 32 to 3,
                33 to 9, 34 to 1, 35 to 4, 36 to 3, 37 to 9,
                38 to 1, 39 to 4)
            for ((book, chapters) in books)
                for (ch in 1..chapters)
                    add(BiblePassage(book, ch, 1, -1))
        }

        // NT Stream 1: Matthew(40) to Acts(44)
        val STREAM_NT1: List<BiblePassage> = buildList {
            val books = listOf(40 to 28, 41 to 16, 42 to 24, 43 to 21, 44 to 28)
            for ((book, chapters) in books)
                for (ch in 1..chapters)
                    add(BiblePassage(book, ch, 1, -1))
        }

        // NT Stream 2: Romans(45) to Revelation(66)
        val STREAM_NT2: List<BiblePassage> = buildList {
            val books = listOf(45 to 16, 46 to 16, 47 to 13, 48 to 6, 49 to 6,
                50 to 4, 51 to 4, 52 to 5, 53 to 3, 54 to 6,
                55 to 4, 56 to 3, 57 to 1, 58 to 13, 59 to 5,
                60 to 5, 61 to 3, 62 to 5, 63 to 1, 64 to 1,
                65 to 1, 66 to 22)
            for ((book, chapters) in books)
                for (ch in 1..chapters)
                    add(BiblePassage(book, ch, 1, -1))
        }
    }
}
