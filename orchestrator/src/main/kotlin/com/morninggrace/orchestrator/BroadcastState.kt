package com.morninggrace.orchestrator

sealed class BroadcastState {
    object Idle : BroadcastState()
    object Preparing : BroadcastState()
    data class Broadcasting(val content: BroadcastContent) : BroadcastState()
    data class Failed(val reason: String) : BroadcastState()
}
