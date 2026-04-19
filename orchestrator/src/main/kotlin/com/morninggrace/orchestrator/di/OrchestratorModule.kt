package com.morninggrace.orchestrator.di

import com.morninggrace.bible.plan.BibleReadingPlan
import com.morninggrace.bible.plan.ChapterADayPlan
import com.morninggrace.bible.plan.McCheyneOnePlan
import com.morninggrace.bible.plan.SequentialPlan
import com.morninggrace.orchestrator.DynamicBibleReadingPlan
import com.morninggrace.core.repository.FinanceRepository
import com.morninggrace.core.repository.NewsRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.orchestrator.finance.YahooFinanceRepository
import com.morninggrace.orchestrator.news.RssNewsRepository
import com.morninggrace.orchestrator.weather.OpenMeteoWeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrchestratorModule {

    @Binds @Singleton
    abstract fun bindsBibleReadingPlan(impl: DynamicBibleReadingPlan): BibleReadingPlan

    @Binds @Singleton
    abstract fun bindsWeatherRepository(impl: OpenMeteoWeatherRepository): WeatherRepository

    @Binds @Singleton
    abstract fun bindsFinanceRepository(impl: YahooFinanceRepository): FinanceRepository

    @Binds @Singleton
    abstract fun bindsNewsRepository(impl: RssNewsRepository): NewsRepository

    companion object {
        @Provides @Singleton fun providesSequentialPlan()  = SequentialPlan()
        @Provides @Singleton fun providesChapterADayPlan() = ChapterADayPlan()

        @Provides @Singleton
        fun providesOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
