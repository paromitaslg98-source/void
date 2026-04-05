package com.launcher.projectvoid.ui.screen

import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.view.Gravity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.launcher.projectvoid.HomeApp
import com.launcher.projectvoid.MainUiState
import com.launcher.projectvoid.R
import com.launcher.projectvoid.data.AppModel
import com.launcher.projectvoid.data.Prefs
import com.launcher.projectvoid.data.Prefs.SwipeAction
import com.launcher.projectvoid.helper.getAppsList
import kotlinx.coroutines.launch
import kotlin.math.abs

private fun gravityToAlignment(gravity: Int): Alignment.Horizontal = when (gravity) {
    Gravity.CENTER, Gravity.CENTER_HORIZONTAL -> Alignment.CenterHorizontally
    Gravity.END, Gravity.RIGHT -> Alignment.End
    else -> Alignment.Start
}

fun gravityToVerticalArrangement(gravity: Int): Arrangement.Vertical = when (gravity) {
    Gravity.TOP -> Arrangement.Top
    Gravity.BOTTOM -> Arrangement.Bottom
    else -> Arrangement.Center
}

private fun gravityToTextAlign(gravity: Int): TextAlign = when (gravity) {
    Gravity.CENTER, Gravity.CENTER_HORIZONTAL -> TextAlign.Center
    Gravity.END, Gravity.RIGHT -> TextAlign.End
    else -> TextAlign.Start
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: MainUiState,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenNotificationSummary: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenNotes: () -> Unit,
    onAppClick: (HomeApp) -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onHomeAppsChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val clockAlign = gravityToAlignment(state.clockHorizontalAlignment)
    val appAlign = gravityToAlignment(state.appHorizontalAlignment)
    val clockVertical = gravityToVerticalArrangement(state.clockVerticalAlignment)
    val appVertical = gravityToVerticalArrangement(state.appVerticalAlignment)
    
    val clockTextAlign = gravityToTextAlign(state.clockHorizontalAlignment)
    val appTextAlign = gravityToTextAlign(state.appHorizontalAlignment)

    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val swipeThreshold = 120f
    var showAppPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, bottom = 24.dp)
            .pointerInput(state.leftSwipeAction, state.rightSwipeAction, state.enableGestures) {
                detectDragGestures(
                    onDragStart = { dragOffset = Offset.Zero },
                    onDragEnd = {
                        val absX = abs(dragOffset.x)
                        val absY = abs(dragOffset.y)
                        if (absX > swipeThreshold || absY > swipeThreshold) {
                            if (absX > absY) {
                                if (state.enableGestures) {
                                    // Android standard: Swipe your finger RIGHT to reveal the screen on the LEFT.
                                    if (dragOffset.x > 0) {
                                        dispatchSwipeAction("left", state.leftSwipeAction, context,
                                            onOpenNotificationSummary, onOpenWidgets, onOpenNotes, onOpenNotifications)
                                    } else {
                                        dispatchSwipeAction("right", state.rightSwipeAction, context,
                                            onOpenNotificationSummary, onOpenWidgets, onOpenNotes, onOpenNotifications)
                                    }
                                }
                            } else {
                                if (dragOffset.y > 0) onOpenNotifications()
                                else onOpenApps()
                            }
                        }
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = { dragOffset = Offset.Zero },
                    onDrag = { change, amount ->
                        change.consume()
                        dragOffset += amount
                    }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Clock section: 1/4 of screen ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(state.clockSectionWeight.coerceIn(0.15f, 0.50f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = clockAlign,
            ) {
                // Separate, weighted clock content block so vertical alignment controls the
                // entire clock/date/screen-time group as one unit.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = clockAlign,
                    verticalArrangement = clockVertical
                ) {
                    // Keep each clock-related item in its own block so spacing remains predictable
                    // and future additions can slot in without crowding neighboring content.
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = clockAlign,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.showClock) {
                            Text(
                                text = state.currentTime,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = MaterialTheme.typography.displayLarge.fontSize * state.homeTextSizeScale
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.87f),
                                textAlign = clockTextAlign,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            onClockClick()
                                        }
                                    }
                                    .padding(vertical = 2.dp)
                            )
                        }

                        if (state.showDate) {
                            Text(
                                text = state.currentDate,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                                textAlign = clockTextAlign,
                                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing * 1.5f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                                            val intent = Intent(Intent.ACTION_VIEW).setData(builder.build())
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            onDateClick()
                                        }
                                    }
                                    .padding(vertical = 2.dp)
                            )
                        }

                        if (state.showScreenTime && state.screenTime.isNotBlank()) {
                            Text(
                                text = state.screenTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                textAlign = clockTextAlign,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = try {
                                            // 1. Try Google Wellbeing Top Level Settings (Direct Activity - settings)
                                            Intent().apply {
                                                setClassName(com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_PACKAGE_NAME, "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        } catch (e: Exception) {
                                            null
                                        }

                                        val intent2 = try {
                                            // 2. Try Google Wellbeing Top Level Settings (Direct Activity - home)
                                            Intent().apply {
                                                setClassName(com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_PACKAGE_NAME, "com.google.android.apps.wellbeing.home.TopLevelSettingsActivity")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        } catch (e: Exception) {
                                            null
                                        }

                                        val intent3 = Intent("com.google.android.apps.wellbeing.VIEW_APP_USAGE").apply {
                                            setPackage(com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_PACKAGE_NAME)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }

                                        val intent4 = Intent("android.settings.DIGITAL_WELLBEING_SETTINGS").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }

                                        val intent5 = Intent().apply {
                                            setClassName(
                                                com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                                                com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY
                                            )
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }

                                        val intent6 = context.packageManager.getLaunchIntentForPackage(com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_PACKAGE_NAME)?.apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }

                                        val intent7 = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }

                                        val list = listOfNotNull(intent, intent2, intent3, intent4, intent5, intent6, intent7)
                                        for (target in list) {
                                            try {
                                                context.startActivity(target)
                                                break
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // ── Apps section: 3/4 of screen ──
            // Long press anywhere in this section → open app picker
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f - state.clockSectionWeight.coerceIn(0.15f, 0.50f))
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = { showAppPicker = true }
                    )
                    .padding(horizontal = 20.dp),
                horizontalAlignment = appAlign
            ) {
                // Keep apps in their own weighted content block so vertical alignment actually
                // controls where the app group appears (Top / Center / Bottom).
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp, bottom = 8.dp),
                    horizontalAlignment = appAlign,
                    verticalArrangement = appVertical
                ) {
                    state.homeApps.forEach { app ->
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * state.homeTextSizeScale
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.87f),
                            textAlign = appTextAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(app) }
                                .padding(vertical = 14.dp)  // 14dp top + 14dp bot + ~20sp text ≈ 48dp touch target
                        )
                    }
                }

                // Keep the bottom area isolated so we can add more footer items later
                // (network status, next alarm, etc.) without touching app-list alignment logic.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = appAlign
                ) {
                    Text(
                        text = "${state.batteryLevel}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        textAlign = appTextAlign,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // ── App picker bottom sheet ──
    if (showAppPicker) {
        HomeAppPickerSheet(
            currentApps = state.homeApps,
            maxApps = state.homeAppsCount.coerceIn(1, 15),
            onDismiss = {
                showAppPicker = false
            },
            onHomeAppsChanged = onHomeAppsChanged
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAppPickerSheet(
    currentApps: List<HomeApp>,
    maxApps: Int,
    onDismiss: () -> Unit,
    onHomeAppsChanged: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allApps = remember { mutableStateListOf<AppModel>() }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            allApps.addAll(getAppsList(context, prefs))
        }
    }

    val currentPackages = remember(currentApps) {
        currentApps.map { it.packageName }.toSet()
    }
    val filtered = remember(search, allApps.toList()) {
        if (search.isBlank()) allApps.toList()
        else allApps.filter { it.appLabel.contains(search, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Home Screen Apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Current apps with remove option
            if (currentApps.isNotEmpty()) {
                Text(
                    "CURRENT (${currentApps.size}/$maxApps)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                currentApps.forEachIndexed { index, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (index > 0) {
                                androidx.compose.material3.IconButton(onClick = {
                                    val other = currentApps[index - 1]
                                    prefs.setAppAtLocation(other.position, app.label, app.packageName, app.activityClassName, app.userString, app.isShortcut, app.shortcutId)
                                    prefs.setAppAtLocation(app.position, other.label, other.packageName, other.activityClassName, other.userString, other.isShortcut, other.shortcutId)
                                    onHomeAppsChanged()
                                }) {
                                    androidx.compose.material3.Text("↑", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (index < currentApps.size - 1) {
                                androidx.compose.material3.IconButton(onClick = {
                                    val other = currentApps[index + 1]
                                    prefs.setAppAtLocation(other.position, app.label, app.packageName, app.activityClassName, app.userString, app.isShortcut, app.shortcutId)
                                    prefs.setAppAtLocation(app.position, other.label, other.packageName, other.activityClassName, other.userString, other.isShortcut, other.shortcutId)
                                    onHomeAppsChanged()
                                }) {
                                    androidx.compose.material3.Text("↓", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            androidx.compose.material3.IconButton(onClick = {
                                prefs.setAppAtLocation(app.position, "", "", null, "", false, "")
                                onHomeAppsChanged()
                            }) {
                                Icon(
                                    Icons.Outlined.Remove,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Search
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search apps to add…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            // Available apps
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .padding(top = 8.dp)
            ) {
                items(filtered, key = { "${it.appPackage}_${it.user}" }) { app ->
                    val isOnHome = currentPackages.contains(app.appPackage)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isOnHome && currentApps.size < maxApps) {
                                    val nextPos = (1..maxApps).firstOrNull { pos ->
                                        prefs.getAppName(pos).isBlank()
                                    } ?: return@clickable
                                    val a = app as? AppModel.App ?: return@clickable
                                    prefs.setAppAtLocation(
                                        nextPos, a.appLabel, a.appPackage,
                                        a.activityClassName, a.user.toString(),
                                        false, ""
                                    )
                                    onHomeAppsChanged()
                                }
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.appLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isOnHome) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isOnHome) {
                            Icon(Icons.Outlined.Check, "On home",
                                tint = MaterialTheme.colorScheme.primary)
                        } else if (currentApps.size < maxApps) {
                            Icon(Icons.Outlined.Add, "Add",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun dispatchSwipeAction(
    direction: String,
    action: String,
    context: android.content.Context,
    onSummary: () -> Unit,
    onWidgets: () -> Unit,
    onNotes: () -> Unit,
    onNotifications: () -> Unit
) {
    when (action) {
        com.launcher.projectvoid.data.Prefs.SwipeAction.NOTIFICATION_SUMMARY -> onSummary()
        com.launcher.projectvoid.data.Prefs.SwipeAction.WIDGETS -> onWidgets()
        com.launcher.projectvoid.data.Prefs.SwipeAction.NOTES -> onNotes()
        com.launcher.projectvoid.data.Prefs.SwipeAction.NOTIFICATIONS -> onNotifications()
        com.launcher.projectvoid.data.Prefs.SwipeAction.APP -> {
            val prefs = com.launcher.projectvoid.data.Prefs(context)
            val pkg = if (direction == "left") prefs.leftSwipeAppPackage else prefs.rightSwipeAppPackage
            if (pkg.isNotEmpty()) {
                try {
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
