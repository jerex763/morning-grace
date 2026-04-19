package com.morninggrace.bible.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.morninggrace.bible.model.BibleVerse

@Database(entities = [BibleVerse::class], version = 1, exportSchema = true)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun verseDao(): BibleVerseDao

    companion object {
        fun create(context: Context): BibleDatabase {
            return Room.databaseBuilder(context, BibleDatabase::class.java, "bible.db")
                .createFromAsset("bible_prebuilt.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
