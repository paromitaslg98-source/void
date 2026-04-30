package com.knownassurajit.app.launcher.voidlauncher.helper

import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.knownassurajit.app.launcher.voidlauncher.MainActivity
import com.knownassurajit.app.launcher.voidlauncher.R

class NoteReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "note_reminders"
        const val ACTION_IN_APP_REMINDER = "com.knownassurajit.app.launcher.voidlauncher.ACTION_IN_APP_REMINDER"
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_NOTE_TITLE = "note_title"
        const val EXTRA_NOTE_TEXT = "note_text"
        const val NOTIFICATION_GROUP = "note_reminder_group"

        @android.annotation.SuppressLint("MissingPermission")
        fun dispatchReminder(context: Context, noteId: Long, noteTitle: String?, noteText: String) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            createReminderChannel(nm)

            val interruptionFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                nm.currentInterruptionFilter
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            val ringerMode = audioManager.ringerMode
            val appInForeground = AppLifecycleState.isAppInForeground

            if (appInForeground) {
                context.sendBroadcast(
                    Intent(ACTION_IN_APP_REMINDER)
                        .setPackage(context.packageName)
                        .putExtra(EXTRA_NOTE_ID, noteId)
                        .putExtra(EXTRA_NOTE_TEXT, noteText)
                )
                return
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NOTE_ID, noteId)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                noteId.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val canInterrupt = interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL ||
                interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_edit)
                .setContentTitle(noteTitle ?: context.getString(R.string.note_reminder_title))
                .setContentText(noteText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(noteText))
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setGroup(NOTIFICATION_GROUP)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            if (ringerMode == AudioManager.RINGER_MODE_VIBRATE && canInterrupt) {
                builder.setVibrate(longArrayOf(0, 300, 200, 300))
            }

            if (ringerMode == AudioManager.RINGER_MODE_SILENT || !canInterrupt) {
                builder.setSilent(true)
            }

            if (ringerMode == AudioManager.RINGER_MODE_NORMAL && canInterrupt && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND)
            }

            @android.annotation.SuppressLint("MissingPermission", "NotificationPermission")
            NotificationManagerCompat.from(context).notify(noteId.toInt(), builder.build())
        }

        private fun createReminderChannel(nm: NotificationManager) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Note Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminder notifications for notes"
                enableVibration(true)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        val noteText = intent.getStringExtra(EXTRA_NOTE_TEXT) ?: return
        if (noteId <= 0L) return
        dispatchReminder(context, noteId, intent.getStringExtra(EXTRA_NOTE_TITLE), noteText)
    }
}
