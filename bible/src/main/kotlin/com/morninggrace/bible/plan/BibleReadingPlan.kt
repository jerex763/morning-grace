package com.morninggrace.bible.plan

import com.morninggrace.bible.model.BiblePassage
import java.time.LocalDate

interface BibleReadingPlan {
    val id: String
    val nameZh: String
    val nameEn: String
    fun getReadingForDate(date: LocalDate): List<BiblePassage>
    fun getTotalDays(): Int
}
