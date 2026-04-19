package com.morninggrace.tts

import com.morninggrace.core.model.Language

interface TtsEngine {
    /** Speaks [text] in [language]. Suspends until speaking completes. */
    suspend fun speak(text: String, language: Language)

    /** Returns true if the engine is initialised and ready. */
    fun isAvailable(): Boolean

    /** Immediately stops any in-progress speech. */
    fun stop()
}
