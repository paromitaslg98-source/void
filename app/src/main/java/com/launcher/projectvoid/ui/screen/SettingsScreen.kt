package com.launcher.projectvoid.ui.screen

import android.provider.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.launcher.projectvoid.R
import com.launcher.projectvoid.data.Prefs
import com.launcher.projectvoid.data.Prefs.SwipeAction

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 24.dp)
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "ⓘ", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = "⚙", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        try {
                            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_SETTINGS)
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
            SettingsToggle(label = stringResource(R.string.screen_time), checked = showScreenTime) {
                showScreenTime = it; prefs.showScreenTimeWidget = it
            }
        }

        // ── APP LIBRARY Section ──
        SettingsSection(title = "App Library") {
            SettingsToggle(label = "Show Alphabet Categories", checked = showAlphabetCategories) {
                showAlphabetCategories = it; prefs.showAlphabetCategories = it
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
        SettingsSection(title = stringResource(R.string.gestures)) {
            SwipeActionSelector(
                label = stringResource(R.string.left_swipe_action),
                currentAction = leftSwipeAction,
                excludeAction = rightSwipeAction
            ) {
                leftSwipeAction = it; prefs.leftSwipeAction = it
            }
            SwipeActionSelector(
                label = stringResource(R.string.right_swipe_action),
                currentAction = rightSwipeAction,
                excludeAction = leftSwipeAction
            ) {
                rightSwipeAction = it; prefs.rightSwipeAction = it
            }
            SettingsRow(
                label = stringResource(R.string.swipe_down_for),
                value = if (swipeDownAction == 1) stringResource(R.string.search) else stringResource(R.string.notifications)
            ) {
                swipeDownAction = if (swipeDownAction == 1) 2 else 1
                prefs.swipeDownAction = swipeDownAction
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
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
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
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
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Text(text = value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
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
    onChanged: (String) -> Unit
) {
    val allActions = listOf(
        SwipeAction.NOTIFICATION_SUMMARY to "Notification Summary",
        SwipeAction.WIDGETS to "Widgets",
        SwipeAction.NOTES to "Notes"
    )
    // Filter out the action already selected for the other gesture
    val available = allActions.filter { it.first != excludeAction }
    val displayName = allActions.firstOrNull { it.first == currentAction }?.second ?: currentAction

    SettingsRow(label = label, value = displayName) {
        // Cycle to next available option
        val currentIndex = available.indexOfFirst { it.first == currentAction }
        val nextIndex = (currentIndex + 1) % available.size
        onChanged(available[nextIndex].first)
    }
}
