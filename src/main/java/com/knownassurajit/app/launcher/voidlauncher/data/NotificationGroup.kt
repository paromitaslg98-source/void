<<<<<<< HEAD:src/main/java/com/knownassurajit/app/launcher/voidlauncher/data/NotificationGroup.kt
package com.knownassurajit.app.launcher.voidlauncher.data
=======
package com.voidlauncher.app.data
>>>>>>> 7c83749 (rebasing develop from stage (#44)):app/src/main/java/com/launcher/projectvoid/data/NotificationGroup.kt

import android.service.notification.StatusBarNotification

data class NotificationGroup(
    val groupKey: String,
    val packageName: String,
    val latestTimestamp: Long,
    val childCount: Int,
    // Highest effective notification importance in this group.
    // Uses channel importance when available; otherwise a legacy-priority fallback.
    val highestImportance: Int,
    val notifications: List<StatusBarNotification>
)
