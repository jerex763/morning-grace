package com.morninggrace.bible.db

import android.content.Context
import android.util.Log
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.morninggrace.bible.model.BibleVerse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.util.zip.ZipFile

class BibleDatabaseCallback(
    private val context: Context,
    private val dao: () -> BibleVerseDao
) : RoomDatabase.Callback() {

    companion object {
        private const val INSERT_BATCH_SIZE = 500
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.d("MorningGrace", "BibleDB: seeding started")
        runBlocking(Dispatchers.IO) {
            seedFromAsset("cuv.csv", "zh")
            Log.d("MorningGrace", "BibleDB: zh seeded")
            seedFromAsset("esv.csv", "en")
            Log.d("MorningGrace", "BibleDB: en seeded")
        }
        Log.d("MorningGrace", "BibleDB: seeding complete")
    }

    /** Opens an asset, falling back to reading directly from the APK zip.
     *  Required when Android Studio overlay mode remounts assets to a path
     *  that AssetManager can't resolve (e.g. emulator Apply Changes). */
    private fun openAsset(fileName: String): InputStream {
        return try {
            context.assets.open(fileName)
        } catch (e: Exception) {
            Log.d("MorningGrace", "BibleDB: assets.open failed, falling back to APK zip: ${e.message}")
            val apkPath = context.applicationInfo.sourceDir
            val zip = ZipFile(apkPath)
            val entry = zip.getEntry("assets/$fileName")
                ?: throw IllegalStateException("Asset not found in APK: assets/$fileName")
            zip.getInputStream(entry)
        }
    }

    private suspend fun seedFromAsset(fileName: String, lang: String) {
        val verses = mutableListOf<BibleVerse>()
        openAsset(fileName).bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                val verse = parseCsvLine(line, lang) ?: continue
                verses.add(verse)
                if (verses.size >= INSERT_BATCH_SIZE) {
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
