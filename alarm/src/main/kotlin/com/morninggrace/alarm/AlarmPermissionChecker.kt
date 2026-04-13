package com.morninggrace.alarm

import android.app.AlarmManager
import android.os.Build
import javax.inject.Inject

class AlarmPermissionChecker @Inject constructor(
    private val alarmManager: AlarmManager
) {
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
