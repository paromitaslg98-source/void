package com.knownassurajit.app.launcher.voidlauncher.ui.screen

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knownassurajit.app.launcher.voidlauncher.LocalFixedStatusBarHeight
import com.knownassurajit.app.launcher.voidlauncher.data.Prefs
import com.knownassurajit.app.launcher.voidlauncher.data.WidgetInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val WIDGET_HOST_ID = 1024

// ── ViewModel ──

class WidgetsViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx = application.applicationContext
    private val prefs = Prefs(ctx)

    val appWidgetHost = AppWidgetHost(ctx, WIDGET_HOST_ID)

    private val _allWidgets = MutableStateFlow<List<WidgetInfo>>(emptyList())
    val allWidgets: StateFlow<List<WidgetInfo>> = _allWidgets.asStateFlow()

    private val _pinnedWidgets = MutableStateFlow<List<WidgetInfo>>(emptyList())
    val pinnedWidgets: StateFlow<List<WidgetInfo>> = _pinnedWidgets.asStateFlow()

    private val _widgetIds = MutableStateFlow<Map<String, Int>>(emptyMap())
    val widgetIds: StateFlow<Map<String, Int>> = _widgetIds.asStateFlow()

    init { loadWidgets() }

    override fun onCleared() {
        super.onCleared()
        appWidgetHost.stopListening()
    }

    private fun loadWidgets() {
        try {
            val manager = AppWidgetManager.getInstance(ctx)
            val pm = ctx.packageManager
            val all = manager.installedProviders.mapNotNull { info ->
                try {
                    val label = info.loadLabel(pm)
                    val appName = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                    } catch (_: Exception) { info.provider.packageName }
                    WidgetInfo(provider = info, label = label, previewImage = null, appName = appName)
                } catch (_: Exception) { null }
            }.sortedBy { it.appName }
            _allWidgets.value = all
            refreshPinnedAndIds(all)
        } catch (_: Exception) {}
    }

    private fun refreshPinnedAndIds(all: List<WidgetInfo> = _allWidgets.value) {
        var pinned = prefs.pinnedWidgets
        val idMap = buildWidgetIdMap()

        // Drop pinned entries that have no allocated ID (e.g., migrating from old version)
        val orphans = pinned.filter { key -> !idMap.containsKey(key) }
        if (orphans.isNotEmpty()) {
            pinned = pinned.toMutableSet().also { it.removeAll(orphans.toSet()) }
            prefs.pinnedWidgets = pinned
        }

        _pinnedWidgets.value = all.filter { widget ->
            val key = widget.provider.provider.flattenToString()
            idMap.containsKey(key) && pinned.contains(key)
        }
        _widgetIds.value = idMap
    }

    private fun buildWidgetIdMap(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        prefs.widgetAllocatedIds.forEach { entry ->
            val idx = entry.lastIndexOf('|')
            if (idx > 0) {
                val id = entry.substring(idx + 1).toIntOrNull()
                if (id != null) map[entry.substring(0, idx)] = id
            }
        }
        return map
    }

    fun pinWidget(widget: WidgetInfo): Boolean {
        val key = widget.provider.provider.flattenToString()
        if (prefs.pinnedWidgets.contains(key)) return true

        val manager = AppWidgetManager.getInstance(ctx)
        val appWidgetId = appWidgetHost.allocateAppWidgetId()

        return try {
            val bound = manager.bindAppWidgetIdIfAllowed(appWidgetId, widget.provider.provider)
            if (bound) {
                val ids = prefs.widgetAllocatedIds.toMutableSet()
                ids.removeAll { it.startsWith("$key|") }
                ids.add("$key|$appWidgetId")
                prefs.widgetAllocatedIds = ids

                val pins = prefs.pinnedWidgets.toMutableSet()
                pins.add(key)
                prefs.pinnedWidgets = pins

                refreshPinnedAndIds()
                true
            } else {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
                false
            }
        } catch (_: Exception) {
            try { appWidgetHost.deleteAppWidgetId(appWidgetId) } catch (_: Exception) {}
            false
        }
    }

    fun unpinWidget(widget: WidgetInfo) {
        val key = widget.provider.provider.flattenToString()
        prefs.widgetAllocatedIds.find { it.startsWith("$key|") }
            ?.substringAfterLast('|')?.toIntOrNull()
            ?.let { id -> try { appWidgetHost.deleteAppWidgetId(id) } catch (_: Exception) {} }

        prefs.widgetAllocatedIds = prefs.widgetAllocatedIds.toMutableSet()
            .also { it.removeAll { e -> e.startsWith("$key|") } }
        prefs.pinnedWidgets = prefs.pinnedWidgets.toMutableSet().also { it.remove(key) }
        refreshPinnedAndIds()
    }

    fun isPinned(widget: WidgetInfo): Boolean =
        prefs.pinnedWidgets.contains(widget.provider.provider.flattenToString())
}

// ── Screen ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    onBack: () -> Unit,
    viewModel: WidgetsViewModel = viewModel()
) {
    val pinnedWidgets by viewModel.pinnedWidgets.collectAsState()
    val allWidgets by viewModel.allWidgets.collectAsState()
    val widgetIds by viewModel.widgetIds.collectAsState()
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var widgetToRemove by remember { mutableStateOf<WidgetInfo?>(null) }

    DisposableEffect(Unit) {
        viewModel.appWidgetHost.startListening()
        onDispose { viewModel.appWidgetHost.stopListening() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = LocalFixedStatusBarHeight.current)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Widgets",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = { showPicker = true }) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add widget",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Add", color = MaterialTheme.colorScheme.primary)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (pinnedWidgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No widgets added",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap Add to pin widgets from your installed apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = pinnedWidgets,
                    key = { it.provider.provider.flattenToString() }
                ) { widget ->
                    val widgetId = widgetIds[widget.provider.provider.flattenToString()]
                    if (widgetId != null) {
                        LiveWidgetItem(
                            widget = widget,
                            widgetId = widgetId,
                            appWidgetHost = viewModel.appWidgetHost,
                            onRemoveClick = { widgetToRemove = widget }
                        )
                    }
                }
            }
        }
    }

    if (showPicker) {
        WidgetPickerSheet(
            allWidgets = allWidgets,
            viewModel = viewModel,
            onDismiss = { showPicker = false },
            onBindFailed = {
                Toast.makeText(
                    context,
                    "Cannot bind widget. Set VOID Launcher as default launcher and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    widgetToRemove?.let { widget ->
        AlertDialog(
            onDismissRequest = { widgetToRemove = null },
            title = { Text("Remove Widget", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Remove \"${widget.label}\" from your widget screen?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unpinWidget(widget)
                    widgetToRemove = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { widgetToRemove = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LiveWidgetItem(
    widget: WidgetInfo,
    widgetId: Int,
    appWidgetHost: AppWidgetHost,
    onRemoveClick: () -> Unit
) {
    val widgetHeightDp = remember(widget.provider) {
        maxOf(widget.provider.minHeight, 100).dp
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = widget.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove ${widget.label}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Live system widget — touch events pass through directly to the widget
        AndroidView(
            factory = { ctx ->
                try {
                    appWidgetHost.createView(ctx, widgetId, widget.provider).apply {
                        setAppWidget(widgetId, widget.provider)
                    }
                } catch (_: Exception) {
                    android.widget.FrameLayout(ctx)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(widgetHeightDp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPickerSheet(
    allWidgets: List<WidgetInfo>,
    viewModel: WidgetsViewModel,
    onDismiss: () -> Unit,
    onBindFailed: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groupedWidgets = remember(allWidgets) {
        allWidgets.groupBy { it.appName }.toSortedMap()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Add Widgets",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                groupedWidgets.forEach { (appName, widgets) ->
                    item(key = "group_$appName") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${widgets.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(items = widgets, key = { "picker_${it.provider.provider}" }) { widget ->
                        val pinned = viewModel.isPinned(widget)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!pinned) {
                                        val ok = viewModel.pinWidget(widget)
                                        if (!ok) onBindFailed()
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = widget.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (pinned) Icons.Outlined.Check else Icons.Outlined.Add,
                                contentDescription = null,
                                tint = if (pinned) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
