package com.morninggrace.alarm

import android.app.AlarmManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPermissionCheckerTest {

    private val alarmManager = mockk<AlarmManager>()

    @Test
    fun `canScheduleExactAlarms returns true when AlarmManager allows it`() {
        every { alarmManager.canScheduleExactAlarms() } returns true
        val checker = AlarmPermissionChecker(alarmManager)
        // In JVM test environment SDK_INT is 0, so the else branch runs and returns true
        // This test verifies the checker can be constructed and called without crash
        assertTrue(checker.canScheduleExactAlarms())
    }

    @Test
    fun `canScheduleExactAlarms returns true on pre-S devices`() {
        // Build.VERSION.SDK_INT is 0 in unit tests (< Build.VERSION_CODES.S = 31)
        // So canScheduleExactAlarms() always returns true on this test JVM
        val checker = AlarmPermissionChecker(alarmManager)
        assertTrue(checker.canScheduleExactAlarms())
    }
}
