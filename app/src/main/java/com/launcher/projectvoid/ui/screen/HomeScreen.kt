package com.launcher.projectvoid.ui.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.provider.CalendarContract
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalConfiguration
import com.launcher.projectvoid.LocalFixedStatusBarHeight
import com.launcher.projectvoid.HomeApp
import com.launcher.projectvoid.MainUiState
import com.launcher.projectvoid.R
import com.launcher.projectvoid.data.AppModel
import com.launcher.projectvoid.data.Prefs
import com.launcher.projectvoid.data.Prefs.SwipeAction
import com.launcher.projectvoid.helper.getAppsList
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.activity.compose.BackHandler

// ── Alignment helpers ──

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

private fun gravityToVerticalContentAlignment(gravity: Int): Alignment.Vertical = when (gravity) {
    Gravity.TOP -> Alignment.Top
    Gravity.BOTTOM -> Alignment.Bottom
    else -> Alignment.CenterVertically
}

private fun gravityToTextAlign(gravity: Int): TextAlign = when (gravity) {
    Gravity.CENTER, Gravity.CENTER_HORIZONTAL -> TextAlign.Center
    Gravity.END, Gravity.RIGHT -> TextAlign.End
    else -> TextAlign.Start
}

private fun openScreenTimeDestination(context: android.content.Context) {
    val packageManager = context.packageManager

    val usageAccessIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val candidateIntents = listOf(
        Intent().apply {
            setClassName(
                com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
                "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent().apply {
            setClassName(
                com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
                "com.google.android.apps.wellbeing.home.TopLevelSettingsActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent("com.google.android.apps.wellbeing.VIEW_APP_USAGE").apply {
            setPackage(com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent("android.settings.DIGITAL_WELLBEING_SETTINGS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent().apply {
            setClassName(
                com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        context.packageManager
            .getLaunchIntentForPackage(com.launcher.projectvoid.data.Constants.DIGITAL_WELLBEING_PACKAGE_NAME)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        usageAccessIntent,
        appDetailsIntent
    ).filterNotNull()

    val resolvedIntent = candidateIntents.firstOrNull { intent ->
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    if (resolvedIntent != null) {
        val launchedFallback = resolvedIntent.action == Settings.ACTION_USAGE_ACCESS_SETTINGS ||
            resolvedIntent.action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        if (launchedFallback) {
            Toast.makeText(
                context,
                "Digital Wellbeing not found. Opening a settings fallback.",
                Toast.LENGTH_SHORT
            ).show()
        }
        context.startActivity(resolvedIntent)
        return
    }

    Toast.makeText(
        context,
        "No screen-time destination available on this device.",
        Toast.LENGTH_SHORT
    ).show()
}

// ── Main Home Screen ──

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
    val prefs = remember { Prefs(context) }
    val haptic = LocalHapticFeedback.current
    val clockAlign = gravityToAlignment(state.clockHorizontalAlignment)
    val appAlign = gravityToAlignment(state.appHorizontalAlignment)
    val clockVertical = gravityToVerticalArrangement(state.clockVerticalAlignment)
    val appVerticalAlignment = gravityToVerticalContentAlignment(state.appVerticalAlignment)

    val clockTextAlign = gravityToTextAlign(state.clockHorizontalAlignment)
    val appTextAlign = gravityToTextAlign(state.appHorizontalAlignment)

    // ── Swipe gesture state ──
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val swipeThreshold = 120f
    var showAppPicker by remember { mutableStateOf(false) }

    // ── Dynamic app spacing based on screen real-estate ──
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val displayedCount = state.homeApps.size.coerceAtLeast(1)
    // Base spacing: proportional to available screen height for the apps section (~75% of screen)
    // divided by the number of elements, yielding a natural spacing.
    val dynamicBaseSpacing = ((screenHeightDp * 0.75f) / (displayedCount + 3)).coerceIn(4f, 32f)
    // User slider (appSpacingDp) acts as a multiplier: 0 = compact, 24 = default (1×), 48 = generous (2×)
    val spacingMultiplier = if (state.appSpacingDp <= 0f) 0f else state.appSpacingDp / 24f
    val computedSpacing = (dynamicBaseSpacing * spacingMultiplier).coerceIn(0f, 64f)

    // ── Drag-to-reorder state ──
    // A single continuous touch: long-press → drag → release.
    // `isDragging` is true from the moment the long-press fires until the finger lifts.
    // While isDragging is true, click listeners on items are suppressed.
    val reorderList = remember { mutableStateListOf<HomeApp>() }
    var isDragging by remember { mutableStateOf(false) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val itemHeights = remember { mutableStateListOf<Float>() }

    // Sync reorder list from state when NOT actively dragging.
    LaunchedEffect(state.homeApps) {
        if (!isDragging) {
            reorderList.clear()
            reorderList.addAll(state.homeApps)
        }
    }

    /** Persist the current reorder list to prefs (called on finger lift = clearView equivalent). */
    fun commitReorder() {
        reorderList.forEachIndexed { idx, app ->
            val targetPosition = state.homeApps.getOrNull(idx)?.position ?: (idx + 1)
            prefs.setAppAtLocation(
                targetPosition,
                app.label,
                app.packageName,
                app.activityClassName,
                app.userString,
                app.isShortcut,
                app.shortcutId
            )
        }
        onHomeAppsChanged()
    }

    /** Swap logic shared by both initial long-press-drag and subsequent drags. */
    fun handleDragDelta(deltaY: Float) {
        dragY += deltaY
        val currentHeight = if (draggedIndex in itemHeights.indices) itemHeights[draggedIndex] else 0f

        // Dragging downward — swap with neighbor below.
        if (dragY > 0 && draggedIndex < reorderList.lastIndex) {
            val neighborHeight = if (draggedIndex + 1 in itemHeights.indices) itemHeights[draggedIndex + 1] else currentHeight
            if (dragY > neighborHeight * 0.5f) {
                val from = draggedIndex
                val moved = reorderList.removeAt(from)
                reorderList.add(from + 1, moved)
                draggedIndex = from + 1
                dragY -= neighborHeight
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        // Dragging upward — swap with neighbor above.
        else if (dragY < 0 && draggedIndex > 0) {
            val neighborHeight = if (draggedIndex - 1 in itemHeights.indices) itemHeights[draggedIndex - 1] else currentHeight
            if (-dragY > neighborHeight * 0.5f) {
                val from = draggedIndex
                val moved = reorderList.removeAt(from)
                reorderList.add(from - 1, moved)
                draggedIndex = from - 1
                dragY += neighborHeight
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    // Consume back press on home screen — prevents re-transition to self
    BackHandler { /* Do nothing — home screen is the root */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = LocalFixedStatusBarHeight.current)
            .navigationBarsPadding()
            .pointerInput(state.leftSwipeAction, state.rightSwipeAction, state.enableGestures, isDragging) {
                if (!isDragging) {
                    detectDragGestures(
                        onDragStart = { dragOffset = Offset.Zero },
                        onDragEnd = {
                            val absX = abs(dragOffset.x)
                            val absY = abs(dragOffset.y)
                            if (absX > swipeThreshold || absY > swipeThreshold) {
                                if (absX > absY) {
                                    if (state.enableGestures) {
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
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ════════════════════════════════════════════════════════════════════
            // CLOCK SECTION — top portion of the screen
            // ════════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = clockAlign,
                verticalArrangement = clockVertical
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = clockAlign,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // ── Time ──
                    if (state.showClock) {
                        Text(
                            text = state.currentTime,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.displayLarge.fontSize * state.homeTextSizeScale * (if (state.clockSectionWeight < 0.5f) 1.0f else state.clockSectionWeight) * (if (state.showSeconds) 0.7f else 1.0f),
                                letterSpacing = (-1.5).sp
                            ),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = clockTextAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
                                    } catch (_: Exception) {
                                        onClockClick()
                                    }
                                }
                        )
                    }

                    // ── Date ──
                    if (state.showDate) {
                        Text(
                            text = state.currentDate.uppercase(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.87f),
                            textAlign = clockTextAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                                        context.startActivity(Intent(Intent.ACTION_VIEW).setData(builder.build()))
                                    } catch (_: Exception) {
                                        onDateClick()
                                    }
                                }
                        )
                    }

                    // ── Screen Time ──
                    if (state.showScreenTime && state.screenTime.isNotBlank()) {
                        Text(
                            text = state.screenTime.uppercase(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 1.5.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            textAlign = clockTextAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                                .clickable { openScreenTimeDestination(context) }
                        )
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
            )

            // ════════════════════════════════════════════════════════════════════
            // APPS SECTION — main content area
            // ════════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.75f)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                // App list block — owns vertical alignment independent of footer.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = { showAppPicker = true }
                        )
                ) {
                    val displayApps = if (isDragging) reorderList else state.homeApps

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp, bottom = 12.dp),
                        horizontalAlignment = appAlign,
                        verticalArrangement = Arrangement.spacedBy(computedSpacing.dp, appVerticalAlignment)
                    ) {
                        // Keep itemHeights list in sync with displayApps count.
                        while (itemHeights.size < displayApps.size) itemHeights.add(0f)
                        while (itemHeights.size > displayApps.size) itemHeights.removeAt(itemHeights.lastIndex)

                        displayApps.forEachIndexed { index, app ->
                            val isThisDragged = isDragging && index == draggedIndex

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (isThisDragged) 10f else 0f)
                                    .onGloballyPositioned { coords ->
                                        if (index < itemHeights.size) {
                                            itemHeights[index] = coords.size.height.toFloat()
                                        }
                                    }
                                    .graphicsLayer {
                                        if (isThisDragged) {
                                            translationY = dragY
                                            scaleX = 1.05f
                                            scaleY = 1.05f
                                            alpha = 0.90f
                                        }
                                    }
                            ) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Normal,
                                        fontSize = MaterialTheme.typography.headlineLarge.fontSize * state.homeTextSizeScale,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = when {
                                        isThisDragged -> MaterialTheme.colorScheme.primary
                                        isDragging -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                                        else -> MaterialTheme.colorScheme.onBackground
                                    },
                                    textAlign = appTextAlign,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // Long-press-drag: single continuous gesture — no finger lift.
                                        .pointerInput(index) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    isDragging = true
                                                    reorderList.clear()
                                                    reorderList.addAll(state.homeApps)
                                                    draggedIndex = index
                                                    dragY = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    handleDragDelta(amount.y)
                                                },
                                                onDragEnd = {
                                                    commitReorder()
                                                    isDragging = false
                                                    draggedIndex = -1
                                                    dragY = 0f
                                                },
                                                onDragCancel = {
                                                    isDragging = false
                                                    draggedIndex = -1
                                                    dragY = 0f
                                                }
                                            )
                                        }
                                        .then(
                                            // Click is disabled during an active drag.
                                            if (!isDragging) {
                                                Modifier.clickable { onAppClick(app) }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(vertical = 16.dp)
                                )
                            }
                        }
                    }

                    // Reorder hint.
                    if (isDragging) {
                        Text(
                            text = "Release to confirm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                }

                // ── Footer: battery ──
                HomeFooterBlock(
                    alignment = appAlign,
                    appTextAlign = appTextAlign,
                    batteryLevel = state.batteryLevel
                )
            }
        }
    }

    // ── App picker sheet ──
    if (showAppPicker) {
        HomeAppPickerSheet(
            currentApps = state.homeApps,
            maxApps = state.homeAppsCount.coerceIn(1, 10),
            onDismiss = { showAppPicker = false },
            onHomeAppsChanged = onHomeAppsChanged
        )
    }
}

// ── Sub-composables ──

@Composable
private fun HomeFooterBlock(
    alignment: Alignment.Horizontal,
    appTextAlign: TextAlign,
    batteryLevel: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = alignment
    ) {
        Text(
            text = "$batteryLevel%",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
            textAlign = appTextAlign,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
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

            if (currentApps.isNotEmpty()) {
                Text(
                    "CURRENT (${currentApps.size}/$maxApps)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                currentApps.forEach { app ->
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
                            IconButton(onClick = {
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
