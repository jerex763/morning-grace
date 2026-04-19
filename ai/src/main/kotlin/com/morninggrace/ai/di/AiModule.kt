package com.morninggrace.ai.di

import com.morninggrace.ai.AiClient
import com.morninggrace.ai.GeminiClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds @Singleton
    abstract fun bindsAiClient(impl: GeminiClient): AiClient
}
