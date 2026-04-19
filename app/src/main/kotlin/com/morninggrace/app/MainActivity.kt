package com.morninggrace.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.morninggrace.ai.GeminiClient
import com.morninggrace.ai.KEY_GEMINI_API_KEY
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.morninggrace.alarm.AlarmPermissionChecker
import com.morninggrace.alarm.AlarmScheduler
import com.morninggrace.alarm.AlarmService
import com.morninggrace.core.model.AlarmConfig
import com.morninggrace.core.repository.LocationRepository
import com.morninggrace.orchestrator.DynamicBibleReadingPlan
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var permissionChecker: AlarmPermissionChecker
    @Inject lateinit var locationRepo: LocationRepository

    private lateinit var prefs: SharedPreferences
    private lateinit var locationStatus: TextView
    private lateinit var timeDisplay: TextView
    private lateinit var networkBanner: TextView

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = runOnUiThread { networkBanner.visibility = View.GONE }
        override fun onLost(network: Network) = runOnUiThread { networkBanner.visibility = View.VISIBLE }
    }

    private var selectedHour = 6
    private var selectedMinute = 0

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* silent */ }

    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* silent — SpeechEngine falls back gracefully if denied */ }

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
        requestRecordAudioPermissionIfNeeded()

        val alarmSwitch = findViewById<SwitchMaterial>(R.id.alarmSwitch)
        val warning = findViewById<TextView>(R.id.permissionWarning)
        locationStatus = findViewById(R.id.locationStatus)
        timeDisplay = findViewById(R.id.timeDisplay)
        networkBanner = findViewById(R.id.networkBanner)

        selectedHour = prefs.getInt("hour", 6)
        selectedMinute = prefs.getInt("minute", 0)
        alarmSwitch.isChecked = prefs.getBoolean("enabled", false)
        updateTimeDisplay()
        updateLocationStatus()

        timeDisplay.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                selectedHour = h
                selectedMinute = m
                updateTimeDisplay()
                if (alarmSwitch.isChecked) {
                    saveAndSchedule(h, m, enabled = true)
                } else {
                    prefs.edit().putInt("hour", h).putInt("minute", m).apply()
                }
            }, selectedHour, selectedMinute, true).show()
        }

        alarmSwitch.setOnCheckedChangeListener { _, enabled ->
            if (enabled && !permissionChecker.canScheduleExactAlarms()) {
                warning.visibility = View.VISIBLE
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                alarmSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }
            warning.visibility = View.GONE
            saveAndSchedule(selectedHour, selectedMinute, enabled)
        }

        findViewById<MaterialButton>(R.id.locationButton).setOnClickListener {
            requestLocationOrFetch()
        }

        // Module toggles
        bindModuleCheckbox(R.id.moduleWeather, AlarmService.KEY_MODULE_WEATHER)
        bindModuleCheckbox(R.id.moduleFinance, AlarmService.KEY_MODULE_FINANCE)
        bindModuleCheckbox(R.id.moduleNews,    AlarmService.KEY_MODULE_NEWS)

        // Bible checkbox + reading plan (plan visible only when Bible is enabled)
        val moduleBible = findViewById<CheckBox>(R.id.moduleBible)
        val planGroup   = findViewById<RadioGroup>(R.id.planRadioGroup)
        moduleBible.isChecked = prefs.getBoolean(AlarmService.KEY_MODULE_BIBLE, true)
        planGroup.visibility  = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        moduleBible.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AlarmService.KEY_MODULE_BIBLE, checked).apply()
            planGroup.visibility = if (checked) View.VISIBLE else View.GONE
        }
        val savedPlan = prefs.getString(DynamicBibleReadingPlan.KEY, DynamicBibleReadingPlan.ID_MCCHEYNE)
        planGroup.check(when (savedPlan) {
            DynamicBibleReadingPlan.ID_SEQUENTIAL    -> R.id.planSequential
            DynamicBibleReadingPlan.ID_CHAPTER_A_DAY -> R.id.planChapterADay
            else                                     -> R.id.planMcCheyne
        })
        planGroup.setOnCheckedChangeListener { _, checkedId ->
            val planId = when (checkedId) {
                R.id.planSequential    -> DynamicBibleReadingPlan.ID_SEQUENTIAL
                R.id.planChapterADay   -> DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
                else                   -> DynamicBibleReadingPlan.ID_MCCHEYNE
            }
            prefs.edit().putString(DynamicBibleReadingPlan.KEY, planId).apply()
        }

        // AI: Gemini API key
        val aiPrefs = getSharedPreferences("ai_prefs", MODE_PRIVATE)
        val apiKeyInput = findViewById<TextInputEditText>(R.id.geminiApiKeyInput)
        apiKeyInput.setText(aiPrefs.getString(KEY_GEMINI_API_KEY, ""))
        apiKeyInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveApiKey(aiPrefs, apiKeyInput); true
            } else false
        }
        findViewById<MaterialButton>(R.id.saveApiKeyButton).setOnClickListener {
            saveApiKey(aiPrefs, apiKeyInput)
        }

        // Dev: test broadcast (uses current module prefs)
        findViewById<MaterialButton>(R.id.testBroadcastButton).setOnClickListener {
            ContextCompat.startForegroundService(this, Intent(this, AlarmService::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        val cm = getSystemService(ConnectivityManager::class.java)
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, networkCallback)
        val connected = cm.activeNetwork?.let {
            cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false
        networkBanner.visibility = if (connected) View.GONE else View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
    }

    override fun onResume() {
        super.onResume()
        if (permissionChecker.canScheduleExactAlarms()) {
            findViewById<TextView>(R.id.permissionWarning).visibility = View.GONE
        }
        updateLocationStatus()
    }

    private fun saveApiKey(prefs: android.content.SharedPreferences, input: TextInputEditText) {
        val key = input.text?.toString()?.trim() ?: ""
        prefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()
        input.clearFocus()
        val imm = getSystemService(android.view.inputmethod.InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun bindModuleCheckbox(viewId: Int, prefKey: String) {
        val checkbox = findViewById<CheckBox>(viewId)
        checkbox.isChecked = prefs.getBoolean(prefKey, true)
        checkbox.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(prefKey, checked).apply()
        }
    }

    private fun updateTimeDisplay() {
        timeDisplay.text = "%02d:%02d".format(selectedHour, selectedMinute)
    }

    private fun saveAndSchedule(hour: Int, minute: Int, enabled: Boolean) {
        prefs.edit()
            .putInt("hour", hour)
            .putInt("minute", minute)
            .putBoolean("enabled", enabled)
            .apply()
        val config = AlarmConfig(hourOfDay = hour, minute = minute, enabled = enabled)
        if (enabled) scheduler.schedule(config) else scheduler.cancel()
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
                        client.lastLocation.addOnSuccessListener { last ->
                            if (last != null) {
                                locationRepo.save(last.latitude, last.longitude)
                                updateLocationStatus()
                            } else {
                                locationStatus.text = "无法获取位置，请检查 GPS 是否开启"
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
        locationStatus.text = if (locationRepo.hasLocation()) {
            val loc = locationRepo.get()
            "📍 %.4f, %.4f".format(loc.lat, loc.lon)
        } else {
            "未设置（默认：悉尼）"
        }
    }

    private fun requestRecordAudioPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
