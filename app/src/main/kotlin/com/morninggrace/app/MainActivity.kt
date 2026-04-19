package com.morninggrace.app

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.morninggrace.alarm.AlarmPermissionChecker
import com.morninggrace.alarm.AlarmScheduler
import com.morninggrace.alarm.AlarmService
import com.morninggrace.core.model.AlarmConfig
import com.morninggrace.core.repository.LocationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var permissionChecker: AlarmPermissionChecker
    @Inject lateinit var locationRepo: LocationRepository

    private lateinit var prefs: SharedPreferences
    private lateinit var locationStatus: TextView

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* silent */ }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) fetchLocation()
        else locationStatus.text = "位置权限被拒绝"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("alarm_prefs", MODE_PRIVATE)
        requestNotificationPermissionIfNeeded()

        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val alarmSwitch = findViewById<Switch>(R.id.alarmSwitch)
        val warning = findViewById<TextView>(R.id.permissionWarning)
        locationStatus = findViewById(R.id.locationStatus)

        timePicker.hour = prefs.getInt("hour", 6)
        timePicker.minute = prefs.getInt("minute", 0)
        alarmSwitch.isChecked = prefs.getBoolean("enabled", false)

        updateLocationStatus()

        alarmSwitch.setOnCheckedChangeListener { _, enabled ->
            if (enabled && !permissionChecker.canScheduleExactAlarms()) {
                warning.visibility = View.VISIBLE
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                alarmSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }
            warning.visibility = View.GONE
            val config = AlarmConfig(
                hourOfDay = timePicker.hour,
                minute = timePicker.minute,
                enabled = enabled
            )
            prefs.edit()
                .putInt("hour", timePicker.hour)
                .putInt("minute", timePicker.minute)
                .putBoolean("enabled", enabled)
                .apply()
            if (enabled) scheduler.schedule(config) else scheduler.cancel()
        }

        findViewById<Button>(R.id.locationButton).setOnClickListener {
            requestLocationOrFetch()
        }

        val skipBibleCheckbox = findViewById<CheckBox>(R.id.skipBibleCheckbox)
        findViewById<Button>(R.id.testBroadcastButton).setOnClickListener {
            val intent = Intent(this, AlarmService::class.java).apply {
                putExtra(AlarmService.EXTRA_SKIP_BIBLE, skipBibleCheckbox.isChecked)
            }
            ContextCompat.startForegroundService(this, intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionChecker.canScheduleExactAlarms()) {
            findViewById<TextView>(R.id.permissionWarning).visibility = View.GONE
        }
        updateLocationStatus()
    }

    private fun requestLocationOrFetch() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        if (ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        } else {
            locationPermissionLauncher.launch(arrayOf(fine, coarse))
        }
    }

    private fun fetchLocation() {
        locationStatus.text = "正在获取位置..."
        val client = LocationServices.getFusedLocationProviderClient(this)
        try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        locationRepo.save(location.latitude, location.longitude)
                        updateLocationStatus()
                    } else {
                        // fall back to last known
                        client.lastLocation.addOnSuccessListener { last ->
                            if (last != null) {
                                locationRepo.save(last.latitude, last.longitude)
                                updateLocationStatus()
                            } else {
                                locationStatus.text = "无法获取位置，请检查GPS是否开启"
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    locationStatus.text = "位置获取失败：${it.message}"
                }
        } catch (e: SecurityException) {
            locationStatus.text = "位置权限不足"
        }
    }

    private fun updateLocationStatus() {
        if (locationRepo.hasLocation()) {
            val loc = locationRepo.get()
            locationStatus.text = "📍 %.4f, %.4f".format(loc.lat, loc.lon)
        } else {
            locationStatus.text = "位置未设置（使用默认：Sydney）"
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
