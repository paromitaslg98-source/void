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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.Widgets
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
    var appSpacing by remember { mutableFloatStateOf(prefs.appSpacingDp) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name) + " Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    IconButton(onClick = { showDeveloperInfo = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "Credits")
                    }
                    IconButton(onClick = {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                        } catch (_: Exception) {}
                    }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "System Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            
            Spacer(modifier = Modifier.height(8.dp))
            SettingActionItem(
                title = stringResource(R.string.change_default_launcher),
                subtitle = "Manage default home app settings",
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                onClick = {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                        context.startActivity(intent)
                    }
                }
            )

            // --- Clock & Date Section ---
            SettingsSectionHeader("Clock & Date")
            SettingToggleItem(
                title = "Show Clock",
                subtitle = "Top-level time display",
                icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                checked = showClock,
                onCheckedChange = { showClock = it; prefs.showClockWidget = it }
            )
            SettingToggleItem(
                title = stringResource(R.string.show_date_time),
                subtitle = "Current date under the clock",
                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                checked = showDate,
                onCheckedChange = { showDate = it; prefs.showDateWidget = it }
            )
            SettingToggleItem(
                title = stringResource(R.string.screen_time),
                subtitle = "Device usage metrics",
                icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                checked = showScreenTime,
                onInfoClick = {
                    // Show standard info or we can rely on Snackbar for usage stats logic
                },
                onCheckedChange = {
                    showScreenTime = it; prefs.showScreenTimeWidget = it
                    if (it) {
                        val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                        } else {
                            @Suppress("DEPRECATION")
                            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                        }
                        if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
                            try {
                                context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                scope.launch { snackbarHostState.showSnackbar("Redirected to Usage Access settings") }
                            } catch (e: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Could not open settings automatically") }
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Screen Time enabled") }
                        }
                    }
                }
            )
            SettingAlignmentItem(
                title = "Clock Horizontal Alignment",
                subtitle = "Left, Center, or Right",
                icon = { Icon(Icons.Default.LinearScale, contentDescription = null) },
                currentGravity = clockHorizontalAlignment,
                options = listOf(Gravity.START to "Left", Gravity.CENTER_HORIZONTAL to "Center", Gravity.END to "Right"),
                onChanged = { clockHorizontalAlignment = it; prefs.clockAlignment = it }
            )
            SettingAlignmentItem(
                title = "Clock Vertical Alignment",
                subtitle = "Top, Center, or Bottom",
                icon = { Icon(Icons.Default.Transform, contentDescription = null) },
                currentGravity = clockVerticalAlignment,
                options = listOf(Gravity.TOP to "Top", Gravity.CENTER_VERTICAL to "Center", Gravity.BOTTOM to "Bottom"),
                onChanged = { clockVerticalAlignment = it; prefs.clockVerticalAlignment = it }
            )
            SettingSliderItem(
                title = "Clock Size",
                subtitle = "Adjust clock scale",
                icon = { Icon(Icons.Default.ViewDay, contentDescription = null) },
                value = if (clockSectionWeight < 0.5f) 1.0f else clockSectionWeight,
                range = 0.5f..3.0f,
                onChanged = { clockSectionWeight = it; prefs.clockSectionWeight = it }
            )

            // --- Home Apps Section ---
            SettingsSectionHeader("Home Apps")
            SettingCounterItem(
                title = "Max apps on Home Screen",
                subtitle = "Limit applications shown",
                icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                value = maxHomeApps,
                onDecrease = { if (maxHomeApps > 1) { maxHomeApps--; prefs.maxHomeApps = maxHomeApps } },
                onIncrease = { if (maxHomeApps < 15) { maxHomeApps++; prefs.maxHomeApps = maxHomeApps } }
            )
            SettingAlignmentItem(
                title = "Apps Horizontal Alignment",
                subtitle = "Left, Center, or Right",
                icon = { Icon(Icons.Default.LinearScale, contentDescription = null) },
                currentGravity = appHorizontalAlignment,
                options = listOf(Gravity.START to "Left", Gravity.CENTER_HORIZONTAL to "Center", Gravity.END to "Right"),
                onChanged = { appHorizontalAlignment = it; prefs.homeAlignment = it }
            )
            SettingAlignmentItem(
                title = "Apps Vertical Alignment",
                subtitle = "Top, Center, or Bottom",
                icon = { Icon(Icons.Default.Transform, contentDescription = null) },
                currentGravity = appVerticalAlignment,
                options = listOf(Gravity.TOP to "Top", Gravity.CENTER_VERTICAL to "Center", Gravity.BOTTOM to "Bottom"),
                onChanged = { appVerticalAlignment = it; prefs.homeVerticalAlignment = it }
            )
            SettingSliderItem(
                title = "App Spacing",
                subtitle = "Space between app labels",
                icon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }, // SortByAlpha is just a placeholder icon
                value = appSpacing,
                range = 0f..48f,
                onChanged = { appSpacing = it; prefs.appSpacingDp = it }
            )

            // --- Text Size Section ---
            SettingsSectionHeader("Text Size")
            SettingSliderItem(
                title = stringResource(R.string.home_text_size),
                subtitle = "Scale font on home",
                icon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                value = homeTextSize,
                range = 0.5f..2.0f,
                onChanged = { homeTextSize = it; prefs.homeTextSizeScale = it }
            )
            SettingSliderItem(
                title = stringResource(R.string.app_drawer_text_size),
                subtitle = "Scale font in drawer",
                icon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                value = drawerTextSize,
                range = 0.5f..2.0f,
                onChanged = { drawerTextSize = it; prefs.appDrawerTextSizeScale = it }
            )

            // --- App Library Section ---
            SettingsSectionHeader("App Library")
            SettingToggleItem(
                title = "Show Alphabet Categories",
                subtitle = "Group by starting letter",
                icon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) },
                checked = showAlphabetCategories,
                onCheckedChange = { showAlphabetCategories = it; prefs.showAlphabetCategories = it }
            )
            SettingToggleItem(
                title = "Enable Private Space",
                subtitle = "Unlock OS hidden profiles",
                icon = { Icon(Icons.Default.Security, contentDescription = null) },
                checked = privateSpaceEnabled,
                onCheckedChange = { 
                    privateSpaceEnabled = it; prefs.privateSpaceEnabled = it
                    scope.launch { snackbarHostState.showSnackbar(if (it) "Private Space Enabled" else "Private Space Disabled") }
                }
            )

            // --- Appearance Section ---
            SettingsSectionHeader(stringResource(R.string.appearance))
            SettingToggleItem(
                title = stringResource(R.string.notification_bar),
                subtitle = "Show system status icons",
                icon = { Icon(Icons.Default.SpaceBar, contentDescription = null) },
                checked = showStatusBar,
                onCheckedChange = { showStatusBar = it; prefs.showStatusBar = it }
            )
            SettingToggleItem(
                title = stringResource(R.string.auto_show_keyboard),
                subtitle = "Pop up keyboard on search",
                icon = { Icon(Icons.Default.Keyboard, contentDescription = null) },
                checked = autoShowKeyboard,
                onCheckedChange = { autoShowKeyboard = it; prefs.autoShowKeyboard = it }
            )

            // --- Gestures Section ---
            var enableGestures by remember { mutableStateOf(prefs.enableGestures) }
            var enableSummary by remember { mutableStateOf(prefs.enableNotificationSummary) }
            var enableWidgets by remember { mutableStateOf(prefs.enableWidgets) }
            var enableNotes by remember { mutableStateOf(prefs.enableNotes) }

            SettingsSectionHeader(stringResource(R.string.gestures))
            SettingToggleItem(
                title = "Enable Gestures",
                subtitle = "Activate screen swipes",
                icon = { Icon(Icons.Default.Swipe, contentDescription = null) },
                checked = enableGestures,
                onCheckedChange = { enableGestures = it; prefs.enableGestures = it }
            )
            if (enableGestures) {
                SettingToggleItem(
                    title = "Enable Notification Summary",
                    subtitle = "AI curated updates",
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    checked = enableSummary,
                    onCheckedChange = { 
                        enableSummary = it; prefs.enableNotificationSummary = it
                        if (!it) {
                            if (leftSwipeAction == SwipeAction.NOTIFICATION_SUMMARY) { leftSwipeAction = SwipeAction.NONE; prefs.leftSwipeAction = SwipeAction.NONE }
                            if (rightSwipeAction == SwipeAction.NOTIFICATION_SUMMARY) { rightSwipeAction = SwipeAction.NONE; prefs.rightSwipeAction = SwipeAction.NONE }
                        }
                    }
                )
                SettingToggleItem(
                    title = "Enable Widgets Screen",
                    subtitle = "Swipe to native widgets",
                    icon = { Icon(Icons.Default.Widgets, contentDescription = null) },
                    checked = enableWidgets,
                    onCheckedChange = { 
                        enableWidgets = it; prefs.enableWidgets = it
                        if (!it) {
                            if (leftSwipeAction == SwipeAction.WIDGETS) { leftSwipeAction = SwipeAction.NONE; prefs.leftSwipeAction = SwipeAction.NONE }
                            if (rightSwipeAction == SwipeAction.WIDGETS) { rightSwipeAction = SwipeAction.NONE; prefs.rightSwipeAction = SwipeAction.NONE }
                        }
                    }
                )
                SettingToggleItem(
                    title = "Enable Notes Screen",
                    subtitle = "Quick scratchpad area",
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    checked = enableNotes,
                    onCheckedChange = { 
                        enableNotes = it; prefs.enableNotes = it
                        if (!it) {
                            if (leftSwipeAction == SwipeAction.NOTES) { leftSwipeAction = SwipeAction.NONE; prefs.leftSwipeAction = SwipeAction.NONE }
                            if (rightSwipeAction == SwipeAction.NOTES) { rightSwipeAction = SwipeAction.NONE; prefs.rightSwipeAction = SwipeAction.NONE }
                        }
                    }
                )
                
                SwipeActionSelector(
                    label = stringResource(R.string.left_swipe_action),
                    subtitle = "Assign left swipe behavior",
                    icon = { Icon(Icons.Default.SwipeLeft, contentDescription = null) },
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
                    subtitle = "Assign right swipe behavior",
                    icon = { Icon(Icons.Default.SwipeRight, contentDescription = null) },
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

            Spacer(modifier = Modifier.height(32.dp))
        }

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

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary
    )
}

// ── Reusable Component Logic ──

@Composable
fun SettingActionItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle.isNotEmpty()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingToggleItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    if (onInfoClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (subtitle.isNotEmpty()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
fun SettingSliderItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChanged: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    if (subtitle.isNotEmpty()) {
                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
}

@Composable
fun SettingAlignmentItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    currentGravity: Int,
    options: List<Pair<Int, String>>,
    onChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    if (subtitle.isNotEmpty()) {
                        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (optionGravity, optionLabel) ->
                    val isSelected = currentGravity == optionGravity
                    Card(
                        modifier = Modifier.weight(1f).clickable { onChanged(optionGravity) },
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
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
}

@Composable
fun SettingCounterItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle.isNotEmpty()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(text = "$value", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = onIncrease) {
                    Icon(Icons.Outlined.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SwipeActionSelector(
    label: String,
    subtitle: String,
    icon: @Composable () -> Unit,
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

    val available = allActions.filter {
        it.first != excludeAction || it.first == SwipeAction.NONE || it.first == SwipeAction.NOTIFICATIONS || it.first == SwipeAction.APP || it.first == SwipeAction.ACCESSIBILITY
    }
    val displayName = allActions.firstOrNull { it.first == currentAction }?.second ?: currentAction

    SettingActionItem(
        title = label,
        subtitle = displayName,
        icon = icon,
        onClick = {
            val currentIndex = available.indexOfFirst { it.first == currentAction }
            val nextIndex = (currentIndex + 1) % available.size
            onChanged(available[nextIndex].first)
        }
    )
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
