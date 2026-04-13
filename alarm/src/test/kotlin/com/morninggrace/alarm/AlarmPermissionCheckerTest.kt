package com.morninggrace.alarm

import android.app.AlarmManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmPermissionCheckerTest {

    private val alarmManager = mockk<AlarmManager>()
    private val checker = AlarmPermissionChecker(alarmManager)

    @Test
    fun `canScheduleExactAlarms returns true when AlarmManager allows it`() {
        every { alarmManager.canScheduleExactAlarms() } returns true
        assertTrue(checker.canScheduleExactAlarms())
    }

    @Test
    fun `canScheduleExactAlarms returns false when AlarmManager denies it`() {
        every { alarmManager.canScheduleExactAlarms() } returns false
        assertFalse(checker.canScheduleExactAlarms())
    }
}
