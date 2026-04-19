package com.morninggrace.bible

import com.morninggrace.bible.model.BiblePassage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookNamesTest {

    // ── BookNames.ZH completeness ────────────────────────────────────────────

    @Test fun `all 66 books are present`() {
        assertEquals(66, BookNames.ZH.size)
    }

    @Test fun `book 1 is 创世记`() {
        assertEquals("创世记", BookNames.ZH[1])
    }

    @Test fun `book 40 is 马太福音`() {
        assertEquals("马太福音", BookNames.ZH[40])
    }

    @Test fun `book 66 is 启示录`() {
        assertEquals("启示录", BookNames.ZH[66])
    }

    // ── toChineseTitle ────────────────────────────────────────────────────────

    @Test fun `Matthew chapter 1`() {
        val title = BiblePassage(book = 40, chapter = 1).toChineseTitle()
        assertEquals("马太福音第一章", title)
    }

    @Test fun `Genesis chapter 10`() {
        val title = BiblePassage(book = 1, chapter = 10).toChineseTitle()
        assertEquals("创世记第十章", title)
    }

    @Test fun `Psalms chapter 119`() {
        val title = BiblePassage(book = 19, chapter = 119).toChineseTitle()
        assertEquals("诗篇第一百一十九章", title)
    }

    @Test fun `Revelation chapter 22`() {
        val title = BiblePassage(book = 66, chapter = 22).toChineseTitle()
        assertEquals("启示录第二十二章", title)
    }

    @Test fun `unknown book uses fallback`() {
        val title = BiblePassage(book = 99, chapter = 1).toChineseTitle()
        assertTrue("fallback should mention book number: $title", title.contains("99"))
    }

    // ── chapterToZh edge cases ────────────────────────────────────────────────

    @Test fun `chapter 1 to 9`() {
        val passage = BiblePassage(book = 1, chapter = 5)
        assertEquals("创世记第五章", passage.toChineseTitle())
    }

    @Test fun `chapter 11`() {
        assertEquals("创世记第十一章", BiblePassage(book = 1, chapter = 11).toChineseTitle())
    }

    @Test fun `chapter 20`() {
        assertEquals("创世记第二十章", BiblePassage(book = 1, chapter = 20).toChineseTitle())
    }

    @Test fun `chapter 100`() {
        // Only Psalms has ≥100 chapters
        assertEquals("诗篇第一百章", BiblePassage(book = 19, chapter = 100).toChineseTitle())
    }

    @Test fun `chapter 150`() {
        assertEquals("诗篇第一百五十章", BiblePassage(book = 19, chapter = 150).toChineseTitle())
    }
}
