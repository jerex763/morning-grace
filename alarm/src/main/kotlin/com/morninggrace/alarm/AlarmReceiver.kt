package com.morninggrace.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.morninggrace.core.model.AlarmConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        // Reschedule for tomorrow before starting the service
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ENABLED, false)) {
            scheduler.schedule(AlarmConfig(
                hourOfDay = prefs.getInt(KEY_HOUR, 6),
                minute    = prefs.getInt(KEY_MINUTE, 0),
                enabled   = true
            ))
        }

        val serviceIntent = Intent(context, AlarmService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val PREFS       = "alarm_prefs"
        const val KEY_HOUR    = "hour"
        const val KEY_MINUTE  = "minute"
        const val KEY_ENABLED = "enabled"
    }
}
