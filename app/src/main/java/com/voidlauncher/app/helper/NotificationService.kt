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
            
            // Group notifications
            val groupedMap = mutableMapOf<String, MutableList<StatusBarNotification>>()
            
            for (sbn in activeNotifications) {
                // Ignore ongoing/foreground service notifications to minimize clutter
                if (sbn.isOngoing || sbn.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) {
                    continue
                }
                
                val key = sbn.notification.group ?: sbn.packageName
                groupedMap.getOrPut(key) { mutableListOf() }.add(sbn)
            }
            
            val resultList = groupedMap.map { (key, list) ->
                // Sort within group by timestamp descending
                list.sortByDescending { it.postTime }
                
                val latestSbn = list.first()
                val pkg = latestSbn.packageName
                @Suppress("DEPRECATION")
                val highestImportance = list.maxOfOrNull { 
                    it.notification.priority 
                } ?: 0

                NotificationGroup(
                    groupKey = key,
                    packageName = pkg,
                    latestTimestamp = latestSbn.postTime,
                    childCount = list.size,
                    highestImportance = highestImportance,
                    notifications = list
                )
            }.sortedByDescending { it.latestTimestamp }

            notificationsLiveData.postValue(resultList)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notifications: ${e.message}")
        }
    }
}
