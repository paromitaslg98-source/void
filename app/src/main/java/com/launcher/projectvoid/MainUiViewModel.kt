package com.launcher.projectvoid

import android.app.Application
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.Gravity
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.usage.UsageStatsManager
import com.launcher.projectvoid.data.Prefs
import com.launcher.projectvoid.data.Prefs.SwipeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MainUiState(
    val currentTime: String = "",
    val currentDate: String = "",
    val batteryLevel: Int = 100,
    val screenTime: String = "",
    val homeApps: List<HomeApp> = emptyList(),
    val clockHorizontalAlignment: Int = Gravity.START,
    val clockVerticalAlignment: Int = Gravity.BOTTOM,
    val appHorizontalAlignment: Int = Gravity.START,
    val appVerticalAlignment: Int = Gravity.BOTTOM,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showScreenTime: Boolean = true,
    val showStatusBar: Boolean = false,
    val homeAppsCount: Int = 4,
    val leftSwipeAction: String = SwipeAction.NOTIFICATION_SUMMARY,
    val rightSwipeAction: String = SwipeAction.WIDGETS,
    val clockSectionWeight: Float = 0.25f,
    val homeTextSizeScale: Float = 1.0f,
    val appDrawerTextSizeScale: Float = 1.0f,
    val appSpacingDp: Float = 16f,
    val enableGestures: Boolean = true,
    val appFont: String = "inter"
)

data class HomeApp(
    val position: Int,
    val label: String,
    val packageName: String,
    val activityClassName: String?,
    val userString: String,
    val isShortcut: Boolean = false,
    val shortcutId: String = ""
)

class MainUiViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val prefs = Prefs(appContext)

    private val _uiState = MutableStateFlow(
        MainUiState(
            // Seed with persisted value so first composition uses the correct visibility whenever possible.
            showStatusBar = prefs.showStatusBar
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) ?: 100
            _uiState.update { it.copy(batteryLevel = level) }
        }
    }

    init {
        // Start time ticker
        viewModelScope.launch {
            while (isActive) {
                val now = Date()
                _uiState.update {
                    it.copy(
                        currentTime = timeFormat.format(now),
                        currentDate = dateFormat.format(now).uppercase(Locale.getDefault())
                    )
                }
                delay(15_000L)
            }
        }

        // Register battery receiver
        try {
            appContext.registerReceiver(
                batteryReceiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        } catch (_: Exception) {}

        // Load prefs
        refreshFromPrefs()

        // Load screen time and refresh periodically
        loadScreenTime()
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                loadScreenTime()
            }
        }

        // Observe pref changes — fully reactive propagation
        viewModelScope.launch {
            prefs.homescreenPreferences.collect { hsPrefs ->
                val apps = loadHomeApps(hsPrefs.maxApps)
                _uiState.update {
                    it.copy(
                        clockHorizontalAlignment = hsPrefs.clockHorizontalAlignment,
                        clockVerticalAlignment = hsPrefs.clockVerticalAlignment,
                        appHorizontalAlignment = hsPrefs.appHorizontalAlignment,
                        appVerticalAlignment = hsPrefs.appVerticalAlignment,
                        showClock = hsPrefs.showClock,
                        showDate = hsPrefs.showDate,
                        showScreenTime = hsPrefs.showScreenTime,
                        homeAppsCount = hsPrefs.maxApps,
                        homeApps = apps,
                        showStatusBar = hsPrefs.showStatusBar,
                        leftSwipeAction = hsPrefs.leftSwipeAction,
                        rightSwipeAction = hsPrefs.rightSwipeAction,
                        clockSectionWeight = hsPrefs.clockSectionWeight,
                        homeTextSizeScale = hsPrefs.homeTextSizeScale,
                        appDrawerTextSizeScale = hsPrefs.appDrawerTextSizeScale,
                        appSpacingDp = hsPrefs.appSpacingDp,
                        enableGestures = hsPrefs.enableGestures,
                        appFont = hsPrefs.appFont
                    )
                }
            }
        }
    }

    private fun loadHomeApps(count: Int): List<HomeApp> {
        val apps = mutableListOf<HomeApp>()
        val limit = count.coerceIn(0, 10)
        for (i in 1..limit) {
            val name = prefs.getAppName(i)
            if (name.isNotBlank()) {
                apps.add(
                    HomeApp(
                        position = i,
                        label = name,
                        packageName = prefs.getAppPackage(i),
                        activityClassName = prefs.getAppActivityClassName(i),
                        userString = prefs.getAppUser(i),
                        isShortcut = prefs.getIsShortcut(i),
                        shortcutId = prefs.getShortcutId(i)
                    )
                )
            }
        }
        return apps
    }

    fun refreshFromPrefs() {
        val count = prefs.maxHomeApps.coerceIn(0, 10)
        val apps = loadHomeApps(count)
        _uiState.update {
            it.copy(
                homeApps = apps,
                homeAppsCount = count,
                clockHorizontalAlignment = prefs.clockAlignment,
                clockVerticalAlignment = prefs.clockVerticalAlignment,
                appHorizontalAlignment = prefs.homeAlignment,
                appVerticalAlignment = prefs.homeVerticalAlignment,
                showClock = prefs.showClockWidget,
                showDate = prefs.showDateWidget,
                showScreenTime = prefs.showScreenTimeWidget,
                showStatusBar = prefs.showStatusBar,
                leftSwipeAction = prefs.leftSwipeAction,
                rightSwipeAction = prefs.rightSwipeAction,
                clockSectionWeight = prefs.clockSectionWeight,
                homeTextSizeScale = prefs.homeTextSizeScale,
                appDrawerTextSizeScale = prefs.appDrawerTextSizeScale,
                appSpacingDp = prefs.appSpacingDp,
                appFont = prefs.appFont,
            )
        }
    }

    private fun loadScreenTime() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), appContext.packageName)
                } else {
                    @Suppress("DEPRECATION")
                    appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), appContext.packageName)
                }
                if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
                    _uiState.update { it.copy(screenTime = "Screen time: Permission required") }
                    return@launch
                }

                val usageStatsManager = getApplication<android.app.Application>().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    calendar.timeInMillis,
                    System.currentTimeMillis()
                )
                
                val totalMillis = stats
                    .filter { it.packageName != appContext.packageName }
                    .sumOf { it.totalTimeInForeground }
                if (totalMillis > 0) {
                    val hours = totalMillis / (1000 * 60 * 60)
                    val minutes = (totalMillis / (1000 * 60)) % 60
                    val text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                    _uiState.update { it.copy(screenTime = "Screen time: $text") }
                } else {
                    _uiState.update { it.copy(screenTime = "Screen time: None") }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(screenTime = "Screen time: Data unavailable") }
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = appContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName)
        }

        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onCleared() {
        try {
            appContext.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        super.onCleared()
    }
}
