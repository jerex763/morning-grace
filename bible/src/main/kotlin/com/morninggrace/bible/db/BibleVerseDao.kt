package com.morninggrace.bible.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.morninggrace.bible.model.BibleVerse

@Dao
interface BibleVerseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(verses: List<BibleVerse>)

    @Query("""
        SELECT * FROM bible_verses
        WHERE book = :book AND chapter = :chapter AND lang = :lang
        ORDER BY verse ASC
    """)
    suspend fun getChapter(book: Int, chapter: Int, lang: String): List<BibleVerse>

    @Query("""
        SELECT * FROM bible_verses
        WHERE book = :book AND chapter = :chapter AND verse BETWEEN :verseStart AND :verseEnd AND lang = :lang
        ORDER BY verse ASC
    """)
    suspend fun getVerses(book: Int, chapter: Int, verseStart: Int, verseEnd: Int, lang: String): List<BibleVerse>

    @Query("SELECT COUNT(*) FROM bible_verses WHERE lang = :lang")
    suspend fun countByLang(lang: String): Int
}
