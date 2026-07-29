package com.morninggrace.bible.plan

import com.morninggrace.bible.model.BiblePassage
import java.time.LocalDate
import javax.inject.Inject

/** Robert Murray M'Cheyne's verified 365-day reading calendar. */
class McCheyneOnePlan @Inject constructor() : BibleReadingPlan {

    override val id = "mccheyneone"
    override val nameZh = "麦大卫一年读经"
    override val nameEn = "McCheyne One-Year Plan"

    override fun getTotalDays() = 365

    override fun getReadingForDate(date: LocalDate): List<BiblePassage> {
        val index = if (date.dayOfYear == 366) 0 else date.dayOfYear - 1
        return McCheynePlanData.days[index]
    }
}
