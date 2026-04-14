package com.morninggrace.bible.model

data class BiblePassage(
    val book: Int,
    val chapter: Int,
    val verseStart: Int = 1,
    val verseEnd: Int = -1  // -1 means whole chapter
) {
    fun verseCount(): Int {
        require(verseEnd != -1) { "verseEnd is -1 (whole chapter); count unknown without DB" }
        return verseEnd - verseStart + 1
    }

    fun isWholeChapter(): Boolean = verseEnd == -1
}
