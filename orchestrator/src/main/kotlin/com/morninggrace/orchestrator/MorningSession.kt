package com.morninggrace.orchestrator

import javax.inject.Inject

class MorningSession @Inject constructor(
    private val orchestrator: BroadcastOrchestrator
) {
    suspend fun start(skipBible: Boolean = false) = orchestrator.broadcast(skipBible = skipBible)
    fun stop() = orchestrator.stop()
}
