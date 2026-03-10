package com.launcher.void

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.launcher.void.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = Prefs(application.applicationContext)

    private val _isStatusBarVisible = MutableStateFlow(prefs.showStatusBar)
    val isStatusBarVisible: StateFlow<Boolean> = _isStatusBarVisible.asStateFlow()

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "STATUS_BAR") {
            _isStatusBarVisible.value = prefs.showStatusBar
        }
    }

    init {
        prefs.registerPreferenceChangeListener(prefChangeListener)
    }

    fun setStatusBarVisibility(isVisible: Boolean) {
        prefs.showStatusBar = isVisible
        _isStatusBarVisible.value = isVisible
    }

    fun toggleStatusBarVisibility() {
        setStatusBarVisibility(!_isStatusBarVisible.value)
    }

    override fun onCleared() {
        prefs.unregisterPreferenceChangeListener(prefChangeListener)
        super.onCleared()
    }
}
