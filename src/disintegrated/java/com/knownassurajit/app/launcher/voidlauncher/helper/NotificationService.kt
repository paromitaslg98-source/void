package com.knownassurajit.app.launcher.voidlauncher.helper

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import androidx.lifecycle.MutableLiveData
import com.knownassurajit.app.launcher.voidlauncher.data.NotificationGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play-safe stub. Class shape mirrors the integrated implementation so any
 * shared code that references [NotificationService] (including the companion
 * surface) compiles unchanged. The service is never declared in the
 * disintegrated manifest, so the system will never bind it.
 */
class NotificationService : NotificationListenerService() {

    companion object {
        val notificationsLiveData: MutableLiveData<List<NotificationGroup>> =
            MutableLiveData(emptyList())

        private val _notificationsState = MutableStateFlow<List<NotificationGroup>>(emptyList())
        val notificationsState: StateFlow<List<NotificationGroup>> =
            _notificationsState.asStateFlow()

        const val TAG = "NotificationService"

        fun dismissNotification(key: String) = Unit
        fun dismissNotificationsForPackage(packageName: String) = Unit
        fun dismissAll() = Unit
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
