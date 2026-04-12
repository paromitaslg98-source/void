package com.knownassurajit.app.launcher.voidlauncher.ui.screen

import android.app.Application
import android.appwidget.AppWidgetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.knownassurajit.app.launcher.voidlauncher.LocalFixedStatusBarHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.knownassurajit.app.launcher.voidlauncher.data.Prefs
import com.knownassurajit.app.launcher.voidlauncher.data.WidgetInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

// ── ViewModel ──

class WidgetsViewModel(application: Application) : AndroidViewModel(application) {
    private val ctx = application.applicationContext
    private val prefs = Prefs(ctx)
    private val _allWidgets = MutableStateFlow<List<WidgetInfo>>(emptyList())
    val allWidgets: StateFlow<List<WidgetInfo>> = _allWidgets.asStateFlow()

    private val _pinnedWidgets = MutableStateFlow<List<WidgetInfo>>(emptyList())
    val pinnedWidgets: StateFlow<List<WidgetInfo>> = _pinnedWidgets.asStateFlow()

    private val _widgetSpans = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val widgetSpans: StateFlow<Map<String, Pair<Int, Int>>> = _widgetSpans.asStateFlow()

    init { loadWidgets() }

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
                    val preview = try { info.loadPreviewImage(ctx, 0) } catch (_: Exception) { null }
                    WidgetInfo(provider = info, label = label, previewImage = preview, appName = appName)
                } catch (_: Exception) { null }
            }.sortedBy { it.appName }
            _allWidgets.value = all
            refreshPinned(all)
        } catch (_: Exception) {}
    }

    private fun refreshPinned(all: List<WidgetInfo> = _allWidgets.value) {
        val pinned = prefs.pinnedWidgets
        _pinnedWidgets.value = all.filter { pinned.contains(it.provider.provider.flattenToString()) }
        refreshSpans()
    }

    private fun refreshSpans() {
        val map = mutableMapOf<String, Pair<Int, Int>>()
        prefs.widgetSpans.forEach { entry ->
            val parts = entry.split("|")
            if (parts.size == 3) {
                map[parts[0]] = Pair(parts[1].toIntOrNull() ?: 2, parts[2].toIntOrNull() ?: 2)
            }
        }
        _widgetSpans.value = map
    }

    fun togglePin(widget: WidgetInfo) {
        val key = widget.provider.provider.flattenToString()
        val current = prefs.pinnedWidgets.toMutableSet()
        if (current.contains(key)) {
            current.remove(key)
            // Cleanup span mapping when unpinned
            val currentSpans = prefs.widgetSpans.toMutableSet()
            currentSpans.removeAll { it.startsWith("$key|") }
            prefs.widgetSpans = currentSpans
        } else {
            current.add(key)
            // Add default 2x2 span mapping when pinned
            val currentSpans = prefs.widgetSpans.toMutableSet()
            currentSpans.removeAll { it.startsWith("$key|") }
            currentSpans.add("$key|2|2")
            prefs.widgetSpans = currentSpans
        }
        prefs.pinnedWidgets = current
        refreshPinned()
    }

    fun updateWidgetSpan(widget: WidgetInfo, spanX: Int, spanY: Int) {
        val key = widget.provider.provider.flattenToString()
        val currentSpans = prefs.widgetSpans.toMutableSet()
        currentSpans.removeAll { it.startsWith("$key|") }
        currentSpans.add("$key|$spanX|$spanY")
        prefs.widgetSpans = currentSpans
        refreshSpans()
    }

    fun isPinned(widget: WidgetInfo): Boolean {
        return prefs.pinnedWidgets.contains(widget.provider.provider.flattenToString())
    }
}

// ── Screen ──

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    onBack: () -> Unit,
    viewModel: WidgetsViewModel = viewModel()
) {
    val pinnedWidgets by viewModel.pinnedWidgets.collectAsState()
    val allWidgets by viewModel.allWidgets.collectAsState()
    val widgetSpans by viewModel.widgetSpans.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var widgetToResize by remember { mutableStateOf<WidgetInfo?>(null) }

    // Reverse swipe detection (swipe left → back if opened via swipe right)
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = LocalFixedStatusBarHeight.current)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Widgets",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (pinnedWidgets.isEmpty()) {
                // Empty state — long press to add
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = { showPicker = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No widgets added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Long press to add widgets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = pinnedWidgets,
                        key = { "${it.provider.provider}" },
                        span = { widget ->
                            val spanX = widgetSpans[widget.provider.provider.flattenToString()]?.first ?: 2
                            GridItemSpan(spanX)
                        }
                    ) { widget ->
                        val spanY = widgetSpans[widget.provider.provider.flattenToString()]?.second ?: 2
                        Box(
                            modifier = Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = { widgetToResize = widget }
                            )
                        ) {
                            WidgetCard(widget, spanY)
                        }
                    }
                }
            }
        }
    }

    // Widget picker bottom sheet
    if (showPicker) {
        WidgetPickerSheet(
            allWidgets = allWidgets,
            viewModel = viewModel,
            onDismiss = { showPicker = false }
        )
    }

    if (widgetToResize != null) {
        AlertDialog(
            onDismissRequest = { widgetToResize = null },
            title = { Text("Widget Options", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    val currentSpan = widgetSpans[widgetToResize!!.provider.provider.flattenToString()] ?: Pair(2, 2)
                    Text(
                        "Resize",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    arrayOf("2x1", "2x2", "4x2").forEach { size ->
                        val (sx, sy) = when(size) {
                            "2x1" -> Pair(2, 1)
                            "4x2" -> Pair(4, 2)
                            else -> Pair(2, 2)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateWidgetSpan(widgetToResize!!, sx, sy)
                                    widgetToResize = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (currentSpan == Pair(sx, sy)) Icons.Outlined.Check else Icons.Outlined.Add, 
                                contentDescription = null,
                                tint = if (currentSpan == Pair(sx, sy)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(size, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    // Remove widget option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.togglePin(widgetToResize!!)
                                widgetToResize = null
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Remove Widget",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { widgetToResize = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPickerSheet(
    allWidgets: List<WidgetInfo>,
    viewModel: WidgetsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Add/Remove Widgets",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Group widgets by app name
            val groupedWidgets = remember(allWidgets) {
                allWidgets.groupBy { it.appName }.toSortedMap()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
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
                                .clickable { viewModel.togglePin(widget) }
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
                                tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WidgetCard(widget: WidgetInfo, spanY: Int = 2) {
    val heightDp = if (spanY == 1) 70.dp else 140.dp
    val grayscaleBitmap = remember(widget) {
        try {
            val drawable = widget.previewImage ?: return@remember null
            val w = drawable.intrinsicWidth.coerceAtLeast(100)
            val h = drawable.intrinsicHeight.coerceAtLeast(100)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            val grayBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val grayCanvas = Canvas(grayBmp)
            val paint = Paint()
            val cm = ColorMatrix()
            cm.setSaturation(0f)
            paint.colorFilter = ColorMatrixColorFilter(cm)
            grayCanvas.drawBitmap(bmp, 0f, 0f, paint)
            bmp.recycle()
            grayBmp
        } catch (_: Exception) { null }
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (grayscaleBitmap != null) {
                Image(
                    bitmap = grayscaleBitmap.asImageBitmap(),
                    contentDescription = widget.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.6f
                )
            } else {
                Text(
                    text = widget.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
