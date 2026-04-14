package com.morninggrace.orchestrator

import javax.inject.Inject

class MorningSession @Inject constructor(
    private val orchestrator: BroadcastOrchestrator
) {
    suspend fun start() = orchestrator.broadcast()
    fun stop() = orchestrator.stop()
}
