package com.morninggrace.bible.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.morninggrace.bible.model.BibleVerse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BibleDatabaseCallback(
    private val context: Context,
    private val dao: () -> BibleVerseDao
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            seedFromAsset("cuv.csv", "zh")
            seedFromAsset("esv.csv", "en")
        }
    }

    private suspend fun seedFromAsset(fileName: String, lang: String) {
        val verses = mutableListOf<BibleVerse>()
        context.assets.open(fileName).bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                val verse = parseCsvLine(line, lang) ?: continue
                verses.add(verse)
                if (verses.size >= 500) {
                    dao().insertAll(verses)
                    verses.clear()
                }
            }
        }
        if (verses.isNotEmpty()) dao().insertAll(verses)
    }

    // CSV format: book,chapter,verse,"text"
    private fun parseCsvLine(line: String, lang: String): BibleVerse? {
        return try {
            val firstComma = line.indexOf(',')
            val secondComma = line.indexOf(',', firstComma + 1)
            val thirdComma = line.indexOf(',', secondComma + 1)
            val book = line.substring(0, firstComma).trim().toInt()
            val chapter = line.substring(firstComma + 1, secondComma).trim().toInt()
            val verse = line.substring(secondComma + 1, thirdComma).trim().toInt()
            val text = line.substring(thirdComma + 1).trim().removeSurrounding("\"")
            BibleVerse(book, chapter, verse, lang, text)
        } catch (e: Exception) {
            null
        }
    }
}
