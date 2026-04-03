package com.launcher.projectvoid.ui.screen

import android.app.Application
import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.launcher.projectvoid.data.AppNotificationSummary
import com.launcher.projectvoid.helper.AiSummarizer
import com.launcher.projectvoid.helper.NotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

// ── ViewModel ──

class NotificationSummaryViewModel(application: Application) : AndroidViewModel(application) {
    private val summarizer = AiSummarizer(application.applicationContext)

    private val _summaries = MutableStateFlow<List<AppNotificationSummary>>(emptyList())
    val summaries: StateFlow<List<AppNotificationSummary>> = _summaries.asStateFlow()

    private val _isAiAvailable = MutableStateFlow<Boolean?>(null)
    val isAiAvailable: StateFlow<Boolean?> = _isAiAvailable.asStateFlow()

    init {
        viewModelScope.launch {
            _isAiAvailable.value = summarizer.isAvailable()
        }

        viewModelScope.launch {
            NotificationService.notificationsState.collect { groups ->
                val pm = application.packageManager
                val summaryList = groups
                    .sortedByDescending { it.latestTimestamp }
                    .map { group ->
                        val texts = group.notifications.map { sbn ->
                            val extras = sbn.notification.extras
                            val title = extras.getCharSequence("android.title")?.toString() ?: ""
                            val text = extras.getCharSequence("android.text")?.toString() ?: ""
                            if (title.isNotBlank() && text.isNotBlank()) "$title: $text"
                            else title.ifBlank { text }
                        }.filter { it.isNotBlank() }

                        AppNotificationSummary(
                            packageName = group.packageName,
                            appLabel = group.appLabel,
                            notifications = group.notifications,
                            latestTimestamp = group.latestTimestamp,
                            isLoading = true
                        )
                    }

                _summaries.value = summaryList

                // Generate AI summaries in background
                summaryList.forEachIndexed { index, summary ->
                    launch {
                        val texts = summary.notifications.map { sbn ->
                            val extras = sbn.notification.extras
                            val title = extras.getCharSequence("android.title")?.toString() ?: ""
                            val text = extras.getCharSequence("android.text")?.toString() ?: ""
                            if (title.isNotBlank() && text.isNotBlank()) "$title: $text"
                            else title.ifBlank { text }
                        }.filter { it.isNotBlank() }

                        val aiResult = summarizer.summarize(summary.appLabel, texts)
                        _summaries.value = _summaries.value.toMutableList().also {
                            if (index < it.size) {
                                it[index] = it[index].copy(
                                    aiSummary = aiResult ?: texts.joinToString(". "),
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun dismissApp(packageName: String) {
        NotificationService.dismissNotificationsForPackage(packageName)
        _summaries.value = _summaries.value.filter { it.packageName != packageName }
    }
}

// ── Screen ──

@Composable
fun NotificationSummaryScreen(
    onBack: () -> Unit,
    viewModel: NotificationSummaryViewModel = viewModel()
) {
    val summaries by viewModel.summaries.collectAsState()
    val aiAvailable by viewModel.isAiAvailable.collectAsState()
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 24.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragOffset = Offset.Zero },
                    onDragEnd = {
                        if (abs(dragOffset.x) > 120f && abs(dragOffset.x) > abs(dragOffset.y)) {
                            if (dragOffset.x > 0) onBack() // swipe right → back
                        }
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = { dragOffset = Offset.Zero },
                    onDrag = { change, amount -> change.consume(); dragOffset += amount }
                )
            }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notification Summary",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "✨",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        if (aiAvailable == false) {
            Text(
                text = "AI summary unavailable on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (summaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notifications to summarize",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = summaries,
                    key = { it.packageName }
                ) { summary ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                viewModel.dismissApp(summary.packageName)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "Remove",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    ) {
                        SummaryCard(summary)
                    }
                }
            }
        }
    } // Column
    } // Box
}

@Composable
private fun SummaryCard(summary: AppNotificationSummary) {
    val relativeTime = remember(summary.latestTimestamp) {
        DateUtils.getRelativeTimeSpanString(
            summary.latestTimestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // App header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary.appLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${summary.notifications.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Summary or loading
            if (summary.isLoading) {
                Text(
                    text = "Summarizing…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = summary.aiSummary ?: "No summary available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
