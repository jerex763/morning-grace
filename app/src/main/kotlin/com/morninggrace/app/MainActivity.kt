package com.morninggrace.app

import android.Manifest
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
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.morninggrace.ai.GeminiClient
import com.morninggrace.ai.KEY_GEMINI_API_KEY
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.morninggrace.alarm.AlarmPermissionChecker
import com.morninggrace.alarm.AlarmScheduler
import com.morninggrace.alarm.AlarmService
import com.morninggrace.bible.BookNames
import com.morninggrace.bible.plan.SequentialPlan
import com.morninggrace.bible.toChineseTitle
import com.morninggrace.core.model.AlarmConfig
import com.morninggrace.core.model.WeatherData
import com.morninggrace.core.repository.FinanceRepository
import com.morninggrace.core.repository.LocationRepository
import com.morninggrace.core.repository.NewsRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.orchestrator.DynamicBibleReadingPlan
import com.morninggrace.tts.AndroidTtsEngine
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var permissionChecker: AlarmPermissionChecker
    @Inject lateinit var locationRepo: LocationRepository
    @Inject lateinit var readingPlan: DynamicBibleReadingPlan
    @Inject lateinit var weatherRepo: WeatherRepository
    @Inject lateinit var financeRepo: FinanceRepository
    @Inject lateinit var newsRepo: NewsRepository

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
        val today = LocalDate.now()
        val weekday = arrayOf("一", "二", "三", "四", "五", "六", "日")[today.dayOfWeek.value - 1]
        findViewById<TextView>(R.id.dateDisplay).text =
            "${today.year}年${today.monthValue}月${today.dayOfMonth}日  星期$weekday"

        selectedHour = prefs.getInt("hour", 6)
        selectedMinute = prefs.getInt("minute", 0)
        alarmSwitch.isChecked = prefs.getBoolean("enabled", false)
        updateTimeDisplay()
        updateLocationStatus()
        bindSettingsPanel()
        refreshHomeSummaries()
        if (alarmSwitch.isChecked && permissionChecker.canScheduleExactAlarms()) {
            scheduler.schedule(
                AlarmConfig(
                    hourOfDay = selectedHour,
                    minute = selectedMinute,
                    enabled = true
                )
            )
        }

        timeDisplay.setOnClickListener {
            showWheelTimePicker(alarmSwitch)
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
        val moduleBible = findViewById<SwitchMaterial>(R.id.moduleBible)
        val planGroup   = findViewById<RadioGroup>(R.id.planRadioGroup)
        val bibleEnglish = findViewById<SwitchMaterial>(R.id.bibleEnglish)
        val progressRow = findViewById<LinearLayout>(R.id.bibleProgressRow)
        val readingPreview = findViewById<TextView>(R.id.bibleReadingPreview)
        val speechRateLabel = findViewById<TextView>(R.id.chineseSpeechRateLabel)
        val speechRate = findViewById<SeekBar>(R.id.chineseSpeechRate)
        moduleBible.isChecked = prefs.getBoolean(AlarmService.KEY_MODULE_BIBLE, true)
        bibleEnglish.isChecked = prefs.getBoolean(AlarmService.KEY_BIBLE_ENGLISH, false)
        planGroup.visibility  = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        bibleEnglish.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        progressRow.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        readingPreview.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        speechRateLabel.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        speechRate.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        moduleBible.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AlarmService.KEY_MODULE_BIBLE, checked).apply()
            planGroup.visibility = if (checked) View.VISIBLE else View.GONE
            bibleEnglish.visibility = if (checked) View.VISIBLE else View.GONE
            progressRow.visibility = if (checked) View.VISIBLE else View.GONE
            readingPreview.visibility = if (checked) View.VISIBLE else View.GONE
            speechRateLabel.visibility = if (checked) View.VISIBLE else View.GONE
            speechRate.visibility = if (checked) View.VISIBLE else View.GONE
        }
        bibleEnglish.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AlarmService.KEY_BIBLE_ENGLISH, checked).apply()
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
            updateBibleProgress()
        }
        findViewById<View>(R.id.planChapterADay).setOnClickListener {
            showChapterADayBookPicker()
        }
        bindBiblePlanSelector(planGroup)
        bindBibleProgressControls()
        bindChineseSpeechRate(speechRate, speechRateLabel)

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
        updateBibleProgress()
    }

    private fun saveApiKey(prefs: android.content.SharedPreferences, input: TextInputEditText) {
        val key = input.text?.toString()?.trim() ?: ""
        prefs.edit().putString(KEY_GEMINI_API_KEY, key).apply()
        input.clearFocus()
        val imm = getSystemService(android.view.inputmethod.InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun bindModuleCheckbox(viewId: Int, prefKey: String) {
        val checkbox = findViewById<SwitchMaterial>(viewId)
        checkbox.isChecked = prefs.getBoolean(prefKey, true)
        checkbox.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(prefKey, checked).apply()
        }
    }

    private fun bindSettingsPanel() {
        val panel = findViewById<View>(R.id.settingsPanel)
        val scroll = findViewById<ScrollView>(R.id.mainScroll)
        val button = findViewById<TextView>(R.id.settingsButton)
        button.setOnClickListener {
            val opening = panel.visibility != View.VISIBLE
            panel.visibility = if (opening) View.VISIBLE else View.GONE
            button.contentDescription = if (opening) "关闭播报设置" else "打开播报设置"
            if (opening) panel.post { scroll.smoothScrollTo(0, panel.top) }
        }
    }

    private fun refreshHomeSummaries() {
        val weatherView = findViewById<TextView>(R.id.weatherSummary)
        val financeView = findViewById<TextView>(R.id.financeSummary)
        val newsView = findViewById<TextView>(R.id.newsSummary)
        val cache = getSharedPreferences("home_summary_cache", MODE_PRIVATE)

        weatherView.text = cache.getString("weather", "正在更新…")
        financeView.text = cache.getString("finance", "正在更新…")
        newsView.text = cache.getString("news", "正在更新…")

        lifecycleScope.launch {
            val location = locationRepo.get()
            val weatherJob = async {
                weatherRepo.getCurrentWeather(location.lat, location.lon)
            }
            val financeJob = async { financeRepo.getMarketData() }
            val newsJob = async { newsRepo.getTopHeadlines(1) }

            weatherJob.await()?.let { weather ->
                val summary = "${weather.temperatureCelsius.toInt()}°  ${weather.shortDescription()}\n" +
                    "湿度 ${weather.humidity}%"
                weatherView.text = summary
                cache.edit().putString("weather", summary).apply()
            } ?: run {
                if (cache.getString("weather", null) == null) {
                    weatherView.text = "天气暂时\n无法获取"
                }
            }

            val markets = financeJob.await()
            if (markets.isNotEmpty()) {
                val summary = markets.take(2).joinToString("\n") {
                    val sign = if (it.changePercent >= 0) "+" else ""
                    "${it.indexName} $sign${"%.2f".format(it.changePercent)}%"
                }
                financeView.text = summary
                cache.edit().putString("finance", summary).apply()
            } else if (cache.getString("finance", null) == null) {
                financeView.text = "行情暂时\n无法获取"
            }

            val headlines = newsJob.await()
            if (headlines.isNotEmpty()) {
                val summary = headlines.first().title
                newsView.text = summary
                cache.edit().putString("news", summary).apply()
            } else if (cache.getString("news", null) == null) {
                newsView.text = "新闻暂时\n无法获取"
            }
        }
    }

    private fun WeatherData.shortDescription(): String = when (weatherCode) {
        0 -> "晴"
        1, 2 -> "少云"
        3 -> "多云"
        45, 48 -> "有雾"
        51, 53, 55 -> "毛毛雨"
        61, 63, 65 -> "有雨"
        71, 73, 75 -> "有雪"
        80, 81, 82 -> "阵雨"
        95 -> "雷阵雨"
        else -> "天气变化"
    }

    private fun showWheelTimePicker(alarmSwitch: SwitchMaterial) {
        val view = layoutInflater.inflate(R.layout.dialog_time_picker, null)
        val hourPicker = view.findViewById<NumberPicker>(R.id.hourPicker).apply {
            minValue = 0
            maxValue = 23
            value = selectedHour
            displayedValues = Array(24) { "%02d".format(it) }
            wrapSelectorWheel = true
        }
        val minutePicker = view.findViewById<NumberPicker>(R.id.minutePicker).apply {
            minValue = 0
            maxValue = 59
            value = selectedMinute
            displayedValues = Array(60) { "%02d".format(it) }
            wrapSelectorWheel = true
        }
        val preview = view.findViewById<TextView>(R.id.selectedTimePreview)
        fun updatePreview() {
            preview.text = "%02d:%02d".format(hourPicker.value, minutePicker.value)
        }
        hourPicker.setOnValueChangedListener { _, _, _ -> updatePreview() }
        minutePicker.setOnValueChangedListener { _, _, _ -> updatePreview() }
        updatePreview()

        AlertDialog.Builder(this)
            .setTitle("设置晨间播报时间")
            .setView(view)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                selectedHour = hourPicker.value
                selectedMinute = minutePicker.value
                updateTimeDisplay()
                if (alarmSwitch.isChecked) {
                    saveAndSchedule(selectedHour, selectedMinute, enabled = true)
                } else {
                    prefs.edit()
                        .putInt("hour", selectedHour)
                        .putInt("minute", selectedMinute)
                        .apply()
                }
            }
            .show()
    }

    private fun bindBibleProgressControls() {
        findViewById<MaterialButton>(R.id.biblePreviousDay).setOnClickListener {
            val total = readingPlan.getTotalDays()
            val current = readingPlan.getCurrentDay()
            readingPlan.setCurrentDay(if (current == 1) total else current - 1)
            updateBibleProgress()
        }
        findViewById<MaterialButton>(R.id.bibleNextDay).setOnClickListener {
            val total = readingPlan.getTotalDays()
            val current = readingPlan.getCurrentDay()
            readingPlan.setCurrentDay(if (current == total) 1 else current + 1)
            updateBibleProgress()
        }
        findViewById<TextView>(R.id.bibleDayPickerButton).setOnClickListener {
            val picker = NumberPicker(this).apply {
                minValue = 1
                maxValue = readingPlan.getTotalDays()
                value = readingPlan.getCurrentDay()
                wrapSelectorWheel = false
            }
            AlertDialog.Builder(this)
                .setTitle("选择计划日")
                .setView(picker)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定") { _, _ ->
                    readingPlan.setCurrentDay(picker.value, LocalDate.now())
                    updateBibleProgress()
                }
                .show()
        }
        updateBibleProgress()
    }

    private fun bindBiblePlanSelector(planGroup: RadioGroup) {
        findViewById<TextView>(R.id.biblePlanSelector).setOnClickListener {
            val planIds = arrayOf(
                DynamicBibleReadingPlan.ID_MCCHEYNE,
                DynamicBibleReadingPlan.ID_SEQUENTIAL,
                DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
            )
            val planNames = arrayOf(
                "麦大卫一年读经",
                "顺序读经（创世记 → 启示录）",
                "每天三章"
            )
            val currentId = prefs.getString(
                DynamicBibleReadingPlan.KEY,
                DynamicBibleReadingPlan.ID_MCCHEYNE
            )
            val checkedIndex = planIds.indexOf(currentId).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("选择读经计划")
                .setSingleChoiceItems(planNames, checkedIndex) { dialog, which ->
                    val radioId = when (planIds[which]) {
                        DynamicBibleReadingPlan.ID_SEQUENTIAL -> R.id.planSequential
                        DynamicBibleReadingPlan.ID_CHAPTER_A_DAY -> R.id.planChapterADay
                        else -> R.id.planMcCheyne
                    }
                    dialog.dismiss()
                    planGroup.check(radioId)
                    if (radioId == R.id.planChapterADay) {
                        showChapterADayBookPicker()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun showChapterADayBookPicker() {
        val bookNames = (1..66).map { BookNames.ZH[it] ?: "第${it}卷" }.toTypedArray()
        val currentBook = prefs.getInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_BOOK, 1)
        AlertDialog.Builder(this)
            .setTitle("从哪卷书开始？")
            .setSingleChoiceItems(bookNames, currentBook - 1) { dialog, which ->
                dialog.dismiss()
                showChapterADayChapterPicker(which + 1)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showChapterADayChapterPicker(book: Int) {
        val savedBook = prefs.getInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_BOOK, 1)
        val savedChapter = prefs.getInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_CHAPTER, 1)
        val chapterPicker = NumberPicker(this).apply {
            minValue = 1
            maxValue = SequentialPlan.BOOK_CHAPTER_COUNTS[book - 1]
            value = if (book == savedBook) savedChapter.coerceIn(1, maxValue) else 1
            wrapSelectorWheel = false
        }
        val bookName = BookNames.ZH[book] ?: "第${book}卷"
        AlertDialog.Builder(this)
            .setTitle("选择${bookName}的起始章节")
            .setView(chapterPicker)
            .setNegativeButton("返回") { _, _ -> showChapterADayBookPicker() }
            .setPositiveButton("确定") { _, _ ->
                prefs.edit()
                    .putInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_BOOK, book)
                    .putInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_CHAPTER, chapterPicker.value)
                    .apply()
                readingPlan.setCurrentDay(1, LocalDate.now())
                updateBibleProgress()
            }
            .show()
    }

    private fun updateBibleProgress() {
        val today = LocalDate.now()
        val planName = when (prefs.getString(
            DynamicBibleReadingPlan.KEY,
            DynamicBibleReadingPlan.ID_MCCHEYNE
        )) {
            DynamicBibleReadingPlan.ID_SEQUENTIAL -> "顺序读经（创世记 → 启示录）"
            DynamicBibleReadingPlan.ID_CHAPTER_A_DAY -> {
                val book = prefs.getInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_BOOK, 1)
                val chapter = prefs.getInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_CHAPTER, 1)
                "每天三章 · ${BookNames.ZH[book] ?: "第${book}卷"}${chapter}章起"
            }
            else -> "麦大卫一年读经"
        }
        findViewById<TextView>(R.id.biblePlanSelector).text = "$planName  ⌄"
        findViewById<TextView>(R.id.bibleProgress).text =
            "第 ${readingPlan.getCurrentDay(today)} / ${readingPlan.getTotalDays()} 天"
        val titles = readingPlan.getReadingForDate(today).map { it.toChineseTitle() }
        findViewById<TextView>(R.id.bibleReadingPreview).text =
            titles.mapIndexed { index, title -> "${index + 1}   $title" }.joinToString("\n")
    }

    private fun bindChineseSpeechRate(seekBar: SeekBar, label: TextView) {
        val saved = prefs.getFloat(
            AndroidTtsEngine.KEY_CHINESE_SPEECH_RATE,
            AndroidTtsEngine.DEFAULT_CHINESE_SPEECH_RATE
        )
        seekBar.progress = ((saved - 0.70f) * 100).toInt().coerceIn(0, seekBar.max)

        fun updateLabel(rate: Float) {
            val description = when {
                rate < 0.82f -> "慢"
                rate < 0.95f -> "自然"
                rate < 1.08f -> "标准"
                else -> "快"
            }
            label.text = "中文语速：$description（%.2f×）".format(rate)
        }
        updateLabel(saved)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                val rate = 0.70f + progress / 100f
                updateLabel(rate)
                if (fromUser) {
                    prefs.edit()
                        .putFloat(AndroidTtsEngine.KEY_CHINESE_SPEECH_RATE, rate)
                        .apply()
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar) = Unit
            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })
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
                        refreshHomeSummaries()
                    } else {
                        client.lastLocation.addOnSuccessListener { last ->
                            if (last != null) {
                                locationRepo.save(last.latitude, last.longitude)
                                updateLocationStatus()
                                refreshHomeSummaries()
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
