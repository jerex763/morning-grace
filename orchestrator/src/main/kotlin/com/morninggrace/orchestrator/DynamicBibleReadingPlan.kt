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

/**
 * Dispatches to the reading plan selected in SharedPreferences.
 *
 * McCheyne and Sequential are calendar-driven (pure functions of the date).
 * "Chapter a Day" is progress-driven: it reads whatever chapter [KEY_CHAPTER_INDEX]
 * points at and only moves forward when [advanceProgress] is called after a reading
 * is actually delivered — so a skipped or missed morning does not skip a chapter.
 * [chapterADay] is retained only for its id/name metadata; its own calendar logic is
 * not used for reading selection.
 */
@Singleton
class DynamicBibleReadingPlan @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mcCheyne: McCheyneOnePlan,
    private val sequential: SequentialPlan,
    private val chapterADay: ChapterADayPlan
) : BibleReadingPlan {

    private fun currentId(): String =
        prefs().getString(KEY, ID_MCCHEYNE) ?: ID_MCCHEYNE

    private val current: BibleReadingPlan
        get() = when (currentId()) {
            ID_SEQUENTIAL    -> sequential
            ID_CHAPTER_A_DAY -> chapterADay
            else             -> mcCheyne
        }

    override val id: String      get() = current.id
    override val nameZh: String  get() = current.nameZh
    override val nameEn: String  get() = current.nameEn
    override fun getTotalDays(): Int = current.getTotalDays()

    override fun getReadingForDate(date: LocalDate): List<BiblePassage> =
        if (currentId() == ID_CHAPTER_A_DAY)
            listOf(SequentialPlan.ALL_CHAPTERS[progressIndex()])
        else
            current.getReadingForDate(date)

    override fun advanceProgress() {
        if (currentId() != ID_CHAPTER_A_DAY) return
        val size = SequentialPlan.ALL_CHAPTERS.size
        val next = (progressIndex() + 1) % size
        prefs().edit().putInt(KEY_CHAPTER_INDEX, next).apply()
    }

    private fun prefs() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun progressIndex(): Int {
        val size = SequentialPlan.ALL_CHAPTERS.size
        val raw = prefs().getInt(KEY_CHAPTER_INDEX, 0)
        return ((raw % size) + size) % size
    }

    companion object {
        const val PREFS             = "alarm_prefs"
        const val KEY               = "reading_plan"
        const val KEY_CHAPTER_INDEX = "chapter_progress_index"
        const val ID_MCCHEYNE       = "mccheyneone"
        const val ID_SEQUENTIAL     = "sequential"
        const val ID_CHAPTER_A_DAY  = "chapteraday"
    }
}
