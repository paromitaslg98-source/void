package com.launcher.projectvoid.ui.screen

import android.view.Gravity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.launcher.projectvoid.R
import com.launcher.projectvoid.data.Prefs
import com.launcher.projectvoid.data.Prefs.SwipeAction

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var maxHomeApps by remember { mutableIntStateOf(prefs.maxHomeApps) }
    var showClock by remember { mutableStateOf(prefs.showClockWidget) }
    var showDate by remember { mutableStateOf(prefs.showDateWidget) }
    var clockHorizontalAlignment by remember { mutableIntStateOf(prefs.clockAlignment) }
    var clockVerticalAlignment by remember { mutableIntStateOf(prefs.clockVerticalAlignment) }
    var appHorizontalAlignment by remember { mutableIntStateOf(prefs.homeAlignment) }
    var appVerticalAlignment by remember { mutableIntStateOf(prefs.homeVerticalAlignment) }
    var showScreenTime by remember { mutableStateOf(prefs.showScreenTimeWidget) }
    var autoShowKeyboard by remember { mutableStateOf(prefs.autoShowKeyboard) }
    var showAlphabetCategories by remember { mutableStateOf(prefs.showAlphabetCategories) }
    var showStatusBar by remember { mutableStateOf(prefs.showStatusBar) }
    var homeTextSize by remember { mutableFloatStateOf(prefs.homeTextSizeScale) }
    var drawerTextSize by remember { mutableFloatStateOf(prefs.appDrawerTextSizeScale) }
    var leftSwipeAction by remember { mutableStateOf(prefs.leftSwipeAction) }
    var rightSwipeAction by remember { mutableStateOf(prefs.rightSwipeAction) }
    var swipeDownAction by remember { mutableIntStateOf(prefs.swipeDownAction) }
    var clockSectionWeight by remember { mutableFloatStateOf(prefs.clockSectionWeight) }
    var privateSpaceEnabled by remember { mutableStateOf(prefs.privateSpaceEnabled) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAppPicker by remember { mutableStateOf<String?>(null) }
    var showDeveloperInfo by remember { mutableStateOf(false) }

    if (showDeveloperInfo) {
        AlertDialog(
            onDismissRequest = { showDeveloperInfo = false },
            title = { Text("Developer Credits") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Made with 💜 by Surajit Das.", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "GitHub: https://github.com/knownassurajit/void",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/knownassurajit/void")))
                            } catch (_: Exception) {}
                        }
                    )
                    Text(
                        "LinkedIn: https://www.linkedin.com/in/knownassurajit/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.linkedin.com/in/knownassurajit/")))
                            } catch (_: Exception) {}
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeveloperInfo = false }) {
                    Text("Close")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // ── Header Card ──
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { showDeveloperInfo = true }) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = "Credits",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                        } catch (_: Exception) {}
                    }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "System Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.change_default_launcher),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── HOME SCREEN Section ──
        SettingsSection(title = stringResource(R.string.home_screen)) {
            // App limit configurator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Max apps on Home Screen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (maxHomeApps > 1) { maxHomeApps--; prefs.maxHomeApps = maxHomeApps } }) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = "$maxHomeApps", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = { if (maxHomeApps < 15) { maxHomeApps++; prefs.maxHomeApps = maxHomeApps } }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            SettingsToggle(label = "Show clock", checked = showClock) {
                showClock = it; prefs.showClockWidget = it
            }
            SettingsToggle(label = stringResource(R.string.show_date_time), checked = showDate) {
                showDate = it; prefs.showDateWidget = it
            }
            SettingsAlignmentSelector(
                label = "Clock Horizontal Alignment",
                currentGravity = clockHorizontalAlignment,
                options = listOf(Gravity.START to "Left", Gravity.CENTER_HORIZONTAL to "Center", Gravity.END to "Right"),
                onChanged = { clockHorizontalAlignment = it; prefs.clockAlignment = it }
            )
            SettingsAlignmentSelector(
                label = "Clock Vertical Alignment",
                currentGravity = clockVerticalAlignment,
                options = listOf(Gravity.TOP to "Top", Gravity.CENTER_VERTICAL to "Center", Gravity.BOTTOM to "Bottom"),
                onChanged = { clockVerticalAlignment = it; prefs.clockVerticalAlignment = it }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SettingsAlignmentSelector(
                label = "Apps Horizontal Alignment",
                currentGravity = appHorizontalAlignment,
                options = listOf(Gravity.START to "Left", Gravity.CENTER_HORIZONTAL to "Center", Gravity.END to "Right"),
                onChanged = { appHorizontalAlignment = it; prefs.homeAlignment = it }
            )
            SettingsAlignmentSelector(
                label = "Apps Vertical Alignment",
                currentGravity = appVerticalAlignment,
                options = listOf(Gravity.TOP to "Top", Gravity.CENTER_VERTICAL to "Center", Gravity.BOTTOM to "Bottom"),
                onChanged = { appVerticalAlignment = it; prefs.homeVerticalAlignment = it }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SettingsToggle(
                label = stringResource(R.string.screen_time), 
                checked = showScreenTime,
                infoTitle = "Screen Time Permissions",
                infoText = "This feature requires 'Usage Data Access' (PACKAGE_USAGE_STATS) to accurately calculate and display foreground activity time for apps on the Home Screen. If not granted, you will be redirected to Android's Usage Access control panel."
            ) {
                showScreenTime = it; prefs.showScreenTimeWidget = it
                if (it) {
                    val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                    val mode = appOps.checkOpNoThrow(
                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        context.packageName
                    )
                    if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            scope.launch { snackbarHostState.showSnackbar("Redirected to Usage Access settings") }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            scope.launch { snackbarHostState.showSnackbar("Could not open settings automatically") }
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("Screen Time enabled") }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SettingsSlider(
                label = "Clock Section Size",
                value = clockSectionWeight,
                range = 0.15f..0.50f
            ) { clockSectionWeight = it; prefs.clockSectionWeight = it }
        }

        // ── APP LIBRARY Section ──
        SettingsSection(title = "App Library") {
            SettingsToggle(label = "Show Alphabet Categories", checked = showAlphabetCategories) {
                showAlphabetCategories = it; prefs.showAlphabetCategories = it
            }
            SettingsToggle(
                label = "Enable Private Space", 
                checked = privateSpaceEnabled,
                infoTitle = "Private Space Profiles",
                infoText = "Requires OS support for Hidden Profiles (access to Android 15's private spaces). We use LauncherApps.getProfiles to bypass blocks and securely fetch unlocked private sandbox apps directly into your drawer."
            ) {
                privateSpaceEnabled = it; prefs.privateSpaceEnabled = it
                scope.launch { snackbarHostState.showSnackbar(if (it) "Private Space Enabled" else "Private Space Disabled") }
            }
        }

        // ── APPEARANCE Section ──
        SettingsSection(title = stringResource(R.string.appearance)) {
            SettingsToggle(label = stringResource(R.string.auto_show_keyboard), checked = autoShowKeyboard) {
                autoShowKeyboard = it; prefs.autoShowKeyboard = it
            }
            SettingsToggle(label = stringResource(R.string.notification_bar), checked = showStatusBar) {
                showStatusBar = it; prefs.showStatusBar = it
            }
            SettingsSlider(
                label = stringResource(R.string.home_text_size),
                value = homeTextSize,
                range = 0.5f..2.0f
            ) { homeTextSize = it; prefs.homeTextSizeScale = it }
            SettingsSlider(
                label = stringResource(R.string.app_drawer_text_size),
                value = drawerTextSize,
                range = 0.5f..2.0f
            ) { drawerTextSize = it; prefs.appDrawerTextSizeScale = it }
        }

        // ── GESTURES Section ──
        var enableGestures by remember { mutableStateOf(prefs.enableGestures) }
        var enableSummary by remember { mutableStateOf(prefs.enableNotificationSummary) }
        var enableWidgets by remember { mutableStateOf(prefs.enableWidgets) }
        var enableNotes by remember { mutableStateOf(prefs.enableNotes) }

        SettingsSection(title = stringResource(R.string.gestures)) {
            SettingsToggle(label = "Enable Gestures", checked = enableGestures) {
                enableGestures = it; prefs.enableGestures = it
            }
            if (enableGestures) {
                SettingsToggle(label = "Enable Notification Summary", checked = enableSummary) {
                    enableSummary = it; prefs.enableNotificationSummary = it
                }
                SettingsToggle(label = "Enable Widgets Screen", checked = enableWidgets) {
                    enableWidgets = it; prefs.enableWidgets = it
                }
                SettingsToggle(label = "Enable Notes Screen", checked = enableNotes) {
                    enableNotes = it; prefs.enableNotes = it
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SwipeActionSelector(
                    label = stringResource(R.string.left_swipe_action),
                    currentAction = leftSwipeAction,
                    excludeAction = rightSwipeAction,
                    enableSummary = enableSummary,
                    enableWidgets = enableWidgets,
                    enableNotes = enableNotes
                ) {
                    leftSwipeAction = it; prefs.leftSwipeAction = it
                    if (it == SwipeAction.APP) showAppPicker = "left"
                }
                SwipeActionSelector(
                    label = stringResource(R.string.right_swipe_action),
                    currentAction = rightSwipeAction,
                    excludeAction = leftSwipeAction,
                    enableSummary = enableSummary,
                    enableWidgets = enableWidgets,
                    enableNotes = enableNotes
                ) {
                    rightSwipeAction = it; prefs.rightSwipeAction = it
                    if (it == SwipeAction.APP) showAppPicker = "right"
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        )
        if (showAppPicker != null) {
            SettingsAppPickerSheet(
                onDismiss = { showAppPicker = null },
                onAppSelected = { pkg ->
                    if (showAppPicker == "left") prefs.leftSwipeAppPackage = pkg
                    if (showAppPicker == "right") prefs.rightSwipeAppPackage = pkg
                    showAppPicker = null
                }
            )
        }
    }
}

// ── Reusable settings components ──

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                fontWeight = FontWeight.Medium,
                letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
            content()
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),  // 48dp touch target
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f))
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, infoTitle: String? = null, infoText: String? = null, onChanged: (Boolean) -> Unit) {
    var showInfo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),  // 48dp touch target with switch height
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f))
            if (infoTitle != null && infoText != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showInfo = true }) {
                    Icon(Icons.Outlined.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }

    if (showInfo && infoTitle != null && infoText != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(infoTitle) },
            text = { Text(infoText) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SettingsAlignmentSelector(label: String, currentGravity: Int, options: List<Pair<Int, String>>, onChanged: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 6.dp))
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (optionGravity, optionLabel) ->
                val isSelected = currentGravity == optionGravity
                Card(
                    modifier = Modifier.weight(1f).clickable { onChanged(optionGravity) }, // Selectable segments
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    border = if (isSelected) null else CardDefaults.outlinedCardBorder()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = optionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChanged: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
            Text(text = String.format("%.1f", value), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onChanged,
            valueRange = range,
            steps = 5,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SwipeActionSelector(
    label: String,
    currentAction: String,
    excludeAction: String,
    enableSummary: Boolean,
    enableWidgets: Boolean,
    enableNotes: Boolean,
    onChanged: (String) -> Unit
) {
    val allActions = mutableListOf<Pair<String, String>>()
    if (enableSummary) allActions.add(SwipeAction.NOTIFICATION_SUMMARY to "Notification Summary")
    if (enableWidgets) allActions.add(SwipeAction.WIDGETS to "Widgets")
    if (enableNotes) allActions.add(SwipeAction.NOTES to "Notes")
    allActions.add(SwipeAction.NOTIFICATIONS to "System Dropdown")
    allActions.add(SwipeAction.APP to "Open App")
    allActions.add(SwipeAction.ACCESSIBILITY to "Accessibility Action")
    allActions.add(SwipeAction.NONE to "None")

    // Filter out the action already selected for the other gesture, except NONE/NOTIFICATIONS/APP
    val available = allActions.filter {
        it.first != excludeAction || it.first == SwipeAction.NONE || it.first == SwipeAction.NOTIFICATIONS || it.first == SwipeAction.APP || it.first == SwipeAction.ACCESSIBILITY
    }
    val displayName = allActions.firstOrNull { it.first == currentAction }?.second ?: currentAction

    SettingsRow(label = label, value = displayName) {
        // Cycle to next available option
        val currentIndex = available.indexOfFirst { it.first == currentAction }
        val nextIndex = (currentIndex + 1) % available.size
        onChanged(available[nextIndex].first)
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun SettingsAppPickerSheet(
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val list = pm.queryIntentActivities(intent, 0)
            val mapped = list.map { it.loadLabel(pm).toString() to it.activityInfo.packageName }.sortedBy { it.first.lowercase() }
            apps = mapped
        }
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Select App", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            androidx.compose.material3.OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search apps...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                shape = MaterialTheme.shapes.extraLarge
            )
            androidx.compose.foundation.lazy.LazyColumn {
                val filtered = if (search.isBlank()) apps else apps.filter { it.first.contains(search, ignoreCase = true) }
                items(filtered.size) { i ->
                    val app = filtered[i]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppSelected(app.second) }
                            .padding(vertical = 14.dp)
                    ) {
                        Text(app.first, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
