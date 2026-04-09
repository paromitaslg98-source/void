package com.launcher.projectvoid.ui.screen

import android.app.Application
import android.app.Notification
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

// ── ViewModel ──

class NotificationSummaryViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "NotificationSummaryVM"
        private const val SUMMARY_DEBOUNCE_MS = 250L
    }

    private val summarizer = AiSummarizer(application.applicationContext)
    private val summaryJobs = mutableMapOf<String, Job>()
    private var generation = 0L

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
                generation += 1
                val currentGeneration = generation
                val activePackages = groups.map { it.packageName }.toSet()
                summaryJobs.keys.filter { it !in activePackages }.forEach { pkg ->
                    summaryJobs.remove(pkg)?.cancel()
                }

                val summaryList = groups
                    .sortedByDescending { it.latestTimestamp }
                    .map { group ->
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
                summaryList.forEach { summary ->
                    summaryJobs[summary.packageName]?.cancel()
                    summaryJobs[summary.packageName] = launch {
                        val texts = extractNotificationTexts(summary.notifications)

                        val aiResult = summarizer.summarize(summary.appLabel, texts)
                        if (currentGeneration != generation) return@launch

                        val fallbackSummary = texts.joinToString(". ")
                        _summaries.value = _summaries.value.map { current ->
                            if (current.packageName == summary.packageName) {
                                current.copy(
                                    aiSummary = aiResult ?: fallbackSummary,
                                    isLoading = false
                                )
                            } else {
                                current
                            }
                        }
                    }
                }
            }
        }
    }

    fun dismissApp(packageName: String) {
        summaryJobs.remove(packageName)?.cancel()
        NotificationService.dismissNotificationsForPackage(packageName)
        _summaries.value = _summaries.value.filter { it.packageName != packageName }
    }

    private fun extractNotificationTexts(notifications: List<android.service.notification.StatusBarNotification>): List<String> {
        return notifications.mapNotNull { sbn ->
            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
            val text = extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()
            val bigText = extras.getCharSequence("android.bigText")?.toString()?.trim().orEmpty()
            val lines = extras.getCharSequenceArray("android.textLines")
                ?.map { it.toString().trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()

            when {
                title.isNotBlank() && bigText.isNotBlank() -> "$title: $bigText"
                title.isNotBlank() && text.isNotBlank() -> "$title: $text"
                bigText.isNotBlank() -> bigText
                text.isNotBlank() -> text
                lines.isNotEmpty() -> lines.joinToString(" • ")
                title.isNotBlank() -> title
                else -> null
            }
        }
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
            .padding(top = 48.dp, bottom = 24.dp)
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Notification Summary",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            // Total notification count badge
            val totalCount = summaries.sumOf { it.notifications.size }
            if (totalCount > 0) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (aiAvailable == false) {
            Text(
                text = "On-device AI summary is not available on this device. Displaying raw notification content.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        if (summaries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "All clear",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No pending notifications to summarize",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
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
                                    text = "Dismiss",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
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

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // App header with notification count badge
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
                    Spacer(modifier = Modifier.width(8.dp))
                    // Notification count badge
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${summary.notifications.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                    text = "Summarizing notifications...",
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
