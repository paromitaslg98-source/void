package com.launcher.projectvoid.data

data class NoteItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isCompleted: Boolean = false,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null
)
