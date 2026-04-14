package com.morninggrace.bible

import com.morninggrace.bible.db.BibleVerseDao
import com.morninggrace.bible.model.BiblePassage
import com.morninggrace.bible.model.BibleVerse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleRepository @Inject constructor(
    private val dao: BibleVerseDao
) {
    suspend fun getVersesForPassage(passage: BiblePassage, lang: String): List<BibleVerse> {
        return if (passage.isWholeChapter()) {
            dao.getChapter(passage.book, passage.chapter, lang)
        } else {
            dao.getVerses(passage.book, passage.chapter, passage.verseStart, passage.verseEnd, lang)
        }
    }
}
