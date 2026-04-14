package com.morninggrace.bible

import com.morninggrace.bible.db.BibleVerseDao
import com.morninggrace.bible.model.BiblePassage
import com.morninggrace.bible.model.BibleVerse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BibleRepositoryTest {

    private val dao = mockk<BibleVerseDao>()
    private val repo = BibleRepository(dao)

    @Test
    fun `getVersesForPassage returns verses from DAO for whole chapter`() = runTest {
        val passage = BiblePassage(book = 1, chapter = 1, verseStart = 1, verseEnd = -1)
        val verses = listOf(
            BibleVerse(1, 1, 1, "zh", "起初，神创造天地。"),
            BibleVerse(1, 1, 2, "zh", "地是空虚混沌，渊面黑暗；神的灵运行在水面上。")
        )
        coEvery { dao.getChapter(1, 1, "zh") } returns verses

        val result = repo.getVersesForPassage(passage, "zh")

        assertEquals(2, result.size)
        assertEquals("起初，神创造天地。", result[0].text)
    }

    @Test
    fun `getVersesForPassage fetches range when not whole chapter`() = runTest {
        val passage = BiblePassage(book = 40, chapter = 5, verseStart = 3, verseEnd = 5)
        val verses = listOf(
            BibleVerse(40, 5, 3, "en", "Blessed are the poor in spirit"),
            BibleVerse(40, 5, 4, "en", "Blessed are those who mourn"),
            BibleVerse(40, 5, 5, "en", "Blessed are the meek")
        )
        coEvery { dao.getVerses(40, 5, 3, 5, "en") } returns verses

        val result = repo.getVersesForPassage(passage, "en")

        assertEquals(3, result.size)
    }
}
