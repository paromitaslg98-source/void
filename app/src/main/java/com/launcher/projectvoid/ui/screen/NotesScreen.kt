package com.launcher.projectvoid.ui.screen

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.launcher.projectvoid.LocalFixedStatusBarHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.launcher.projectvoid.R
import com.launcher.projectvoid.data.NoteItem
import com.launcher.projectvoid.data.NoteRepository
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun NotesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NoteRepository(context) }
    val notes = remember { mutableStateListOf<NoteItem>() }
    var newNoteText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        notes.clear()
        notes.addAll(repo.getAllNotes())
    }

    fun refreshNotes() {
        notes.clear()
        notes.addAll(repo.getAllNotes())
    }

    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = LocalFixedStatusBarHeight.current)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Add note input
            OutlinedTextField(
                value = newNoteText,
                onValueChange = { newNoteText = it },
                placeholder = {
                    Text(
                        stringResource(R.string.add_note_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val text = newNoteText.trim()
                        if (text.isNotBlank()) {
                            repo.addNote(text)
                            refreshNotes()
                        }
                        newNoteText = ""
                    }
                )
            )

            // Auto-add on newline directly from hardware keyboard
            LaunchedEffect(newNoteText) {
                // Auto-add on newline
                if (newNoteText.endsWith("\n")) {
                    val text = newNoteText.trim()
                    if (text.isNotBlank()) {
                        repo.addNote(text)
                        refreshNotes()
                    }
                    newNoteText = ""
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes list with swipe-to-delete
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.no_notes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = notes.toList(), key = { it.id }) { note ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value != SwipeToDismissBoxValue.Settled) {
                                    repo.deleteNote(note.id)
                                    refreshNotes()
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
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = stringResource(R.string.delete_note),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        ) {
                            NoteRow(
                                note = note,
                                onToggle = {
                                    repo.toggleComplete(note.id)
                                    refreshNotes()
                                },
                                onDelete = {
                                    repo.deleteNote(note.id)
                                    refreshNotes()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NoteRow(note: NoteItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { NoteRepository(context) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    // MD3 DatePickerDialog
    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }

    // MD3 TimePickerDialog (custom since M3 doesn't have a built-in dialog wrapper)
    if (showTimePicker && selectedDateMillis != null) {
        val timePickerState = androidx.compose.material3.rememberTimePickerState(
            initialHour = 9, initialMinute = 0, is24Hour = false
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis!!
                        set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(java.util.Calendar.MINUTE, timePickerState.minute)
                        set(java.util.Calendar.SECOND, 0)
                    }
                    val triggerTime = cal.timeInMillis
                    repo.updateNoteReminder(note.id, triggerTime)

                    // Schedule alarm
                    try {
                        val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
                        val alarmIntent = Intent(context, com.launcher.projectvoid.helper.NoteReminderReceiver::class.java).apply {
                            putExtra("note_id", note.id)
                            putExtra("note_text", note.text)
                        }
                        val pending = android.app.PendingIntent.getBroadcast(
                            context, note.id.toInt(), alarmIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerTime, pending)
                    } catch (_: Exception) {}

                    // Insert calendar event via intent (transparent to user)
                    try {
                        val calIntent = Intent(Intent.ACTION_INSERT).apply {
                            data = android.provider.CalendarContract.Events.CONTENT_URI
                            putExtra(android.provider.CalendarContract.Events.TITLE, note.text)
                            putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, triggerTime)
                            putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, triggerTime + 30 * 60 * 1000)
                        }
                        context.startActivity(calIntent)
                    } catch (_: Exception) {}

                    showTimePicker = false
                }) { Text("Set Reminder") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    // Card-based note item
    androidx.compose.material3.OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = note.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    text = note.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (note.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Reminder badge
                if (note.reminderTime != null && note.reminderTime > 0L) {
                    val formatted = remember(note.reminderTime) {
                        java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
                            .format(java.util.Date(note.reminderTime))
                    }
                    Text(
                        text = "⏰ $formatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_note),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
