package com.morninggrace.orchestrator.di

import com.morninggrace.bible.plan.BibleReadingPlan
import com.morninggrace.bible.plan.McCheyneOnePlan
import com.morninggrace.core.repository.FinanceRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.orchestrator.finance.YahooFinanceRepository
import com.morninggrace.orchestrator.weather.OpenMeteoWeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrchestratorModule {

    @Binds @Singleton
    abstract fun bindsBibleReadingPlan(impl: McCheyneOnePlan): BibleReadingPlan

    @Binds @Singleton
    abstract fun bindsWeatherRepository(impl: OpenMeteoWeatherRepository): WeatherRepository

    @Binds @Singleton
    abstract fun bindsFinanceRepository(impl: YahooFinanceRepository): FinanceRepository

    companion object {
        @Provides @Singleton
        fun providesOkHttpClient(): OkHttpClient = OkHttpClient()
    }
}
