<<<<<<< HEAD:src/main/java/com/knownassurajit/app/launcher/voidlauncher/helper/NotificationService.kt
package com.knownassurajit.app.launcher.voidlauncher.helper
=======
package com.voidlauncher.app.helper
>>>>>>> 7c83749 (rebasing develop from stage (#44)):app/src/main/java/com/launcher/projectvoid/helper/NotificationService.kt

import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.MutableLiveData
<<<<<<< HEAD:src/main/java/com/knownassurajit/app/launcher/voidlauncher/helper/NotificationService.kt
import com.knownassurajit.app.launcher.voidlauncher.data.NotificationGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
=======
import com.voidlauncher.app.data.NotificationGroup
>>>>>>> 7c83749 (rebasing develop from stage (#44)):app/src/main/java/com/launcher/projectvoid/helper/NotificationService.kt

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
<<<<<<< HEAD:src/main/java/com/knownassurajit/app/launcher/voidlauncher/helper/NotificationService.kt

                // Keep highestImportance as "most interruptive notification in the group".
                // On Android O+ this is channel importance (system-truth); when unavailable we
                // gracefully fall back to legacy notification priority semantics.
                val highestImportance = list.maxOfOrNull { resolveNotificationImportance(it) } ?: 0

                val appLabel = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(pkg, 0)
                    ).toString()
                } catch (e: Exception) { pkg }
=======
                @Suppress("DEPRECATION")
                val highestImportance = list.maxOfOrNull {
                    it.notification.priority
                } ?: 0
>>>>>>> 7c83749 (rebasing develop from stage (#44)):app/src/main/java/com/launcher/projectvoid/helper/NotificationService.kt

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
