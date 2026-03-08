package com.voidlauncher.app.data

data class NoteItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isCompleted: Boolean = false,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
