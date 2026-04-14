package com.morninggrace.bible.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiblePassageTest {

    @Test
    fun `passage covers single verse`() {
        val p = BiblePassage(book = 1, chapter = 1, verseStart = 1, verseEnd = 1)
        assertEquals(1, p.verseCount())
    }

    @Test
    fun `passage covers multiple verses`() {
        val p = BiblePassage(book = 1, chapter = 1, verseStart = 1, verseEnd = 10)
        assertEquals(10, p.verseCount())
    }

    @Test
    fun `whole chapter uses sentinel -1 for verseEnd`() {
        val p = BiblePassage(book = 1, chapter = 1, verseStart = 1, verseEnd = -1)
        assertTrue(p.isWholeChapter())
    }
}
