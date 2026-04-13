package com.morninggrace.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmConfigTest {

    @Test
    fun `default alarm config is disabled`() {
        val config = AlarmConfig()
        assertFalse(config.enabled)
        assertEquals(Language.ZH, config.language)
    }

    @Test
    fun `alarm config with time is valid`() {
        val config = AlarmConfig(hourOfDay = 6, minute = 30, enabled = true)
        assertTrue(config.enabled)
        assertEquals(6, config.hourOfDay)
        assertEquals(30, config.minute)
    }
}
