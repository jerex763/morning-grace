package com.morninggrace.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Location
import android.location.Geocoder
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.text.format.Formatter
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.morninggrace.alarm.AlarmPermissionChecker
import com.morninggrace.alarm.AlarmScheduler
import com.morninggrace.alarm.AlarmService
import com.morninggrace.bible.BookNames
import com.morninggrace.bible.audio.BibleAudioLibrary
import com.morninggrace.bible.audio.InsufficientStorageException
import com.morninggrace.bible.plan.SequentialPlan
import com.morninggrace.bible.toChineseTitle
import com.morninggrace.core.model.AlarmConfig
import com.morninggrace.core.model.TimeGreeting
import com.morninggrace.core.model.WeatherData
import com.morninggrace.core.repository.LocationRepository
import com.morninggrace.core.repository.NewsRepository
import com.morninggrace.core.repository.WeatherRepository
import com.morninggrace.orchestrator.DynamicBibleReadingPlan
import com.morninggrace.tts.AndroidTtsEngine
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var permissionChecker: AlarmPermissionChecker
    @Inject lateinit var locationRepo: LocationRepository
    @Inject lateinit var readingPlan: DynamicBibleReadingPlan
    @Inject lateinit var weatherRepo: WeatherRepository
    @Inject lateinit var newsRepo: NewsRepository
    @Inject lateinit var bibleAudioLibrary: BibleAudioLibrary

    private lateinit var prefs: SharedPreferences
    private lateinit var locationStatus: TextView
    private lateinit var timeDisplay: TextView
    private lateinit var networkBanner: TextView

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = runOnUiThread { updateNetworkBanner() }
        override fun onLost(network: Network) = runOnUiThread { updateNetworkBanner() }
    }

    private var selectedHour = 6
    private var selectedMinute = 0
    private var isBroadcastPlaying = false
    private var playbackReceiverRegistered = false
    private var summaryRefreshGeneration = 0

    private val playbackStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AlarmService.ACTION_PLAYBACK_STATE -> {
                    updatePlaybackButton(
                        intent.getBooleanExtra(AlarmService.EXTRA_IS_PLAYING, false)
                    )
                }
                AlarmService.ACTION_PLAYBACK_ERROR -> {
                    updatePlaybackButton(false)
                    updateBibleAudioStatus()
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("暂时无法播放")
                        .setMessage(
                            intent.getStringExtra(AlarmService.EXTRA_PLAYBACK_ERROR)
                                ?: "请确认录音已经导入、媒体音量已开启，然后再试一次。"
                        )
                        .setPositiveButton("知道了", null)
                        .show()
                }
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updateSystemReadinessStatus()
        // Android only presents one runtime-permission dialog at a time.
        // Start location onboarding after the notification decision completes.
        refreshLocationAutomatically()
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) fetchLocation()
        else {
            locationRepo.useDefault()
            locationStatus.text = "未允许位置，天气默认使用北京"
            updateLocationStatus()
        }
    }

    private val bibleAudioImportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@registerForActivityResult
        val button = findViewById<MaterialButton>(R.id.importBibleAudioButton)
        val status = findViewById<TextView>(R.id.bibleAudioStatus)
        button.isEnabled = false
        status.text = "正在导入 ${uris.size} 个音频包，请保持应用开启…"
        lifecycleScope.launch {
            try {
                val result = bibleAudioLibrary.importZipArchives(contentResolver, uris) { progress ->
                    runOnUiThread {
                        status.text =
                            "正在导入第 ${progress.archiveNumber}/${progress.archiveCount} 个录音包\n" +
                                "已检查 ${progress.processedChapters} 章，请保持应用开启…"
                    }
                }
                updateBibleAudioStatus()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("真人圣经录音导入完成")
                    .setMessage(
                        "新增 ${result.imported} 章\n" +
                            "已有 ${result.alreadyPresent} 章\n" +
                            "忽略 ${result.ignored} 个无法识别的文件\n" +
                            "失败 ${result.failedArchives} 个压缩包"
                    )
                    .setPositiveButton("好", null)
                    .show()
            } catch (error: InsufficientStorageException) {
                status.text = "空间不足，录音尚未导入"
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("手机空间不足")
                    .setMessage(error.message)
                    .setPositiveButton("好", null)
                    .show()
            } finally {
                button.isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("alarm_prefs", MODE_PRIVATE)
        initializeDefaultReadingPlan()
        val waitingForNotificationPermission = requestNotificationPermissionIfNeeded()

        val alarmSwitch = findViewById<SwitchMaterial>(R.id.alarmSwitch)
        val warning = findViewById<TextView>(R.id.permissionWarning)
        locationStatus = findViewById(R.id.locationStatus)
        timeDisplay = findViewById(R.id.timeDisplay)
        networkBanner = findViewById(R.id.networkBanner)
        val today = LocalDate.now()
        findViewById<TextView>(R.id.greetingDisplay).text =
            TimeGreeting.forTime(LocalTime.now())
        val weekday = arrayOf("一", "二", "三", "四", "五", "六", "日")[today.dayOfWeek.value - 1]
        findViewById<TextView>(R.id.dateDisplay).text =
            "${today.year}年${today.monthValue}月${today.dayOfMonth}日  星期$weekday"

        selectedHour = prefs.getInt("hour", 6)
        selectedMinute = prefs.getInt("minute", 0)
        alarmSwitch.isChecked = prefs.getBoolean("enabled", false)
        updateTimeDisplay()
        updateLocationStatus()
        updateSystemReadinessStatus()
        if (!waitingForNotificationPermission) {
            refreshLocationAutomatically()
        }
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
            if (enabled && !hasNotificationPermission()) {
                warning.text = "请先允许通知，锁屏后才能看到停止播放按钮"
                warning.visibility = View.VISIBLE
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                alarmSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }
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
        findViewById<MaterialButton>(R.id.backgroundProtectionButton).setOnClickListener {
            showBackgroundProtectionGuide()
        }

        // Module toggles
        bindModuleCheckbox(R.id.moduleWeather, AlarmService.KEY_MODULE_WEATHER)
        bindModuleCheckbox(R.id.moduleNews,    AlarmService.KEY_MODULE_NEWS)
        bindModuleCheckbox(R.id.newsFullArticles, AlarmService.KEY_NEWS_FULL_ARTICLES, false)

        // Bible broadcast settings. The home reading preview remains visible even
        // when Bible audio is excluded from the scheduled broadcast.
        val moduleBible = findViewById<SwitchMaterial>(R.id.moduleBible)
        val planGroup   = findViewById<RadioGroup>(R.id.planRadioGroup)
        val bibleEnglish = findViewById<SwitchMaterial>(R.id.bibleEnglish)
        val bibleRecordedAudio = findViewById<SwitchMaterial>(R.id.bibleRecordedAudio)
        val bibleAudioStatus = findViewById<TextView>(R.id.bibleAudioStatus)
        val importBibleAudioButton = findViewById<MaterialButton>(R.id.importBibleAudioButton)
        val progressRow = findViewById<LinearLayout>(R.id.bibleProgressRow)
        val readingPreview = findViewById<TextView>(R.id.bibleReadingPreview)
        val speechRateLabel = findViewById<TextView>(R.id.chineseSpeechRateLabel)
        val speechRate = findViewById<SeekBar>(R.id.chineseSpeechRate)
        moduleBible.isChecked = prefs.getBoolean(AlarmService.KEY_MODULE_BIBLE, true)
        bibleEnglish.isChecked = false
        bibleRecordedAudio.isChecked =
            prefs.getBoolean(AlarmService.KEY_BIBLE_RECORDED_AUDIO, true)
        planGroup.visibility  = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        bibleEnglish.visibility = View.GONE
        bibleRecordedAudio.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        bibleAudioStatus.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        importBibleAudioButton.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        progressRow.visibility = View.VISIBLE
        readingPreview.visibility = View.VISIBLE
        speechRateLabel.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        speechRate.visibility = if (moduleBible.isChecked) View.VISIBLE else View.GONE
        moduleBible.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AlarmService.KEY_MODULE_BIBLE, checked).apply()
            planGroup.visibility = if (checked) View.VISIBLE else View.GONE
            bibleEnglish.visibility = View.GONE
            bibleRecordedAudio.visibility = if (checked) View.VISIBLE else View.GONE
            bibleAudioStatus.visibility = if (checked) View.VISIBLE else View.GONE
            importBibleAudioButton.visibility = if (checked) View.VISIBLE else View.GONE
            progressRow.visibility = View.VISIBLE
            readingPreview.visibility = View.VISIBLE
            speechRateLabel.visibility = if (checked) View.VISIBLE else View.GONE
            speechRate.visibility = if (checked) View.VISIBLE else View.GONE
        }
        bibleEnglish.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AlarmService.KEY_BIBLE_ENGLISH, checked).apply()
        }
        bibleRecordedAudio.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AlarmService.KEY_BIBLE_RECORDED_AUDIO, checked).apply()
        }
        importBibleAudioButton.setOnClickListener {
            bibleAudioImportLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
        }
        updateBibleAudioStatus()
        val savedPlan = prefs.getString(
            DynamicBibleReadingPlan.KEY,
            DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
        )
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

        // Dev: test broadcast (uses current module prefs)
        findViewById<MaterialButton>(R.id.testBroadcastButton).setOnClickListener {
            if (isBroadcastPlaying) {
                startService(
                    Intent(this, AlarmService::class.java).setAction(AlarmService.ACTION_STOP)
                )
            } else {
                ContextCompat.startForegroundService(this, Intent(this, AlarmService::class.java))
            }
        }
        updatePlaybackButton(prefs.getBoolean(AlarmService.KEY_PLAYBACK_ACTIVE, false))
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            playbackStateReceiver,
            IntentFilter().apply {
                addAction(AlarmService.ACTION_PLAYBACK_STATE)
                addAction(AlarmService.ACTION_PLAYBACK_ERROR)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        playbackReceiverRegistered = true
        val cm = getSystemService(ConnectivityManager::class.java)
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, networkCallback)
        updateNetworkBanner()
    }

    override fun onStop() {
        super.onStop()
        getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
        if (playbackReceiverRegistered) {
            unregisterReceiver(playbackStateReceiver)
            playbackReceiverRegistered = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionChecker.canScheduleExactAlarms()) {
            findViewById<TextView>(R.id.permissionWarning).visibility = View.GONE
        }
        updateLocationStatus()
        updateSystemReadinessStatus()
        updateBibleProgress()
        updatePlaybackButton(prefs.getBoolean(AlarmService.KEY_PLAYBACK_ACTIVE, false))
    }

    private fun updateNetworkBanner() {
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val connected = connectivity.activeNetwork?.let { network ->
            connectivity.getNetworkCapabilities(network)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } == true
        networkBanner.visibility = if (connected) View.GONE else View.VISIBLE
    }

    private fun updateSystemReadinessStatus() {
        val notificationReady = hasNotificationPermission()
        val batteryReady = getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(packageName)
        val audioManager = getSystemService(AudioManager::class.java)
        val mediaVolumeReady = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0
        val status = buildList {
            add(if (notificationReady) "✓ 通知栏停止按钮可用" else "⚠ 未允许通知，锁屏时看不到停止按钮")
            add(if (mediaVolumeReady) "✓ 播放音量已开启" else "⚠ 媒体音量为零，播报可能无声")
            add(if (batteryReady) "✓ 已允许后台持续播放" else "⚠ 请设置后台播放保护")
        }.joinToString("\n")
        findViewById<TextView>(R.id.systemReadinessStatus).text = status
    }

    private fun showBackgroundProtectionGuide() {
        AlertDialog.Builder(this)
            .setTitle("设置后台播放保护")
            .setMessage(
                "为了每天锁屏后也能准时播放，请完成下面设置：\n\n" +
                    "1. 允许忽略电池优化\n" +
                    "2. 在 OPPO 手机管家中允许“晨光”自启动和后台活动\n" +
                    "3. 不要在“一键清理”中关闭晨光\n" +
                    "4. 确认媒体音量不是零；播放时可直接用音量键调整"
            )
            .setNegativeButton("稍后", null)
            .setPositiveButton("打开设置") { _, _ -> openBatteryOptimizationSettings() }
            .show()
    }

    private fun openBatteryOptimizationSettings() {
        val power = getSystemService(PowerManager::class.java)
        val intent = if (!power.isIgnoringBatteryOptimizations(packageName)) {
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
        } else {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }
        runCatching { startActivity(intent) }
            .onFailure {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
    }

    private fun updatePlaybackButton(playing: Boolean) {
        isBroadcastPlaying = playing
        val button = findViewById<MaterialButton>(R.id.testBroadcastButton)
        if (playing) {
            button.text = "■  停止播放"
            button.contentDescription = "停止晨间播报"
            button.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.warning))
        } else {
            button.text = "▶  开始今天的播报"
            button.contentDescription = "开始今天的晨间播报"
            button.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_dark))
        }
    }

    private fun bindModuleCheckbox(viewId: Int, prefKey: String, defaultValue: Boolean = true) {
        val checkbox = findViewById<SwitchMaterial>(viewId)
        checkbox.isChecked = prefs.getBoolean(prefKey, defaultValue)
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
        val weatherIcon = findViewById<TextView>(R.id.weatherIcon)
        val newsView = findViewById<TextView>(R.id.newsSummary)
        val cache = getSharedPreferences("home_summary_cache", MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val location = locationRepo.get()
        val locationKey = "%.3f,%.3f".format(Locale.US, location.lat, location.lon)
        val generation = ++summaryRefreshGeneration

        val weatherFresh = cache.getString("weather_date", null) == today &&
            cache.getString("weather_location", null) == locationKey
        weatherView.text = if (weatherFresh) {
            cache.getString("weather", "正在更新…")
        } else {
            cache.getString("weather_date", null)?.let { "上次更新：$it\n正在获取今天的天气…" }
                ?: "正在更新…"
        }
        weatherIcon.text = if (weatherFresh) cache.getString("weather_icon", "🌤") else "🕒"

        val newsFresh = cache.getString("news_date", null) == today
        newsView.text = if (newsFresh) {
            cache.getString("news", "正在更新…")
        } else {
            cache.getString("news_date", null)?.let { "上次更新：$it\n正在获取今天的新闻…" }
                ?: "正在更新…"
        }

        lifecycleScope.launch {
            val weatherJob = async {
                weatherRepo.getCurrentWeather(location)
            }
            val newsJob = async { newsRepo.getTopHeadlines(3) }

            weatherJob.await()?.let { weather ->
                if (generation != summaryRefreshGeneration) return@let
                val summary = "${weather.locationName}  ${weather.temperatureCelsius.toInt()}°  ${weather.shortDescription()}\n" +
                    "湿度 ${weather.humidity}%"
                val icon = weather.icon()
                weatherView.text = summary
                weatherIcon.text = icon
                cache.edit()
                    .putString("weather", summary)
                    .putString("weather_icon", icon)
                    .putString("weather_date", today)
                    .putString("weather_location", locationKey)
                    .putLong("weather_updated_at", System.currentTimeMillis())
                    .apply()
            } ?: run {
                if (generation == summaryRefreshGeneration && !weatherFresh) {
                    weatherView.text = cache.getString("weather_date", null)?.let {
                        "今天的天气暂时无法获取\n上次更新：$it"
                    } ?: "天气暂时\n无法获取"
                    weatherIcon.text = "⚠"
                }
            }

            val headlines = newsJob.await()
            if (headlines.isNotEmpty() && generation == summaryRefreshGeneration) {
                val summary = headlines.take(3).mapIndexed { index, item ->
                    "${index + 1}. ${item.title}"
                }.joinToString("\n")
                newsView.text = summary
                cache.edit()
                    .putString("news", summary)
                    .putString("news_date", today)
                    .putLong("news_updated_at", System.currentTimeMillis())
                    .apply()
            } else if (generation == summaryRefreshGeneration && !newsFresh) {
                newsView.text = cache.getString("news_date", null)?.let {
                    "今天的新闻暂时无法获取\n上次更新：$it"
                } ?: "新闻暂时\n无法获取"
            }
        }
    }

    private fun WeatherData.shortDescription(): String =
        descriptionZh.ifBlank { when (weatherCode) {
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
    } }

    private fun WeatherData.icon(): String {
        val description = shortDescription()
        return when {
            "雷" in description -> "⛈"
            "雪" in description -> "🌨"
            "雨" in description -> "🌧"
            "雾" in description -> "🌫"
            "晴" in description -> "☀"
            "云" in description || "阴" in description -> "☁"
            else -> "🌤"
        }
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
                DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
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
            DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
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

    private fun updateBibleAudioStatus() {
        val stats = bibleAudioLibrary.stats()
        val size = Formatter.formatShortFileSize(this, stats.totalBytes)
        val ttsKnownUnavailable =
            prefs.contains(AlarmService.KEY_TTS_AVAILABLE) &&
                !prefs.getBoolean(AlarmService.KEY_TTS_AVAILABLE, true)
        findViewById<TextView>(R.id.bibleAudioStatus).text =
            if (stats.chapterCount == 0) {
                if (ttsKnownUnavailable) {
                    "系统中文语音不可用，请先导入真人圣经录音"
                } else {
                    "尚未导入真人录音；播报会使用中文系统语音"
                }
            } else {
                "真人录音：${stats.chapterCount} / ${BibleAudioLibrary.TOTAL_BIBLE_CHAPTERS} 章 · $size" +
                    if (ttsKnownUnavailable) "\n系统语音不可用，但真人录音仍可播放" else ""
            }
    }

    private fun initializeDefaultReadingPlan() {
        if (prefs.contains(DynamicBibleReadingPlan.KEY)) return

        prefs.edit()
            .putString(
                DynamicBibleReadingPlan.KEY,
                DynamicBibleReadingPlan.ID_CHAPTER_A_DAY
            )
            .putInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_BOOK, 1)
            .putInt(DynamicBibleReadingPlan.KEY_CHAPTER_A_DAY_CHAPTER, 1)
            .apply()
        readingPlan.setCurrentDay(1, LocalDate.now())
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
        val granted =
            ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            fetchLocation()
        } else {
            locationPermissionLauncher.launch(arrayOf(fine, coarse))
        }
    }

    private fun refreshLocationAutomatically() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val granted =
            ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            fetchLocation()
        } else {
            locationPermissionLauncher.launch(arrayOf(fine, coarse))
        }
    }

    private fun fetchLocation() {
        locationStatus.text = "正在获取位置..."
        val manager = getSystemService(LocationManager::class.java)
        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            useBeijingFallback("定位服务未开启")
            return
        }
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER
            else -> {
                useBeijingFallback("没有可用的定位服务")
                return
            }
        }
        try {
            LocationManagerCompat.getCurrentLocation(
                manager,
                provider,
                null as android.os.CancellationSignal?,
                ContextCompat.getMainExecutor(this)
            ) { current: Location? ->
                val location = current ?: lastKnownLocation(manager)
                if (location == null) {
                    useBeijingFallback("暂时无法取得当前位置")
                } else {
                    saveLocatedCity(location)
                }
            }
        } catch (e: SecurityException) {
            useBeijingFallback("位置权限不足")
        } catch (e: Exception) {
            useBeijingFallback("位置获取失败")
        }
    }

    @Suppress("MissingPermission")
    private fun lastKnownLocation(manager: LocationManager): Location? =
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }

    private fun saveLocatedCity(location: Location) {
        lifecycleScope.launch {
            if (location.latitude !in 18.0..54.0 ||
                location.longitude !in 73.0..135.0
            ) {
                useBeijingFallback("当前位置不在中国")
                return@launch
            }
            val cityName = withContext(Dispatchers.IO) {
                resolveCityName(location.latitude, location.longitude)
            }
            if (cityName == null) {
                useBeijingFallback("无法确认当前位置在中国")
                return@launch
            }
            locationRepo.save(
                location.latitude,
                location.longitude,
                cityName
            )
            updateLocationStatus()
            refreshHomeSummaries()
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveCityName(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) return null
        val address = runCatching {
            Geocoder(this, Locale.SIMPLIFIED_CHINESE)
                .getFromLocation(lat, lon, 1)
                ?.firstOrNull()
        }.getOrNull() ?: return null
        if (address.countryCode?.equals("CN", ignoreCase = true) != true) return null
        return sequenceOf(address.locality, address.subAdminArea, address.adminArea)
            .filterNotNull()
            .map { it.trim().removeSuffix("市").removeSuffix("地区") }
            .firstOrNull { it.isNotBlank() }
    }

    private fun useBeijingFallback(reason: String) {
        locationRepo.useDefault()
        locationStatus.text = "$reason，天气默认使用北京"
        refreshHomeSummaries()
    }

    private fun updateLocationStatus() {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val granted = fine || coarse
        findViewById<MaterialButton>(R.id.locationButton).apply {
            visibility = View.VISIBLE
            text = if (granted) "更新天气位置" else "允许获取天气位置"
        }
        locationStatus.text = if (granted && locationRepo.hasLocation()) {
            "天气位置：${locationRepo.get().cityName}"
        } else if (locationRepo.hasLocation()) {
            val loc = locationRepo.get()
            "使用上次位置：${loc.cityName}"
        } else {
            "未获取位置，天气默认使用北京"
        }
    }

    private fun requestNotificationPermissionIfNeeded(): Boolean {
        if (!hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return true
        }
        return false
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
}
