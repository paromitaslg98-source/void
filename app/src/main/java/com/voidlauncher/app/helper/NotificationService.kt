package com.voidlauncher.app.helper

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.voidlauncher.app.data.NotificationGroup

class NotificationService : NotificationListenerService() {

    companion object {
        val notificationsLiveData: MutableLiveData<List<NotificationGroup>> = MutableLiveData(emptyList())
        const val TAG = "NotificationService"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        updateNotifications()
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
            val activeNotifications = activeNotifications ?: return

            // Group by packageName so all notifications from the same app are merged
            val groupedMap = mutableMapOf<String, MutableList<StatusBarNotification>>()

            for (sbn in activeNotifications) {
                // Ignore ongoing/foreground service notifications
                if (sbn.isOngoing || 
                    (sbn.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) ||
                    (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)) {
                    continue
                }
                groupedMap.getOrPut(sbn.packageName) { mutableListOf() }.add(sbn)
            }

            val resultList = groupedMap.map { (pkg, list) ->
                // Sort within group by timestamp descending
                list.sortByDescending { it.postTime }

                val latestSbn = list.first()
                @Suppress("DEPRECATION")
                val highestImportance = list.maxOfOrNull {
                    it.notification.priority
                } ?: 0

                NotificationGroup(
                    groupKey = pkg,
                    packageName = pkg,
                    latestTimestamp = latestSbn.postTime,
                    childCount = list.size,
                    highestImportance = highestImportance,
                    notifications = list
                )
            }
            // Sort: notification count descending (primary), latest timestamp descending (secondary)
            .sortedWith(compareByDescending<NotificationGroup> { it.childCount }
                .thenByDescending { it.latestTimestamp })

            notificationsLiveData.postValue(resultList)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notifications: ${e.message}")
        }
    }
}
