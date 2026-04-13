package com.morninggrace.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.morninggrace.core.model.AlarmConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            val hour = prefs.getInt("hour", 6)
            val minute = prefs.getInt("minute", 0)
            val enabled = prefs.getBoolean("enabled", false)
            if (enabled) {
                val config = AlarmConfig(hourOfDay = hour, minute = minute, enabled = true)
                scheduler.schedule(config)
            }
        }
    }
}
