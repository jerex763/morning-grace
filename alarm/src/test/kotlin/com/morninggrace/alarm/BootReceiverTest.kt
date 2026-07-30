package com.morninggrace.alarm

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverTest {

    @Test
    fun `reschedules after boot time timezone and app replacement`() {
        assertTrue(BootReceiver.shouldReschedule(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(BootReceiver.shouldReschedule(Intent.ACTION_TIME_CHANGED))
        assertTrue(BootReceiver.shouldReschedule(Intent.ACTION_TIMEZONE_CHANGED))
        assertTrue(BootReceiver.shouldReschedule(Intent.ACTION_MY_PACKAGE_REPLACED))
    }

    @Test
    fun `ignores unrelated and missing actions`() {
        assertFalse(BootReceiver.shouldReschedule(Intent.ACTION_SCREEN_ON))
        assertFalse(BootReceiver.shouldReschedule(null))
    }
}
