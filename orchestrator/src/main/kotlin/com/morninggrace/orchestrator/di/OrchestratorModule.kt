package com.morninggrace.orchestrator.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object OrchestratorModule {
    // BroadcastOrchestrator uses @Inject constructor — nothing to provide manually.
    // This module is a placeholder for future configuration (e.g., coroutine scope).
}
