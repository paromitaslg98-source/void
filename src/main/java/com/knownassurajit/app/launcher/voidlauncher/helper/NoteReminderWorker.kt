package com.knownassurajit.app.launcher.voidlauncher.helper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NoteReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getLong(NoteReminderReceiver.EXTRA_NOTE_ID, -1L)
        val noteText = inputData.getString(NoteReminderReceiver.EXTRA_NOTE_TEXT).orEmpty()
        val noteTitle = inputData.getString(NoteReminderReceiver.EXTRA_NOTE_TITLE)

        if (noteId <= 0L || noteText.isBlank()) {
            return Result.failure()
        }

        NoteReminderReceiver.dispatchReminder(applicationContext, noteId, noteTitle, noteText)
        return Result.success()
    }
}
