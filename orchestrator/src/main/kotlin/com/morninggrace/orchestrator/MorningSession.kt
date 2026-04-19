package com.morninggrace.orchestrator

import com.morninggrace.core.model.BroadcastConfig
import javax.inject.Inject

class MorningSession @Inject constructor(
    private val orchestrator: BroadcastOrchestrator
) {
    suspend fun start(config: BroadcastConfig = BroadcastConfig()) = orchestrator.broadcast(config = config)
    fun stop() = orchestrator.stop()
}
