package com.voidlauncher.app.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.voidlauncher.app.R

class NoteReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "note_reminders"
        const val EXTRA_NOTE_TEXT = "note_text"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val noteText = intent.getStringExtra(EXTRA_NOTE_TEXT) ?: return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (no-op on repeated calls)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_edit)
            .setContentTitle("Note Reminder")
            .setContentText(noteText)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(noteText.hashCode(), notification)
    }
}
