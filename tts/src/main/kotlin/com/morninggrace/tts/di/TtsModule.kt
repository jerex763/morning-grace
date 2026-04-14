package com.morninggrace.tts.di

import com.morninggrace.tts.AndroidTtsEngine
import com.morninggrace.tts.TtsEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {

    @Binds
    @Singleton
    abstract fun bindsTtsEngine(impl: AndroidTtsEngine): TtsEngine
}
