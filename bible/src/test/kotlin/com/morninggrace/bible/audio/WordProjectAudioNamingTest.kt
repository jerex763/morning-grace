package com.morninggrace.bible.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordProjectAudioNamingTest {

    @Test
    fun `parses Genesis file inside WordProject folder`() {
        assertEquals(
            AudioChapter(book = 1, chapter = 1),
            WordProjectAudioNaming.parse("01_GEN/GEN_001_ck7_ef10.mp3")
        )
    }

    @Test
    fun `parses numbered book codes`() {
        assertEquals(
            AudioChapter(book = 62, chapter = 5),
            WordProjectAudioNaming.parse("1JN_005_ck7_ef10.mp3")
        )
    }

    @Test
    fun `parses canonical imported filename`() {
        assertEquals(
            AudioChapter(book = 66, chapter = 22),
            WordProjectAudioNaming.parse("66_022.mp3")
        )
    }

    @Test
    fun `parses current WordProject numbered folder format`() {
        assertEquals(
            AudioChapter(book = 43, chapter = 16),
            WordProjectAudioNaming.parse("43/16.mp3")
        )
    }

    @Test
    fun `rejects chapter outside book range`() {
        assertNull(WordProjectAudioNaming.parse("GEN_051_ck7_ef10.mp3"))
    }

    @Test
    fun `ignores unrelated mp3`() {
        assertNull(WordProjectAudioNaming.parse("introduction.mp3"))
    }
}
