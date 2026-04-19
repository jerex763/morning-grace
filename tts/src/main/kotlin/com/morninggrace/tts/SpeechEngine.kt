package com.morninggrace.tts

import com.morninggrace.core.model.ConfirmationResult

interface SpeechEngine {
    /** Listens for a voice confirmation or skip command.
     *  Returns [ConfirmationResult.Confirmed] / [Skipped] / [Timeout] after [timeoutMs] ms. */
    suspend fun listenForConfirmation(timeoutMs: Long = 8_000L): ConfirmationResult
    fun isAvailable(): Boolean
}
