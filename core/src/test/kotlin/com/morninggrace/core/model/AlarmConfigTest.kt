package com.morninggrace.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmConfigTest {

    @Test
    fun `default alarm config is disabled`() {
        val config = AlarmConfig()
        assertFalse(config.enabled)
    }

    @Test
    fun `alarm config with time is valid`() {
        val config = AlarmConfig(hourOfDay = 6, minute = 30, enabled = true)
        assertTrue(config.enabled)
        assert(config.hourOfDay == 6)
        assert(config.minute == 30)
    }
}
