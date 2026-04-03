package com.launcher.projectvoid.helper

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.launcher.projectvoid.data.NotificationGroup
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

                @Suppress("DEPRECATION")
                val highestImportance = list.maxOfOrNull { it.notification.priority } ?: 0

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
}
