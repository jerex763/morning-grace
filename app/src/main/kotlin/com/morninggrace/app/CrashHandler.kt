package com.morninggrace.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val default = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val message = buildString {
                append(throwable.javaClass.simpleName)
                throwable.message?.let { append(": $it") }
                append("\n\n")
                append(throwable.stackTraceToString().lines().take(12).joinToString("\n"))
            }

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MESSAGE, message)
                .commit()   // commit (not apply) — process is about to die

            val intent = Intent(context, CrashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            pending.send()
        } catch (_: Exception) {
            default?.uncaughtException(thread, throwable)
        } finally {
            Process.killProcess(Process.myPid())
        }
    }

    companion object {
        const val PREFS       = "crash_prefs"
        const val KEY_MESSAGE = "last_crash"
    }
}
