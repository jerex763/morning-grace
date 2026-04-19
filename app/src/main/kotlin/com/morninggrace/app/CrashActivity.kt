package com.morninggrace.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        val message = getSharedPreferences(CrashHandler.PREFS, MODE_PRIVATE)
            .getString(CrashHandler.KEY_MESSAGE, null)

        findViewById<TextView>(R.id.crashDetail).apply {
            text = message ?: "未知错误"
            visibility = if (message != null) android.view.View.VISIBLE else android.view.View.GONE
        }

        findViewById<Button>(R.id.restartButton).setOnClickListener {
            getSharedPreferences(CrashHandler.PREFS, MODE_PRIVATE)
                .edit().remove(CrashHandler.KEY_MESSAGE).apply()

            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }
}
