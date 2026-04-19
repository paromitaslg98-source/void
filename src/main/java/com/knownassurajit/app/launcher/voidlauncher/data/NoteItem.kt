<<<<<<< HEAD:src/main/java/com/knownassurajit/app/launcher/voidlauncher/data/NoteItem.kt
package com.knownassurajit.app.launcher.voidlauncher.data
=======
package com.voidlauncher.app.data
>>>>>>> 7c83749 (rebasing develop from stage (#44)):app/src/main/java/com/launcher/projectvoid/data/NoteItem.kt

data class NoteItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isCompleted: Boolean = false,
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null
)
