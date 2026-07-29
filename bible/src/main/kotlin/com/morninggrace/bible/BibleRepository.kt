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
        val verses = if (passage.isWholeChapter()) {
            dao.getChapter(passage.book, passage.chapter, lang)
        } else {
            dao.getVerses(passage.book, passage.chapter, passage.verseStart, passage.verseEnd, lang)
        }
        return normalizeChineseJohnBoundary(verses)
    }

    /**
     * The bundled CUNP source prints John 7:53 at the start of 8:1. Split the
     * sentence at query time so chapter playback and bilingual verse numbers align.
     */
    private fun normalizeChineseJohnBoundary(verses: List<BibleVerse>): List<BibleVerse> =
        verses.map { verse ->
            when {
                verse.lang == "zh" && verse.book == 43 &&
                    verse.chapter == 7 && verse.verse == 53 && verse.text.isBlank() ->
                    verse.copy(text = "于是各人都回家去了；")
                verse.lang == "zh" && verse.book == 43 &&
                    verse.chapter == 8 && verse.verse == 1 ->
                    verse.copy(
                        text = verse.text
                            .removePrefix("於是各人都回家去了；")
                            .removePrefix("于是各人都回家去了；")
                    )
                else -> verse
            }
        }
}
