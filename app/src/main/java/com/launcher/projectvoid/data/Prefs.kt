package com.launcher.projectvoid.data

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomescreenPreferences(
    val appHorizontalAlignment: Int,
    val appVerticalAlignment: Int,
    val clockHorizontalAlignment: Int,
    val clockVerticalAlignment: Int,
    val showClock: Boolean,
    val showDate: Boolean,
    val showScreenTime: Boolean,
    val maxApps: Int,
    val clockSectionWeight: Float,
    val homeTextSizeScale: Float,
    val appDrawerTextSizeScale: Float,
    val appSpacingDp: Float,
    val showStatusBar: Boolean,
    val leftSwipeAction: String,
    val rightSwipeAction: String,
    val enableGestures: Boolean
)

class Prefs(context: Context) {
    private val PREFS_FILENAME = "com.launcher.void"
    private val OLD_PREFS_FILENAME = "com.launcher.projectvoid"

    private val FIRST_OPEN = "FIRST_OPEN"
    private val FIRST_OPEN_TIME = "FIRST_OPEN_TIME"
    private val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
    private val FIRST_HIDE = "FIRST_HIDE"
    private val USER_STATE = "USER_STATE"
    private val LOCK_MODE = "LOCK_MODE"
    private val HOME_APPS_NUM = "HOME_APPS_NUM"
    private val AUTO_SHOW_KEYBOARD = "AUTO_SHOW_KEYBOARD"
    private val KEYBOARD_MESSAGE = "KEYBOARD_MESSAGE"
    private val DAILY_WALLPAPER = "DAILY_WALLPAPER"
    private val DAILY_WALLPAPER_URL = "DAILY_WALLPAPER_URL"
    private val HOME_ALIGNMENT = "HOME_ALIGNMENT"
    private val HOME_BOTTOM_ALIGNMENT = "HOME_BOTTOM_ALIGNMENT"
    private val HOME_VERTICAL_ALIGNMENT = "HOME_VERTICAL_ALIGNMENT"
    private val APP_LABEL_ALIGNMENT = "APP_LABEL_ALIGNMENT"
    private val STATUS_BAR = "STATUS_BAR"
    private val DATE_TIME_VISIBILITY = "DATE_TIME_VISIBILITY"
    private val SWIPE_LEFT_ENABLED = "SWIPE_LEFT_ENABLED"
    private val SWIPE_RIGHT_ENABLED = "SWIPE_RIGHT_ENABLED"
    private val SWIPE_UP_ENABLED = "SWIPE_UP_ENABLED"
    private val HIDDEN_APPS = "HIDDEN_APPS"
    private val HIDDEN_APPS_UPDATED = "HIDDEN_APPS_UPDATED"
    private val SHOW_HINT_COUNTER = "SHOW_HINT_COUNTER"
    private val APP_THEME = "APP_THEME"
    private val ABOUT_CLICKED = "ABOUT_CLICKED"
    private val RATE_CLICKED = "RATE_CLICKED"
    private val WALLPAPER_MSG_SHOWN = "WALLPAPER_MSG_SHOWN"
    private val SHARE_SHOWN_TIME = "SHARE_SHOWN_TIME"
    private val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
    private val TEXT_SIZE_SCALE = "TEXT_SIZE_SCALE" // Legacy
    private val HOME_TEXT_SIZE_SCALE = "HOME_TEXT_SIZE_SCALE"
    private val APP_DRAWER_TEXT_SIZE_SCALE = "APP_DRAWER_TEXT_SIZE_SCALE"
    private val PRO_MESSAGE_SHOWN = "PRO_MESSAGE_SHOWN"
    private val HIDE_SET_DEFAULT_LAUNCHER = "HIDE_SET_DEFAULT_LAUNCHER"
    private val SCREEN_TIME_LAST_UPDATED = "SCREEN_TIME_LAST_UPDATED"
    private val LAUNCHER_RESTART_TIMESTAMP = "LAUNCHER_RECREATE_TIMESTAMP"
    private val SHOWN_ON_DAY_OF_YEAR = "SHOWN_ON_DAY_OF_YEAR"
    private val SHOW_CLOCK_WIDGET = "SHOW_CLOCK_WIDGET"
    private val SHOW_DATE_WIDGET = "SHOW_DATE_WIDGET"
    private val SHOW_SCREEN_TIME_WIDGET = "SHOW_SCREEN_TIME_WIDGET"
    private val CLOCK_ALIGNMENT = "CLOCK_ALIGNMENT"
    private val CLOCK_VERTICAL_ALIGNMENT = "CLOCK_VERTICAL_ALIGNMENT"
    private val LEFT_SWIPE_ACTION = "LEFT_SWIPE_ACTION"
    private val RIGHT_SWIPE_ACTION = "RIGHT_SWIPE_ACTION"
    private val MAX_HOME_APPS = "MAX_HOME_APPS"
    private val SHOW_ALPHABET_CATEGORIES = "SHOW_ALPHABET_CATEGORIES"
    private val CLOCK_SECTION_WEIGHT = "CLOCK_SECTION_WEIGHT"
    private val PRIVATE_SPACE_ENABLED = "PRIVATE_SPACE_ENABLED"
    private val ENABLE_GESTURES = "ENABLE_GESTURES"
    private val ENABLE_NOTIFICATION_SUMMARY = "ENABLE_NOTIFICATION_SUMMARY"
    private val ENABLE_WIDGETS = "ENABLE_WIDGETS"
    private val ENABLE_NOTES = "ENABLE_NOTES"
    private val APP_SPACING_DP = "APP_SPACING_DP"

    private val APP_NAME_1 = "APP_NAME_1"
    private val APP_NAME_2 = "APP_NAME_2"
    private val APP_NAME_3 = "APP_NAME_3"
    private val APP_NAME_4 = "APP_NAME_4"
    private val APP_NAME_5 = "APP_NAME_5"
    private val APP_NAME_6 = "APP_NAME_6"
    private val APP_NAME_7 = "APP_NAME_7"
    private val APP_NAME_8 = "APP_NAME_8"
    private val APP_NAME_9 = "APP_NAME_9"
    private val APP_NAME_10 = "APP_NAME_10"
    private val APP_PACKAGE_1 = "APP_PACKAGE_1"
    private val APP_PACKAGE_2 = "APP_PACKAGE_2"
    private val APP_PACKAGE_3 = "APP_PACKAGE_3"
    private val APP_PACKAGE_4 = "APP_PACKAGE_4"
    private val APP_PACKAGE_5 = "APP_PACKAGE_5"
    private val APP_PACKAGE_6 = "APP_PACKAGE_6"
    private val APP_PACKAGE_7 = "APP_PACKAGE_7"
    private val APP_PACKAGE_8 = "APP_PACKAGE_8"
    private val APP_PACKAGE_9 = "APP_PACKAGE_9"
    private val APP_PACKAGE_10 = "APP_PACKAGE_10"
    private val APP_ACTIVITY_CLASS_NAME_1 = "APP_ACTIVITY_CLASS_NAME_1"
    private val APP_ACTIVITY_CLASS_NAME_2 = "APP_ACTIVITY_CLASS_NAME_2"
    private val APP_ACTIVITY_CLASS_NAME_3 = "APP_ACTIVITY_CLASS_NAME_3"
    private val APP_ACTIVITY_CLASS_NAME_4 = "APP_ACTIVITY_CLASS_NAME_4"
    private val APP_ACTIVITY_CLASS_NAME_5 = "APP_ACTIVITY_CLASS_NAME_5"
    private val APP_ACTIVITY_CLASS_NAME_6 = "APP_ACTIVITY_CLASS_NAME_6"
    private val APP_ACTIVITY_CLASS_NAME_7 = "APP_ACTIVITY_CLASS_NAME_7"
    private val APP_ACTIVITY_CLASS_NAME_8 = "APP_ACTIVITY_CLASS_NAME_8"
    private val APP_ACTIVITY_CLASS_NAME_9 = "APP_ACTIVITY_CLASS_NAME_9"
    private val APP_ACTIVITY_CLASS_NAME_10 = "APP_ACTIVITY_CLASS_NAME_10"
    private val APP_USER_1 = "APP_USER_1"
    private val APP_USER_2 = "APP_USER_2"
    private val APP_USER_3 = "APP_USER_3"
    private val APP_USER_4 = "APP_USER_4"
    private val APP_USER_5 = "APP_USER_5"
    private val APP_USER_6 = "APP_USER_6"
    private val APP_USER_7 = "APP_USER_7"
    private val APP_USER_8 = "APP_USER_8"
    private val APP_USER_9 = "APP_USER_9"
    private val APP_USER_10 = "APP_USER_10"

    private val APP_NAME_SWIPE_LEFT = "APP_NAME_SWIPE_LEFT"
    private val APP_NAME_SWIPE_RIGHT = "APP_NAME_SWIPE_RIGHT"
    private val APP_PACKAGE_SWIPE_LEFT = "APP_PACKAGE_SWIPE_LEFT"
    private val APP_PACKAGE_SWIPE_RIGHT = "APP_PACKAGE_SWIPE_RIGHT"
    private val APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT = "APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT"
    private val APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT = "APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT"
    private val APP_USER_SWIPE_LEFT = "APP_USER_SWIPE_LEFT"
    private val APP_USER_SWIPE_RIGHT = "APP_USER_SWIPE_RIGHT"
    private val CLOCK_APP_PACKAGE = "CLOCK_APP_PACKAGE"
    private val CLOCK_APP_USER = "CLOCK_APP_USER"
    private val CLOCK_APP_CLASS_NAME = "CLOCK_APP_CLASS_NAME"
    private val CALENDAR_APP_PACKAGE = "CALENDAR_APP_PACKAGE"
    private val CALENDAR_APP_USER = "CALENDAR_APP_USER"
    private val CALENDAR_APP_CLASS_NAME = "CALENDAR_APP_CLASS_NAME"

    private val IS_SHORTCUT_1 = "IS_SHORTCUT_1"
    private val SHORTCUT_ID_1 = "SHORTCUT_ID_1"
    private val IS_SHORTCUT_2 = "IS_SHORTCUT_2"
    private val SHORTCUT_ID_2 = "SHORTCUT_ID_2"
    private val IS_SHORTCUT_3 = "IS_SHORTCUT_3"
    private val SHORTCUT_ID_3 = "SHORTCUT_ID_3"
    private val IS_SHORTCUT_4 = "IS_SHORTCUT_4"
    private val SHORTCUT_ID_4 = "SHORTCUT_ID_4"
    private val IS_SHORTCUT_5 = "IS_SHORTCUT_5"
    private val SHORTCUT_ID_5 = "SHORTCUT_ID_5"
    private val IS_SHORTCUT_6 = "IS_SHORTCUT_6"
    private val SHORTCUT_ID_6 = "SHORTCUT_ID_6"
    private val IS_SHORTCUT_7 = "IS_SHORTCUT_7"
    private val SHORTCUT_ID_7 = "SHORTCUT_ID_7"
    private val IS_SHORTCUT_8 = "IS_SHORTCUT_8"
    private val SHORTCUT_ID_8 = "SHORTCUT_ID_8"
    private val IS_SHORTCUT_9 = "IS_SHORTCUT_9"
    private val SHORTCUT_ID_9 = "SHORTCUT_ID_9"
    private val IS_SHORTCUT_10 = "IS_SHORTCUT_10"
    private val SHORTCUT_ID_10 = "SHORTCUT_ID_10"

    private val SHORTCUT_ID_SWIPE_LEFT = "SHORTCUT_ID_SWIPE_LEFT"
    private val IS_SHORTCUT_SWIPE_LEFT = "IS_SHORTCUT_SWIPE_LEFT"
    private val SHORTCUT_ID_SWIPE_RIGHT = "SHORTCUT_ID_SWIPE_RIGHT"
    private val IS_SHORTCUT_SWIPE_RIGHT = "IS_SHORTCUT_SWIPE_RIGHT"

    private val prefs: SharedPreferences

    private val homescreenPrefKeys = setOf(
        HOME_ALIGNMENT,
        HOME_VERTICAL_ALIGNMENT,
        HOME_BOTTOM_ALIGNMENT,
        SHOW_CLOCK_WIDGET,
        SHOW_DATE_WIDGET,
        SHOW_SCREEN_TIME_WIDGET,
        CLOCK_ALIGNMENT,
        CLOCK_VERTICAL_ALIGNMENT,
        MAX_HOME_APPS,
        CLOCK_SECTION_WEIGHT,
        HOME_TEXT_SIZE_SCALE,
        APP_DRAWER_TEXT_SIZE_SCALE,
        APP_SPACING_DP,
        STATUS_BAR,
        LEFT_SWIPE_ACTION,
        RIGHT_SWIPE_ACTION,
        ENABLE_GESTURES
    )

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in homescreenPrefKeys) emitHomescreenPrefs()
    }

    private val _homescreenPreferences: MutableStateFlow<HomescreenPreferences>
    val homescreenPreferences: StateFlow<HomescreenPreferences>

    init {
        // Migrate from old prefs file if needed
        val newPrefs = context.getSharedPreferences(PREFS_FILENAME, 0)
        val oldPrefs = context.getSharedPreferences(OLD_PREFS_FILENAME, 0)
        if (newPrefs.all.isEmpty() && oldPrefs.all.isNotEmpty()) {
            val editor = newPrefs.edit()
            oldPrefs.all.forEach { (key, value) ->
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is String -> editor.putString(key, value)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                }
            }
            editor.apply()
            // Clean up old file
            oldPrefs.edit().clear().apply()
        }
        prefs = newPrefs

        // Migrate homeAppsNum → maxHomeApps if MAX_HOME_APPS was never set
        if (!prefs.contains(MAX_HOME_APPS) && prefs.contains(HOME_APPS_NUM)) {
            prefs.edit { putInt(MAX_HOME_APPS, prefs.getInt(HOME_APPS_NUM, 4)) }
        }

        _homescreenPreferences = MutableStateFlow(readHomescreenPreferences())
        homescreenPreferences = _homescreenPreferences.asStateFlow()
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun emitHomescreenPrefs() {
        _homescreenPreferences.value = readHomescreenPreferences()
    }

    private fun readHomescreenPreferences(): HomescreenPreferences {
        return HomescreenPreferences(
            appHorizontalAlignment = prefs.getInt(HOME_ALIGNMENT, Gravity.START),
            appVerticalAlignment = prefs.getInt(
                HOME_VERTICAL_ALIGNMENT,
                if (prefs.getBoolean(HOME_BOTTOM_ALIGNMENT, true)) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
            ),
            clockHorizontalAlignment = prefs.getInt(CLOCK_ALIGNMENT, Gravity.START),
            clockVerticalAlignment = prefs.getInt(CLOCK_VERTICAL_ALIGNMENT, Gravity.BOTTOM),
            showClock = prefs.getBoolean(SHOW_CLOCK_WIDGET, prefs.getInt(DATE_TIME_VISIBILITY, Constants.DateTime.ON) == Constants.DateTime.ON),
            showDate = prefs.getBoolean(SHOW_DATE_WIDGET, prefs.getInt(DATE_TIME_VISIBILITY, Constants.DateTime.ON) != Constants.DateTime.OFF),
            showScreenTime = prefs.getBoolean(SHOW_SCREEN_TIME_WIDGET, true),
            maxApps = prefs.getInt(MAX_HOME_APPS, prefs.getInt(HOME_APPS_NUM, 4)),
            clockSectionWeight = prefs.getFloat(CLOCK_SECTION_WEIGHT, 0.25f),
            homeTextSizeScale = prefs.getFloat(HOME_TEXT_SIZE_SCALE, prefs.getFloat(TEXT_SIZE_SCALE, 1.0f)),
            appDrawerTextSizeScale = prefs.getFloat(APP_DRAWER_TEXT_SIZE_SCALE, prefs.getFloat(TEXT_SIZE_SCALE, 1.0f)),
            appSpacingDp = prefs.getFloat(APP_SPACING_DP, 16f),
            showStatusBar = prefs.getBoolean(STATUS_BAR, false),
            leftSwipeAction = prefs.getString(LEFT_SWIPE_ACTION, SwipeAction.NOTIFICATION_SUMMARY) ?: SwipeAction.NOTIFICATION_SUMMARY,
            rightSwipeAction = prefs.getString(RIGHT_SWIPE_ACTION, SwipeAction.WIDGETS) ?: SwipeAction.WIDGETS,
            enableGestures = prefs.getBoolean(ENABLE_GESTURES, true)
        )
    }

    fun resetHomescreenDefaults() {
        homeAlignment = Gravity.START
        homeVerticalAlignment = Gravity.BOTTOM
        showClockWidget = true
        showDateWidget = true
        showScreenTimeWidget = true
        emitHomescreenPrefs()
    }

    fun registerPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    var firstOpen: Boolean
        get() = prefs.getBoolean(FIRST_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_OPEN, value).apply() }

    var firstOpenTime: Long
        get() = prefs.getLong(FIRST_OPEN_TIME, 0L)
        set(value) = prefs.edit { putLong(FIRST_OPEN_TIME, value).apply() }

    var firstSettingsOpen: Boolean
        get() = prefs.getBoolean(FIRST_SETTINGS_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_SETTINGS_OPEN, value).apply() }

    var firstHide: Boolean
        get() = prefs.getBoolean(FIRST_HIDE, true)
        set(value) = prefs.edit { putBoolean(FIRST_HIDE, value).apply() }

    var userState: String
        get() = prefs.getString(USER_STATE, Constants.UserState.START).toString()
        set(value) = prefs.edit { putString(USER_STATE, value).apply() }

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit { putBoolean(LOCK_MODE, value).apply() }

    var autoShowKeyboard: Boolean
        get() = prefs.getBoolean(AUTO_SHOW_KEYBOARD, true)
        set(value) = prefs.edit { putBoolean(AUTO_SHOW_KEYBOARD, value).apply() }

    var keyboardMessageShown: Boolean
        get() = prefs.getBoolean(KEYBOARD_MESSAGE, false)
        set(value) = prefs.edit { putBoolean(KEYBOARD_MESSAGE, value).apply() }

    var dailyWallpaper: Boolean
        get() = prefs.getBoolean(DAILY_WALLPAPER, false)
        set(value) = prefs.edit { putBoolean(DAILY_WALLPAPER, value).apply() }

    var dailyWallpaperUrl: String
        get() = prefs.getString(DAILY_WALLPAPER_URL, "").toString()
        set(value) = prefs.edit { putString(DAILY_WALLPAPER_URL, value).apply() }

    /** @deprecated Use maxHomeApps instead. Reads from MAX_HOME_APPS with fallback. */
    var homeAppsNum: Int
        get() = prefs.getInt(MAX_HOME_APPS, prefs.getInt(HOME_APPS_NUM, 4))
        set(value) {
            prefs.edit { putInt(MAX_HOME_APPS, value) }
            prefs.edit { putInt(HOME_APPS_NUM, value) }
        }

    var homeAlignment: Int
        get() = prefs.getInt(HOME_ALIGNMENT, Gravity.START)
        set(value) {
            prefs.edit { putInt(HOME_ALIGNMENT, value) }
            emitHomescreenPrefs()
        }

    var homeBottomAlignment: Boolean
        get() = prefs.getBoolean(HOME_BOTTOM_ALIGNMENT, true)
        set(value) {
            prefs.edit { putBoolean(HOME_BOTTOM_ALIGNMENT, value).apply() }
            homeVerticalAlignment = if (value) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        }

    var homeVerticalAlignment: Int
        get() = prefs.getInt(
            HOME_VERTICAL_ALIGNMENT,
            if (homeBottomAlignment) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
        )
        set(value) {
            prefs.edit { putInt(HOME_VERTICAL_ALIGNMENT, value).apply() }
            prefs.edit { putBoolean(HOME_BOTTOM_ALIGNMENT, value == Gravity.BOTTOM).apply() }
            emitHomescreenPrefs()
        }

    var showClockWidget: Boolean
        get() = prefs.getBoolean(SHOW_CLOCK_WIDGET, dateTimeVisibility == Constants.DateTime.ON)
        set(value) {
            prefs.edit { putBoolean(SHOW_CLOCK_WIDGET, value).apply() }
            emitHomescreenPrefs()
        }

    var showDateWidget: Boolean
        get() = prefs.getBoolean(SHOW_DATE_WIDGET, dateTimeVisibility != Constants.DateTime.OFF)
        set(value) {
            prefs.edit { putBoolean(SHOW_DATE_WIDGET, value).apply() }
            emitHomescreenPrefs()
        }

    var showScreenTimeWidget: Boolean
        get() = prefs.getBoolean(SHOW_SCREEN_TIME_WIDGET, true)
        set(value) {
            prefs.edit { putBoolean(SHOW_SCREEN_TIME_WIDGET, value).apply() }
            emitHomescreenPrefs()
        }

    var appLabelAlignment: Int
        get() = prefs.getInt(APP_LABEL_ALIGNMENT, Gravity.START)
        set(value) = prefs.edit { putInt(APP_LABEL_ALIGNMENT, value).apply() }

    var showStatusBar: Boolean
        get() = prefs.getBoolean(STATUS_BAR, false)
        set(value) = prefs.edit { putBoolean(STATUS_BAR, value).apply() }

    var dateTimeVisibility: Int
        get() = prefs.getInt(DATE_TIME_VISIBILITY, Constants.DateTime.ON)
        set(value) = prefs.edit { putInt(DATE_TIME_VISIBILITY, value).apply() }

    var swipeLeftEnabled: Boolean
        get() = prefs.getBoolean(SWIPE_LEFT_ENABLED, true)
        set(value) = prefs.edit { putBoolean(SWIPE_LEFT_ENABLED, value).apply() }

    var swipeRightEnabled: Boolean
        get() = prefs.getBoolean(SWIPE_RIGHT_ENABLED, true)
        set(value) = prefs.edit { putBoolean(SWIPE_RIGHT_ENABLED, value).apply() }

    var swipeUpEnabled: Boolean
        get() = prefs.getBoolean(SWIPE_UP_ENABLED, true)
        set(value) = prefs.edit { putBoolean(SWIPE_UP_ENABLED, value).apply() }

    var appTheme: Int
        get() = prefs.getInt(APP_THEME, AppCompatDelegate.MODE_NIGHT_YES)
        set(value) = prefs.edit { putInt(APP_THEME, value).apply() }

    var textSizeScale: Float // Legacy, kept for migration
        get() = prefs.getFloat(TEXT_SIZE_SCALE, 1.0f)
        set(value) = prefs.edit { putFloat(TEXT_SIZE_SCALE, value).apply() }

    var homeTextSizeScale: Float
        get() = prefs.getFloat(HOME_TEXT_SIZE_SCALE, textSizeScale) // Migration fallback
        set(value) = prefs.edit { putFloat(HOME_TEXT_SIZE_SCALE, value).apply() }
        
    var appDrawerTextSizeScale: Float
        get() = prefs.getFloat(APP_DRAWER_TEXT_SIZE_SCALE, textSizeScale) // Migration fallback
        set(value) = prefs.edit { putFloat(APP_DRAWER_TEXT_SIZE_SCALE, value).apply() }

    var proMessageShown: Boolean
        get() = prefs.getBoolean(PRO_MESSAGE_SHOWN, false)
        set(value) = prefs.edit { putBoolean(PRO_MESSAGE_SHOWN, value).apply() }

    var hideSetDefaultLauncher: Boolean
        get() = prefs.getBoolean(HIDE_SET_DEFAULT_LAUNCHER, false)
        set(value) = prefs.edit { putBoolean(HIDE_SET_DEFAULT_LAUNCHER, value).apply() }

    var screenTimeLastUpdated: Long
        get() = prefs.getLong(SCREEN_TIME_LAST_UPDATED, 0L)
        set(value) = prefs.edit { putLong(SCREEN_TIME_LAST_UPDATED, value).apply() }

    var launcherRestartTimestamp: Long
        get() = prefs.getLong(LAUNCHER_RESTART_TIMESTAMP, 0L)
        set(value) = prefs.edit { putLong(LAUNCHER_RESTART_TIMESTAMP, value).apply() }

    var shownOnDayOfYear: Int
        get() = prefs.getInt(SHOWN_ON_DAY_OF_YEAR, 0)
        set(value) = prefs.edit { putInt(SHOWN_ON_DAY_OF_YEAR, value).apply() }

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet(HIDDEN_APPS, value).apply() }

    var hiddenAppsUpdated: Boolean
        get() = prefs.getBoolean(HIDDEN_APPS_UPDATED, false)
        set(value) = prefs.edit { putBoolean(HIDDEN_APPS_UPDATED, value).apply() }

    var toShowHintCounter: Int
        get() = prefs.getInt(SHOW_HINT_COUNTER, 1)
        set(value) = prefs.edit { putInt(SHOW_HINT_COUNTER, value).apply() }

    var aboutClicked: Boolean
        get() = prefs.getBoolean(ABOUT_CLICKED, false)
        set(value) = prefs.edit { putBoolean(ABOUT_CLICKED, value).apply() }

    var rateClicked: Boolean
        get() = prefs.getBoolean(RATE_CLICKED, false)
        set(value) = prefs.edit { putBoolean(RATE_CLICKED, value).apply() }

    var wallpaperMsgShown: Boolean
        get() = prefs.getBoolean(WALLPAPER_MSG_SHOWN, false)
        set(value) = prefs.edit { putBoolean(WALLPAPER_MSG_SHOWN, value).apply() }

    var shareShownTime: Long
        get() = prefs.getLong(SHARE_SHOWN_TIME, 0L)
        set(value) = prefs.edit { putLong(SHARE_SHOWN_TIME, value).apply() }

    var swipeDownAction: Int
        get() = prefs.getInt(SWIPE_DOWN_ACTION, Constants.SwipeDownAction.NOTIFICATIONS)
        set(value) = prefs.edit { putInt(SWIPE_DOWN_ACTION, value).apply() }

    var appName1: String
        get() = prefs.getString(APP_NAME_1, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_1, value).apply() }

    var appName2: String
        get() = prefs.getString(APP_NAME_2, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_2, value).apply() }

    var appName3: String
        get() = prefs.getString(APP_NAME_3, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_3, value).apply() }

    var appName4: String
        get() = prefs.getString(APP_NAME_4, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_4, value).apply() }

    var appName5: String
        get() = prefs.getString(APP_NAME_5, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_5, value).apply() }

    var appName6: String
        get() = prefs.getString(APP_NAME_6, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_6, value).apply() }

    var appName7: String
        get() = prefs.getString(APP_NAME_7, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_7, value).apply() }

    var appName8: String
        get() = prefs.getString(APP_NAME_8, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_8, value).apply() }

    var appName9: String
        get() = prefs.getString(APP_NAME_9, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_9, value).apply() }

    var appName10: String
        get() = prefs.getString(APP_NAME_10, "").toString()
        set(value) = prefs.edit { putString(APP_NAME_10, value).apply() }

    var appPackage1: String
        get() = prefs.getString(APP_PACKAGE_1, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_1, value).apply() }

    var appPackage2: String
        get() = prefs.getString(APP_PACKAGE_2, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_2, value).apply() }

    var appPackage3: String
        get() = prefs.getString(APP_PACKAGE_3, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_3, value).apply() }

    var appPackage4: String
        get() = prefs.getString(APP_PACKAGE_4, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_4, value).apply() }

    var appPackage5: String
        get() = prefs.getString(APP_PACKAGE_5, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_5, value).apply() }

    var appPackage6: String
        get() = prefs.getString(APP_PACKAGE_6, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_6, value).apply() }

    var appPackage7: String
        get() = prefs.getString(APP_PACKAGE_7, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_7, value).apply() }

    var appPackage8: String
        get() = prefs.getString(APP_PACKAGE_8, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_8, value).apply() }

    var appPackage9: String
        get() = prefs.getString(APP_PACKAGE_9, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_9, value).apply() }

    var appPackage10: String
        get() = prefs.getString(APP_PACKAGE_10, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_10, value).apply() }

    var appActivityClassName1: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_1, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_1, value).apply() }

    var appActivityClassName2: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_2, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_2, value).apply() }

    var appActivityClassName3: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_3, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_3, value).apply() }

    var appActivityClassName4: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_4, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_4, value).apply() }

    var appActivityClassName5: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_5, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_5, value).apply() }

    var appActivityClassName6: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_6, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_6, value).apply() }

    var appActivityClassName7: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_7, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_7, value).apply() }

    var appActivityClassName8: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_8, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_8, value).apply() }

    var appActivityClassName9: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_9, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_9, value).apply() }

    var appActivityClassName10: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_10, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_10, value).apply() }

    var appUser1: String
        get() = prefs.getString(APP_USER_1, "").toString()
        set(value) = prefs.edit { putString(APP_USER_1, value).apply() }

    var appUser2: String
        get() = prefs.getString(APP_USER_2, "").toString()
        set(value) = prefs.edit { putString(APP_USER_2, value).apply() }

    var appUser3: String
        get() = prefs.getString(APP_USER_3, "").toString()
        set(value) = prefs.edit { putString(APP_USER_3, value).apply() }

    var appUser4: String
        get() = prefs.getString(APP_USER_4, "").toString()
        set(value) = prefs.edit { putString(APP_USER_4, value).apply() }

    var appUser5: String
        get() = prefs.getString(APP_USER_5, "").toString()
        set(value) = prefs.edit { putString(APP_USER_5, value).apply() }

    var appUser6: String
        get() = prefs.getString(APP_USER_6, "").toString()
        set(value) = prefs.edit { putString(APP_USER_6, value).apply() }

    var appUser7: String
        get() = prefs.getString(APP_USER_7, "").toString()
        set(value) = prefs.edit { putString(APP_USER_7, value).apply() }

    var appUser8: String
        get() = prefs.getString(APP_USER_8, "").toString()
        set(value) = prefs.edit { putString(APP_USER_8, value).apply() }

    var appUser9: String
        get() = prefs.getString(APP_USER_9, "").toString()
        set(value) = prefs.edit { putString(APP_USER_9, value).apply() }

    var appUser10: String
        get() = prefs.getString(APP_USER_10, "").toString()
        set(value) = prefs.edit { putString(APP_USER_10, value).apply() }

    var appNameSwipeLeft: String
        get() = prefs.getString(APP_NAME_SWIPE_LEFT, "Camera").toString()
        set(value) = prefs.edit { putString(APP_NAME_SWIPE_LEFT, value).apply() }

    var appNameSwipeRight: String
        get() = prefs.getString(APP_NAME_SWIPE_RIGHT, "Phone").toString()
        set(value) = prefs.edit { putString(APP_NAME_SWIPE_RIGHT, value).apply() }

    var appPackageSwipeLeft: String
        get() = prefs.getString(APP_PACKAGE_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_SWIPE_LEFT, value).apply() }

    var appActivityClassNameSwipeLeft: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_SWIPE_LEFT, value).apply() }

    var appPackageSwipeRight: String
        get() = prefs.getString(APP_PACKAGE_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_SWIPE_RIGHT, value).apply() }

    var appActivityClassNameRight: String?
        get() = prefs.getString(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_ACTIVITY_CLASS_NAME_SWIPE_RIGHT, value).apply() }

    var appUserSwipeLeft: String
        get() = prefs.getString(APP_USER_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_USER_SWIPE_LEFT, value).apply() }

    var appUserSwipeRight: String
        get() = prefs.getString(APP_USER_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_USER_SWIPE_RIGHT, value).apply() }

    var clockAppPackage: String
        get() = prefs.getString(CLOCK_APP_PACKAGE, "").toString()
        set(value) = prefs.edit { putString(CLOCK_APP_PACKAGE, value).apply() }

    var clockAppUser: String
        get() = prefs.getString(CLOCK_APP_USER, "").toString()
        set(value) = prefs.edit { putString(CLOCK_APP_USER, value).apply() }

    var clockAppClassName: String?
        get() = prefs.getString(CLOCK_APP_CLASS_NAME, "").toString()
        set(value) = prefs.edit { putString(CLOCK_APP_CLASS_NAME, value).apply() }

    var calendarAppPackage: String
        get() = prefs.getString(CALENDAR_APP_PACKAGE, "").toString()
        set(value) = prefs.edit { putString(CALENDAR_APP_PACKAGE, value).apply() }

    var calendarAppUser: String
        get() = prefs.getString(CALENDAR_APP_USER, "").toString()
        set(value) = prefs.edit { putString(CALENDAR_APP_USER, value).apply() }

    var calendarAppClassName: String?
        get() = prefs.getString(CALENDAR_APP_CLASS_NAME, "").toString()
        set(value) = prefs.edit { putString(CALENDAR_APP_CLASS_NAME, value).apply() }

    var isShortcut1: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_1, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_1, value).apply()
    var shortcutId1: String
        get() = prefs.getString(SHORTCUT_ID_1, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_1, value).apply()
    var isShortcut2: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_2, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_2, value).apply()
    var shortcutId2: String
        get() = prefs.getString(SHORTCUT_ID_2, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_2, value).apply()
    var isShortcut3: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_3, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_3, value).apply()
    var shortcutId3: String
        get() = prefs.getString(SHORTCUT_ID_3, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_3, value).apply()
    var isShortcut4: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_4, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_4, value).apply()
    var shortcutId4: String
        get() = prefs.getString(SHORTCUT_ID_4, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_4, value).apply()
    var isShortcut5: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_5, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_5, value).apply()
    var shortcutId5: String
        get() = prefs.getString(SHORTCUT_ID_5, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_5, value).apply()
    var isShortcut6: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_6, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_6, value).apply()
    var shortcutId6: String
        get() = prefs.getString(SHORTCUT_ID_6, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_6, value).apply()
    var isShortcut7: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_7, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_7, value).apply()
    var shortcutId7: String
        get() = prefs.getString(SHORTCUT_ID_7, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_7, value).apply()
    var isShortcut8: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_8, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_8, value).apply()
    var shortcutId8: String
        get() = prefs.getString(SHORTCUT_ID_8, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_8, value).apply()
    var isShortcut9: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_9, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_9, value).apply()
    var shortcutId9: String
        get() = prefs.getString(SHORTCUT_ID_9, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_9, value).apply()
    var isShortcut10: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_10, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_10, value).apply()
    var shortcutId10: String
        get() = prefs.getString(SHORTCUT_ID_10, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_10, value).apply()

    // Swipe left/right shortcut support
    var shortcutIdSwipeLeft: String
        get() = prefs.getString(SHORTCUT_ID_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_SWIPE_LEFT, value).apply()

    var isShortcutSwipeLeft: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_SWIPE_LEFT, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_SWIPE_LEFT, value).apply()

    var shortcutIdSwipeRight: String
        get() = prefs.getString(SHORTCUT_ID_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit().putString(SHORTCUT_ID_SWIPE_RIGHT, value).apply()

    var isShortcutSwipeRight: Boolean
        get() = prefs.getBoolean(IS_SHORTCUT_SWIPE_RIGHT, false)
        set(value) = prefs.edit().putBoolean(IS_SHORTCUT_SWIPE_RIGHT, value).apply()

    fun getAppName(location: Int): String {
        return when (location) {
            1 -> prefs.getString(APP_NAME_1, "").toString()
            2 -> prefs.getString(APP_NAME_2, "").toString()
            3 -> prefs.getString(APP_NAME_3, "").toString()
            4 -> prefs.getString(APP_NAME_4, "").toString()
            5 -> prefs.getString(APP_NAME_5, "").toString()
            6 -> prefs.getString(APP_NAME_6, "").toString()
            7 -> prefs.getString(APP_NAME_7, "").toString()
            8 -> prefs.getString(APP_NAME_8, "").toString()
            9 -> prefs.getString(APP_NAME_9, "").toString()
            10 -> prefs.getString(APP_NAME_10, "").toString()
            else -> ""
        }
    }

    fun getAppPackage(location: Int): String {
        return when (location) {
            1 -> prefs.getString(APP_PACKAGE_1, "").toString()
            2 -> prefs.getString(APP_PACKAGE_2, "").toString()
            3 -> prefs.getString(APP_PACKAGE_3, "").toString()
            4 -> prefs.getString(APP_PACKAGE_4, "").toString()
            5 -> prefs.getString(APP_PACKAGE_5, "").toString()
            6 -> prefs.getString(APP_PACKAGE_6, "").toString()
            7 -> prefs.getString(APP_PACKAGE_7, "").toString()
            8 -> prefs.getString(APP_PACKAGE_8, "").toString()
            9 -> prefs.getString(APP_PACKAGE_9, "").toString()
            10 -> prefs.getString(APP_PACKAGE_10, "").toString()
            else -> ""
        }
    }

    fun getAppActivityClassName(location: Int): String {
        return when (location) {
            1 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_1, "").toString()
            2 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_2, "").toString()
            3 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_3, "").toString()
            4 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_4, "").toString()
            5 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_5, "").toString()
            6 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_6, "").toString()
            7 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_7, "").toString()
            8 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_8, "").toString()
            9 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_9, "").toString()
            10 -> prefs.getString(APP_ACTIVITY_CLASS_NAME_10, "").toString()
            else -> ""
        }
    }

    fun getAppUser(location: Int): String {
        return when (location) {
            1 -> prefs.getString(APP_USER_1, "").toString()
            2 -> prefs.getString(APP_USER_2, "").toString()
            3 -> prefs.getString(APP_USER_3, "").toString()
            4 -> prefs.getString(APP_USER_4, "").toString()
            5 -> prefs.getString(APP_USER_5, "").toString()
            6 -> prefs.getString(APP_USER_6, "").toString()
            7 -> prefs.getString(APP_USER_7, "").toString()
            8 -> prefs.getString(APP_USER_8, "").toString()
            9 -> prefs.getString(APP_USER_9, "").toString()
            10 -> prefs.getString(APP_USER_10, "").toString()
            else -> ""
        }
    }

    fun getShortcutId(location: Int): String {
        return when (location) {
            1 -> shortcutId1
            2 -> shortcutId2
            3 -> shortcutId3
            4 -> shortcutId4
            5 -> shortcutId5
            6 -> shortcutId6
            7 -> shortcutId7
            8 -> shortcutId8
            9 -> shortcutId9
            10 -> shortcutId10
            else -> ""
        }
    }

    fun getIsShortcut(location: Int): Boolean {
        return when (location) {
            1 -> isShortcut1
            2 -> isShortcut2
            3 -> isShortcut3
            4 -> isShortcut4
            5 -> isShortcut5
            6 -> isShortcut6
            7 -> isShortcut7
            8 -> isShortcut8
            9 -> isShortcut9
            10 -> isShortcut10
            else -> false
        }
    }

    fun getAppRenameLabel(appPackage: String): String = prefs.getString(appPackage, "").toString()

    fun setAppRenameLabel(appPackage: String, renameLabel: String) = prefs.edit().putString(appPackage, renameLabel).apply()

    fun swapAppLocations(loc1: Int, loc2: Int) {
        if (loc1 == loc2) return

        val appName1 = getAppName(loc1)
        val appPackage1 = getAppPackage(loc1)
        val appClassName1 = getAppActivityClassName(loc1)
        val appUser1 = getAppUser(loc1)
        val isShortcut1 = getIsShortcut(loc1)
        val shortcutId1 = getShortcutId(loc1)

        val appName2 = getAppName(loc2)
        val appPackage2 = getAppPackage(loc2)
        val appClassName2 = getAppActivityClassName(loc2)
        val appUser2 = getAppUser(loc2)
        val isShortcut2 = getIsShortcut(loc2)
        val shortcutId2 = getShortcutId(loc2)

        setAppAtLocation(loc1, appName2, appPackage2, appClassName2, appUser2, isShortcut2, shortcutId2)
        setAppAtLocation(loc2, appName1, appPackage1, appClassName1, appUser1, isShortcut1, shortcutId1)
    }

    fun setAppAtLocation(
        location: Int,
        appName: String,
        appPackage: String,
        appClassName: String?,
        appUser: String,
        isShortcut: Boolean,
        shortcutId: String
    ) {
        when (location) {
            1 -> {
                this.appName1 = appName
                this.appPackage1 = appPackage
                this.appActivityClassName1 = appClassName
                this.appUser1 = appUser
                this.isShortcut1 = isShortcut
                this.shortcutId1 = shortcutId
            }
            2 -> {
                this.appName2 = appName
                this.appPackage2 = appPackage
                this.appActivityClassName2 = appClassName
                this.appUser2 = appUser
                this.isShortcut2 = isShortcut
                this.shortcutId2 = shortcutId
            }
            3 -> {
                this.appName3 = appName
                this.appPackage3 = appPackage
                this.appActivityClassName3 = appClassName
                this.appUser3 = appUser
                this.isShortcut3 = isShortcut
                this.shortcutId3 = shortcutId
            }
            4 -> {
                this.appName4 = appName
                this.appPackage4 = appPackage
                this.appActivityClassName4 = appClassName
                this.appUser4 = appUser
                this.isShortcut4 = isShortcut
                this.shortcutId4 = shortcutId
            }
            5 -> {
                this.appName5 = appName
                this.appPackage5 = appPackage
                this.appActivityClassName5 = appClassName
                this.appUser5 = appUser
                this.isShortcut5 = isShortcut
                this.shortcutId5 = shortcutId
            }
            6 -> {
                this.appName6 = appName
                this.appPackage6 = appPackage
                this.appActivityClassName6 = appClassName
                this.appUser6 = appUser
                this.isShortcut6 = isShortcut
                this.shortcutId6 = shortcutId
            }
            7 -> {
                this.appName7 = appName
                this.appPackage7 = appPackage
                this.appActivityClassName7 = appClassName
                this.appUser7 = appUser
                this.isShortcut7 = isShortcut
                this.shortcutId7 = shortcutId
            }
            8 -> {
                this.appName8 = appName
                this.appPackage8 = appPackage
                this.appActivityClassName8 = appClassName
                this.appUser8 = appUser
                this.isShortcut8 = isShortcut
                this.shortcutId8 = shortcutId
            }
            9 -> {
                this.appName9 = appName
                this.appPackage9 = appPackage
                this.appActivityClassName9 = appClassName
                this.appUser9 = appUser
                this.isShortcut9 = isShortcut
                this.shortcutId9 = shortcutId
            }
            10 -> {
                this.appName10 = appName
                this.appPackage10 = appPackage
                this.appActivityClassName10 = appClassName
                this.appUser10 = appUser
                this.isShortcut10 = isShortcut
                this.shortcutId10 = shortcutId
            }
        }
    }

    /** Independent clock section alignment (Gravity.START / CENTER_HORIZONTAL / END). */
    var clockAlignment: Int
        get() = prefs.getInt(CLOCK_ALIGNMENT, Gravity.START)
        set(value) {
            prefs.edit { putInt(CLOCK_ALIGNMENT, value) }
            emitHomescreenPrefs()
        }

    /** Independent clock section vertical alignment (Gravity.TOP / CENTER_VERTICAL / BOTTOM). */
    var clockVerticalAlignment: Int
        get() = prefs.getInt(CLOCK_VERTICAL_ALIGNMENT, Gravity.BOTTOM)
        set(value) {
            prefs.edit { putInt(CLOCK_VERTICAL_ALIGNMENT, value) }
            emitHomescreenPrefs()
        }

    /** Clock section weight (0.15–0.50). */
    var clockSectionWeight: Float
        get() = prefs.getFloat(CLOCK_SECTION_WEIGHT, 0.25f)
        set(value) {
            prefs.edit { putFloat(CLOCK_SECTION_WEIGHT, value) }
            emitHomescreenPrefs()
        }

    /** Enable/disable Private Space integration. */
    var privateSpaceEnabled: Boolean
        get() = prefs.getBoolean(PRIVATE_SPACE_ENABLED, true)
        set(value) = prefs.edit { putBoolean(PRIVATE_SPACE_ENABLED, value).apply() }

    /** Left swipe action: "notification_summary", "widgets", or "notes". */
    var leftSwipeAction: String
        get() = prefs.getString(LEFT_SWIPE_ACTION, SwipeAction.NOTIFICATION_SUMMARY).toString()
        set(value) = prefs.edit { putString(LEFT_SWIPE_ACTION, value).apply() }

    /** Right swipe action: "notification_summary", "widgets", or "notes". */
    var rightSwipeAction: String
        get() = prefs.getString(RIGHT_SWIPE_ACTION, SwipeAction.WIDGETS).toString()
        set(value) = prefs.edit { putString(RIGHT_SWIPE_ACTION, value).apply() }

    /** Set of pinned widget provider component names (flattenToString). */
    var pinnedWidgets: Set<String>
        get() = prefs.getStringSet("PINNED_WIDGETS", emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet("PINNED_WIDGETS", value).apply() }
        
    /** Set of widget resizing options encoded as "provider|spanX|spanY" */
    var widgetSpans: Set<String>
        get() = prefs.getStringSet("WIDGET_SPANS", emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet("WIDGET_SPANS", value).apply() }
        
    var maxHomeApps: Int
        get() = prefs.getInt(MAX_HOME_APPS, 10)
        set(value) = prefs.edit { putInt(MAX_HOME_APPS, value).apply() }

    var showAlphabetCategories: Boolean
        get() = prefs.getBoolean(SHOW_ALPHABET_CATEGORIES, true)
        set(value) = prefs.edit { putBoolean(SHOW_ALPHABET_CATEGORIES, value).apply() }

    /** Swipe action constants. */
    object SwipeAction {
        const val NOTIFICATION_SUMMARY = "notification_summary"
        const val WIDGETS = "widgets"
        const val NOTES = "notes"
        const val NOTIFICATIONS = "notifications"
        const val APP = "app"
        const val ACCESSIBILITY = "accessibility"
        const val NONE = "none"
    }

    var enableGestures: Boolean
        get() = prefs.getBoolean(ENABLE_GESTURES, true)
        set(value) {
            prefs.edit { putBoolean(ENABLE_GESTURES, value) }
            emitHomescreenPrefs()
        }

    var enableNotificationSummary: Boolean
        get() = prefs.getBoolean(ENABLE_NOTIFICATION_SUMMARY, true)
        set(value) = prefs.edit { putBoolean(ENABLE_NOTIFICATION_SUMMARY, value).apply() }

    var enableWidgets: Boolean
        get() = prefs.getBoolean(ENABLE_WIDGETS, true)
        set(value) = prefs.edit { putBoolean(ENABLE_WIDGETS, value).apply() }

    var enableNotes: Boolean
        get() = prefs.getBoolean(ENABLE_NOTES, true)
        set(value) = prefs.edit { putBoolean(ENABLE_NOTES, value).apply() }

    var leftSwipeAppPackage: String
        get() = prefs.getString(APP_PACKAGE_SWIPE_LEFT, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_SWIPE_LEFT, value).apply() }

    var rightSwipeAppPackage: String
        get() = prefs.getString(APP_PACKAGE_SWIPE_RIGHT, "").toString()
        set(value) = prefs.edit { putString(APP_PACKAGE_SWIPE_RIGHT, value).apply() }

    var appSpacingDp: Float
        get() = prefs.getFloat(APP_SPACING_DP, 16f)
        set(value) = prefs.edit { putFloat(APP_SPACING_DP, value).apply() }

}
