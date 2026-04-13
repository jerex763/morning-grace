package com.morninggrace.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import com.morninggrace.core.model.AlarmConfig
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class AlarmSchedulerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val permissionChecker = mockk<AlarmPermissionChecker>()
    private val scheduler = AlarmScheduler(context, alarmManager, permissionChecker)

    @Before
    fun setUp() {
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `schedule calls setAlarmClock when permission granted`() {
        every { permissionChecker.canScheduleExactAlarms() } returns true
        val config = AlarmConfig(hourOfDay = 6, minute = 30, enabled = true)

        scheduler.schedule(config)

        verify { alarmManager.setAlarmClock(any(), any()) }
    }

    @Test
    fun `schedule does not call setAlarmClock when permission denied`() {
        every { permissionChecker.canScheduleExactAlarms() } returns false
        val config = AlarmConfig(hourOfDay = 6, minute = 30, enabled = true)

        scheduler.schedule(config)

        verify(exactly = 0) { alarmManager.setAlarmClock(any(), any()) }
    }

    @Test
    fun `cancel calls cancel on AlarmManager`() {
        scheduler.cancel()
        verify { alarmManager.cancel(any<PendingIntent>()) }
    }
}
