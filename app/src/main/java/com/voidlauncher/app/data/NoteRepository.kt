package com.voidlauncher.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class NoteRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("void_notes", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTES = "notes_json"
    }

    fun getAllNotes(): List<NoteItem> {
        val json = prefs.getString(KEY_NOTES, "[]") ?: "[]"
        return parseNotes(json).sortedWith(
            compareBy<NoteItem> { it.isCompleted }
                .thenByDescending { it.priority }
                .thenByDescending { it.createdAt }
        )
    }

    fun addNote(text: String): NoteItem {
        val notes = getAllNotesRaw().toMutableList()
        val highestPriority = notes.maxOfOrNull { it.priority } ?: 0
        val note = NoteItem(
            text = text,
            priority = highestPriority + 1
        )
        notes.add(note)
        saveNotes(notes)
        return note
    }

    fun toggleComplete(id: Long) {
        val notes = getAllNotesRaw().toMutableList()
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(isCompleted = !notes[index].isCompleted)
            saveNotes(notes)
        }
    }

    fun deleteNote(id: Long) {
        val notes = getAllNotesRaw().toMutableList()
        notes.removeAll { it.id == id }
        saveNotes(notes)
    }

    fun updateNoteText(id: Long, newText: String) {
        val notes = getAllNotesRaw().toMutableList()
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(text = newText)
            saveNotes(notes)
        }
    }

    fun updateNoteReminder(id: Long, triggerTime: Long?) {
        val notes = getAllNotesRaw().toMutableList()
        val index = notes.indexOfFirst { it.id == id }
        if (index != -1) {
            notes[index] = notes[index].copy(reminderTime = triggerTime)
            saveNotes(notes)
        }
    }

    private fun getAllNotesRaw(): List<NoteItem> {
        val json = prefs.getString(KEY_NOTES, "[]") ?: "[]"
        return parseNotes(json)
    }

    private fun saveNotes(notes: List<NoteItem>) {
        val arr = JSONArray()
        notes.forEach { note ->
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("text", note.text)
            obj.put("isCompleted", note.isCompleted)
            obj.put("priority", note.priority)
            obj.put("createdAt", note.createdAt)
            if (note.reminderTime != null) {
                obj.put("reminderTime", note.reminderTime)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_NOTES, arr.toString()).apply()
    }

    private fun parseNotes(json: String): List<NoteItem> {
        val list = mutableListOf<NoteItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    NoteItem(
                        id = obj.getLong("id"),
                        text = obj.getString("text"),
                        isCompleted = obj.getBoolean("isCompleted"),
                        priority = obj.getInt("priority"),
                        createdAt = obj.getLong("createdAt"),
                        reminderTime = if (obj.has("reminderTime")) obj.getLong("reminderTime") else null
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
