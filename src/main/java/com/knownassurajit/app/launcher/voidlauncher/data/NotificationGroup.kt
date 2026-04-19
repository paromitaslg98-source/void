package com.knownassurajit.app.launcher.voidlauncher.data

import android.service.notification.StatusBarNotification

data class NotificationGroup(
    val groupKey: String,
    val packageName: String,
    val appLabel: String = "",
    val latestTimestamp: Long,
    val childCount: Int,
    // Highest effective notification importance in this group.
    // Uses channel importance when available; otherwise a legacy-priority fallback.
    val highestImportance: Int,
    val notifications: List<StatusBarNotification>
)
