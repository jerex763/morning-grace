package com.morninggrace.orchestrator.di

import com.morninggrace.bible.plan.BibleReadingPlan
import com.morninggrace.bible.plan.McCheyneOnePlan
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class OrchestratorModule {

    @Binds
    @Singleton
    abstract fun bindsBibleReadingPlan(impl: McCheyneOnePlan): BibleReadingPlan
}
