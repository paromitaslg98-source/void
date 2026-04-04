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
import androidx.compose.foundation.layout.systemBarsPadding
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
            .padding(top = 48.dp, bottom = 24.dp)
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

@Composable
private fun NoteRow(note: NoteItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_MESSAGE, note.text)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback if no alarm app found
                }
            }
            .padding(vertical = 8.dp),
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

        Text(
            text = note.text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (note.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (note.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.delete_note),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
