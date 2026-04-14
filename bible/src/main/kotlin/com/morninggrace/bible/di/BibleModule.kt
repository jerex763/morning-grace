package com.morninggrace.bible.di

import android.content.Context
import com.morninggrace.bible.db.BibleDatabase
import com.morninggrace.bible.db.BibleVerseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BibleModule {

    @Provides
    @Singleton
    fun providesBibleDatabase(@ApplicationContext context: Context): BibleDatabase =
        BibleDatabase.createWithCallback(context)

    @Provides
    fun providesBibleVerseDao(db: BibleDatabase): BibleVerseDao = db.verseDao()
}
