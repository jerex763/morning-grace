package com.morninggrace.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.morninggrace.core.model.AlarmConfig
import java.util.Calendar
import javax.inject.Inject

class AlarmScheduler @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val permissionChecker: AlarmPermissionChecker
) {
    companion object {
        const val ALARM_REQUEST_CODE = 1001
    }

    fun schedule(config: AlarmConfig) {
        if (!permissionChecker.canScheduleExactAlarms()) return

        val triggerAt = nextAlarmMillis(config.hourOfDay, config.minute)
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val info = AlarmManager.AlarmClockInfo(triggerAt, pending)
        alarmManager.setAlarmClock(info, pending)
    }

    fun cancel() {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    private fun nextAlarmMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
