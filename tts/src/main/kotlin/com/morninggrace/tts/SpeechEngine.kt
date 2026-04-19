package com.morninggrace.tts

import com.morninggrace.core.model.ConfirmationResult

interface SpeechEngine {
    /** Listens for a voice confirmation or skip command.
     *  Returns [ConfirmationResult.Confirmed] / [Skipped] / [Timeout] after [timeoutMs] ms. */
    suspend fun listenForConfirmation(timeoutMs: Long = 8_000L): ConfirmationResult

    /** Listens for free-form speech and returns the best-match transcript, or null on timeout/error. */
    suspend fun listenForText(timeoutMs: Long = 30_000L): String? = null

    fun isAvailable(): Boolean
}
