package com.launcher.projectvoid.data

import android.service.notification.StatusBarNotification

data class NotificationGroup(
    val groupKey: String,
    val packageName: String,
    val appLabel: String = "",
    val latestTimestamp: Long,
    val childCount: Int,
    val highestImportance: Int,
    val notifications: List<StatusBarNotification>
)
