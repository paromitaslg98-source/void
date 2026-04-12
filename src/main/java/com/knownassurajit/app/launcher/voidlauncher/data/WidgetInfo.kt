package com.knownassurajit.app.launcher.voidlauncher.data

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable

data class WidgetInfo(
    val provider: AppWidgetProviderInfo,
    val label: String,
    val previewImage: Drawable?,
    val appName: String,
    val isPinned: Boolean = false
)
