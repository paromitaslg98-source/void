package com.knownassurajit.app.launcher.voidlauncher.helper

import androidx.lifecycle.MutableLiveData
import com.knownassurajit.app.launcher.voidlauncher.data.NotificationGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationService {
    val notificationsLiveData: MutableLiveData<List<NotificationGroup>> = MutableLiveData(emptyList())

    private val _notificationsState = MutableStateFlow<List<NotificationGroup>>(emptyList())
    val notificationsState: StateFlow<List<NotificationGroup>> = _notificationsState.asStateFlow()

    const val TAG = "NotificationService"

    fun dismissNotification(key: String) {}
    fun dismissNotificationsForPackage(packageName: String) {}
    fun dismissAll() {}
}
