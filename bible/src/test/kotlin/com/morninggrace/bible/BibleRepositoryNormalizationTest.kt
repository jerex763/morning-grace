package com.morninggrace.bible

import com.morninggrace.bible.db.BibleVerseDao
import com.morninggrace.bible.model.BiblePassage
import com.morninggrace.bible.model.BibleVerse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BibleRepositoryNormalizationTest {

    private val dao = mockk<BibleVerseDao>()
    private val repository = BibleRepository(dao)

    @Test
    fun `John 7 53 is restored from the CUNP combined verse`() = runTest {
        coEvery { dao.getChapter(43, 7, "zh") } returns listOf(
            BibleVerse(43, 7, 53, "zh", "")
        )

        val result = repository.getVersesForPassage(BiblePassage(43, 7), "zh")

        assertEquals("于是各人都回家去了；", result.single().text)
    }

    @Test
    fun `John 8 1 no longer repeats John 7 53`() = runTest {
        coEvery { dao.getChapter(43, 8, "zh") } returns listOf(
            BibleVerse(43, 8, 1, "zh", "於是各人都回家去了；耶穌卻往橄欖山去，")
        )

        val result = repository.getVersesForPassage(BiblePassage(43, 8), "zh")

        assertEquals("耶穌卻往橄欖山去，", result.single().text)
    }
}
