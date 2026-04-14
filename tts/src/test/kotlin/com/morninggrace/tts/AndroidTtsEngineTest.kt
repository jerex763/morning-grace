package com.morninggrace.tts

import android.speech.tts.TextToSpeech
import com.morninggrace.core.model.Language
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTtsEngineTest {

    @Test
    fun `isAvailable returns false before initialization`() {
        val engine = AndroidTtsEngine()
        assertFalse(engine.isAvailable())
    }

    @Test
    fun `isAvailable returns true after successful init`() {
        val engine = AndroidTtsEngine()
        engine.onInitResult(TextToSpeech.SUCCESS)
        assertTrue(engine.isAvailable())
    }

    @Test
    fun `isAvailable returns false after failed init`() {
        val engine = AndroidTtsEngine()
        engine.onInitResult(TextToSpeech.ERROR)
        assertFalse(engine.isAvailable())
    }
}
