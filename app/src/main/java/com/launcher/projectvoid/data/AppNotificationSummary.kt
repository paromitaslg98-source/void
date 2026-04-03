package com.launcher.projectvoid.data

import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification

data class AppNotificationSummary(
    val packageName: String,
    val appLabel: String,
    val notifications: List<StatusBarNotification>,
    val aiSummary: String? = null,
    val latestTimestamp: Long,
    val isLoading: Boolean = true
)
