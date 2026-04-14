package com.morninggrace.bible.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.morninggrace.bible.model.BibleVerse

@Database(entities = [BibleVerse::class], version = 1, exportSchema = false)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun verseDao(): BibleVerseDao

    companion object {
        fun createWithCallback(context: Context): BibleDatabase {
            lateinit var database: BibleDatabase
            database = Room.databaseBuilder(context, BibleDatabase::class.java, "bible.db")
                .fallbackToDestructiveMigration()
                .addCallback(BibleDatabaseCallback(context) { database.verseDao() })
                .build()
            return database
        }
    }
}
