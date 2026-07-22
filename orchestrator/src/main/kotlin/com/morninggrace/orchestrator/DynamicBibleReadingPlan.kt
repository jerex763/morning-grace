package com.morninggrace.orchestrator

import android.content.Context
import com.morninggrace.bible.model.BiblePassage
import com.morninggrace.bible.plan.BibleReadingPlan
import com.morninggrace.bible.plan.ChapterADayPlan
import com.morninggrace.bible.plan.McCheyneOnePlan
import com.morninggrace.bible.plan.SequentialPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val id = prefs.getString(KEY, ID_MCCHEYNE)
            return when (id) {
                ID_SEQUENTIAL   -> sequential
                ID_CHAPTER_A_DAY -> ChapterADayPlan(
                    startBook = prefs.getInt(KEY_CHAPTER_A_DAY_BOOK, 1),
                    startChapter = prefs.getInt(KEY_CHAPTER_A_DAY_CHAPTER, 1)
                )
                else            -> mcCheyne
            }
        }

    override val id: String      get() = current.id
    override val nameZh: String  get() = current.nameZh
    override val nameEn: String  get() = current.nameEn

    override fun getReadingForDate(date: LocalDate): List<BiblePassage> {
        val plan = current
        return plan.getReadingForDate(referenceDateForDay(plan.id, getCurrentDay(date)))
    }

    override fun getTotalDays(): Int = current.getTotalDays()

    /** The user's active plan day on [date], advancing automatically from the saved anchor. */
    fun getCurrentDay(date: LocalDate = LocalDate.now()): Int {
        val plan = current
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val dayKey = progressDayKey(plan.id)
        val dateKey = progressDateKey(plan.id)
        if (!prefs.contains(dayKey) || !prefs.contains(dateKey)) {
            return naturalDay(plan.id, plan.getTotalDays(), date)
        }
        val anchorDay = prefs.getInt(dayKey, 1)
        val anchorDate = LocalDate.ofEpochDay(prefs.getLong(dateKey, date.toEpochDay()))
        val elapsed = ChronoUnit.DAYS.between(anchorDate, date).toInt()
        return wrapDay(anchorDay + elapsed, plan.getTotalDays())
    }

    /** Selects [day] for [date]. Future dates continue from this point automatically. */
    fun setCurrentDay(day: Int, date: LocalDate = LocalDate.now()) {
        val plan = current
        require(day in 1..plan.getTotalDays())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(progressDayKey(plan.id), day)
            .putLong(progressDateKey(plan.id), date.toEpochDay())
            .apply()
    }

    private fun naturalDay(planId: String, totalDays: Int, date: LocalDate): Int =
        if (planId == ID_MCCHEYNE) {
            if (date.dayOfYear > 365) 1 else date.dayOfYear
        } else {
            val elapsed = ChronoUnit.DAYS.between(REFERENCE_EPOCH, date).toInt()
            wrapDay(elapsed + 1, totalDays)
        }

    private fun referenceDateForDay(planId: String, day: Int): LocalDate =
        if (planId == ID_MCCHEYNE) MCCHEYNE_REFERENCE.plusDays((day - 1).toLong())
        else REFERENCE_EPOCH.plusDays((day - 1).toLong())

    private fun wrapDay(day: Int, totalDays: Int): Int =
        ((day - 1) % totalDays + totalDays) % totalDays + 1

    private fun progressDayKey(planId: String) = "${KEY_PROGRESS_DAY}_$planId"
    private fun progressDateKey(planId: String) = "${KEY_PROGRESS_DATE}_$planId"

    companion object {
        const val PREFS             = "alarm_prefs"
        const val KEY               = "reading_plan"
        const val KEY_PROGRESS_DAY  = "reading_progress_day"
        const val KEY_PROGRESS_DATE = "reading_progress_date"
        const val KEY_CHAPTER_A_DAY_BOOK = "chapter_a_day_start_book"
        const val KEY_CHAPTER_A_DAY_CHAPTER = "chapter_a_day_start_chapter"
        const val ID_MCCHEYNE       = "mccheyneone"
        const val ID_SEQUENTIAL     = "sequential"
        const val ID_CHAPTER_A_DAY  = "chapteraday"

        private val MCCHEYNE_REFERENCE = LocalDate.of(2025, 1, 1)
        private val REFERENCE_EPOCH = LocalDate.of(2026, 1, 1)
    }
}
