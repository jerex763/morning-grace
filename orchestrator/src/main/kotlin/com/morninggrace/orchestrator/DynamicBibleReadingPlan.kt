package com.morninggrace.orchestrator

import android.content.Context
import com.morninggrace.bible.model.BiblePassage
import com.morninggrace.bible.plan.BibleReadingPlan
import com.morninggrace.bible.plan.ChapterADayPlan
import com.morninggrace.bible.plan.McCheyneOnePlan
import com.morninggrace.bible.plan.SequentialPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBibleReadingPlan @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mcCheyne: McCheyneOnePlan,
    private val sequential: SequentialPlan,
    private val chapterADay: ChapterADayPlan
) : BibleReadingPlan {

    private val current: BibleReadingPlan
        get() {
            val id = context
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, ID_MCCHEYNE)
            return when (id) {
                ID_SEQUENTIAL   -> sequential
                ID_CHAPTER_A_DAY -> chapterADay
                else            -> mcCheyne
            }
        }

    override val id: String      get() = current.id
    override val nameZh: String  get() = current.nameZh
    override val nameEn: String  get() = current.nameEn

    override fun getReadingForDate(date: LocalDate): List<BiblePassage> = current.getReadingForDate(date)
    override fun getTotalDays(): Int = current.getTotalDays()

    companion object {
        const val PREFS             = "alarm_prefs"
        const val KEY               = "reading_plan"
        const val ID_MCCHEYNE       = "mccheyneone"
        const val ID_SEQUENTIAL     = "sequential"
        const val ID_CHAPTER_A_DAY  = "chapteraday"
    }
}
