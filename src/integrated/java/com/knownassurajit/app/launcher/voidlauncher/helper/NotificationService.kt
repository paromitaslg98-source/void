package com.knownassurajit.app.launcher.voidlauncher.helper

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.knownassurajit.app.launcher.voidlauncher.data.NotificationGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationService : NotificationListenerService() {

    companion object {
        val notificationsLiveData: MutableLiveData<List<NotificationGroup>> = MutableLiveData(emptyList())

        private val _notificationsState = MutableStateFlow<List<NotificationGroup>>(emptyList())
        val notificationsState: StateFlow<List<NotificationGroup>> = _notificationsState.asStateFlow()

        const val TAG = "NotificationService"

        private var instance: NotificationService? = null

        /** Dismiss a single notification by its key. */
        fun dismissNotification(key: String) {
            try {
                instance?.cancelNotification(key)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dismiss notification: ${e.message}")
            }
        }

        /** Dismiss all notifications for a given package. */
        fun dismissNotificationsForPackage(packageName: String) {
            try {
                instance?.let { service ->
                    service.activeNotifications
                        ?.filter { it.packageName == packageName }
                        ?.forEach { sbn -> service.cancelNotification(sbn.key) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dismiss package notifications: ${e.message}")
            }
        }

        /** Dismiss all active notifications. */
        fun dismissAll() {
            try {
                instance?.cancelAllNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dismiss all notifications: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Fail closed: only the system server (UID 1000) is allowed to bind a
        // NotificationListenerService. Reject any other caller defensively, in case
        // a future OEM build or test harness attempts to bind without the permission check.
        val uid = Binder.getCallingUid()
        if (uid != Process.SYSTEM_UID && uid != Process.myUid()) {
            Log.w(TAG, "Rejected onBind from unexpected uid=$uid")
            return null
        }
        return super.onBind(intent)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d(TAG, "Notification listener connected")
        updateNotifications()
    }

    override fun onListenerDisconnected() {
        instance = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    private fun updateNotifications() {
        try {
            val active = activeNotifications ?: return
            val groupedMap = mutableMapOf<String, MutableList<StatusBarNotification>>()

            for (sbn in active) {
                if (sbn.isOngoing ||
                    (sbn.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) ||
                    (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
                ) continue
                groupedMap.getOrPut(sbn.packageName) { mutableListOf() }.add(sbn)
            }

            val resultList = groupedMap.map { (pkg, list) ->
                list.sortByDescending { it.postTime }
                val latestSbn = list.first()

                // Keep highestImportance as "most interruptive notification in the group".
                // On Android O+ this is channel importance (system-truth); when unavailable we
                // gracefully fall back to legacy notification priority semantics.
                val highestImportance = list.maxOfOrNull { resolveNotificationImportance(it) } ?: 0

                val appLabel = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(pkg, 0)
                    ).toString()
                } catch (e: Exception) { pkg }

                NotificationGroup(
                    groupKey = pkg,
                    packageName = pkg,
                    appLabel = appLabel,
                    latestTimestamp = latestSbn.postTime,
                    childCount = list.size,
                    highestImportance = highestImportance,
                    notifications = list
                )
            }.sortedByDescending { it.latestTimestamp }

            notificationsLiveData.postValue(resultList)
            _notificationsState.value = resultList
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notifications: ${e.message}")
        }
    }

    private fun resolveNotificationImportance(sbn: StatusBarNotification): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = sbn.notification.channelId
            if (!channelId.isNullOrBlank()) {
                // NotificationManager is queried first so we reflect the *actual* runtime
                // channel configuration (including user changes), not just the posted payload.
                val notificationManager = getSystemService(NotificationManager::class.java)
                val channel = notificationManager?.getNotificationChannel(channelId)
                if (channel != null) return channel.importance
            }
        }

        // Older devices (or missing channel metadata) still need a stable score. We map
        // legacy priority levels onto NotificationManager IMPORTANCE constants so
        // NotificationGroup.highestImportance stays on one consistent scale.
        @Suppress("DEPRECATION")
        return legacyPriorityToImportance(sbn.notification.priority)
    }

    @Suppress("DEPRECATION")
    private fun legacyPriorityToImportance(priority: Int): Int = when (priority) {
        Notification.PRIORITY_MAX -> NotificationManager.IMPORTANCE_HIGH
        Notification.PRIORITY_HIGH -> NotificationManager.IMPORTANCE_DEFAULT
        Notification.PRIORITY_LOW -> NotificationManager.IMPORTANCE_LOW
        Notification.PRIORITY_MIN -> NotificationManager.IMPORTANCE_MIN
        else -> NotificationManager.IMPORTANCE_DEFAULT
    }
}
