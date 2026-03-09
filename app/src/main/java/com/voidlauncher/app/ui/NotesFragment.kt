package com.voidlauncher.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Paint
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.voidlauncher.app.R
import com.voidlauncher.app.data.NoteItem
import com.voidlauncher.app.data.NoteRepository
import com.voidlauncher.app.databinding.FragmentNotesBinding
import com.voidlauncher.app.databinding.RowNoteItemBinding
import com.voidlauncher.app.helper.NoteReminderReceiver
import com.voidlauncher.app.helper.NoteReminderWorker
import com.voidlauncher.app.listener.OnSwipeTouchListener
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: NoteRepository
    private lateinit var adapter: NoteAdapter
    private val inAppReminderReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            val noteId = intent?.getLongExtra(NoteReminderReceiver.EXTRA_NOTE_ID, -1L) ?: -1L
            val noteText = intent?.getStringExtra(NoteReminderReceiver.EXTRA_NOTE_TEXT).orEmpty()
            if (noteId <= 0L || noteText.isBlank()) return
            com.google.android.material.snackbar.Snackbar.make(binding.root, "Reminder: $noteText", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setAction("Open") { focusNote(noteId) }
                .show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Suppress("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = NoteRepository(requireContext())
        adapter = NoteAdapter()

        binding.rvNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotes.adapter = adapter

        // Swipe to delete notes
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                val item = adapter.items[pos]
                repo.deleteNote(item.id)
                refreshList()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvNotes)

        // Add note on keyboard "Done" / Enter
        binding.etNewNote.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val text = binding.etNewNote.text.toString().trim()
                if (text.isNotEmpty()) {
                    repo.addNote(text)
                    binding.etNewNote.text?.clear()
                    refreshList()
                }
                true
            } else false
        }

        // Reverse swipe: swipe right (finger left-to-right) → go back to home
        view.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeRight() {
                findNavController().popBackStack()
            }
        })

        refreshList()
        handleReminderDeepLink()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            inAppReminderReceiver,
            IntentFilter(NoteReminderReceiver.ACTION_IN_APP_REMINDER),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        runCatching { requireContext().unregisterReceiver(inAppReminderReceiver) }
        super.onStop()
    }

    private fun refreshList() {
        val notes = repo.getAllNotes()
        adapter.submitList(notes)
        val hasNotes = notes.isNotEmpty()
        binding.rvNotes.visibility = if (hasNotes) View.VISIBLE else View.GONE
        binding.tvNoNotes.visibility = if (hasNotes) View.GONE else View.VISIBLE
    }

    private fun showNoteMenu(anchor: View, note: NoteItem) {
        val popup = PopupMenu(requireContext(), anchor, android.view.Gravity.END)
        popup.menuInflater.inflate(R.menu.menu_note_options, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    showEditDialog(note)
                    true
                }
                R.id.action_remind -> {
                    showDateTimePicker(note)
                    true
                }
                R.id.action_delete -> {
                    repo.deleteNote(note.id)
                    refreshList()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showEditDialog(note: NoteItem) {
        val editText = EditText(requireContext()).apply {
            setText(note.text)
            setSelection(note.text.length)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit)
            .setView(editText)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    repo.updateNoteText(note.id, newText)
                    refreshList()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDateTimePicker(note: NoteItem) {
        val now = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                TimePickerDialog(
                    requireContext(),
                    { _, hour, minute ->
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                        }
                        scheduleReminder(note, cal.timeInMillis)
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun scheduleReminder(note: NoteItem, triggerTime: Long) {
        val initialDelay = (triggerTime - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<NoteReminderWorker>()
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(NoteReminderReceiver.EXTRA_NOTE_ID, note.id)
                    .putString(NoteReminderReceiver.EXTRA_NOTE_TITLE, getString(R.string.note_reminder_title))
                    .putString(NoteReminderReceiver.EXTRA_NOTE_TEXT, note.text)
                    .build()
            )
            .addTag("note_reminder_${note.id}")
            .build()
        WorkManager.getInstance(requireContext()).enqueueUniqueWork(
            "note_reminder_${note.id}",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
        
        repo.updateNoteReminder(note.id, triggerTime)
        refreshList()

        val remainingMillis = triggerTime - System.currentTimeMillis()
        val remainingText = if (remainingMillis > 0) {
            android.text.format.DateUtils.getRelativeTimeSpanString(triggerTime, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS)
        } else {
            "Now"
        }
        com.google.android.material.snackbar.Snackbar.make(binding.root, "Reminder set for $remainingText", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleReminderDeepLink() {
        val noteId = activity?.intent?.getLongExtra(NoteReminderReceiver.EXTRA_NOTE_ID, -1L) ?: -1L
        if (noteId > 0L) {
            focusNote(noteId)
            activity?.intent?.removeExtra(NoteReminderReceiver.EXTRA_NOTE_ID)
        }
    }

    private fun focusNote(noteId: Long) {
        val index = adapter.items.indexOfFirst { it.id == noteId }
        if (index >= 0) {
            binding.rvNotes.scrollToPosition(index)
            com.google.android.material.snackbar.Snackbar.make(binding.root, "Opened reminder note", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        }
    }

    inner class NoteAdapter : RecyclerView.Adapter<NoteAdapter.ViewHolder>() {

        var items = listOf<NoteItem>()
            private set

        fun submitList(newItems: List<NoteItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = RowNoteItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(private val binding: RowNoteItemBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(note: NoteItem) {
                binding.tvNoteText.text = note.text
                binding.cbNote.isChecked = note.isCompleted

                if (note.isCompleted) {
                    binding.tvNoteText.paintFlags = binding.tvNoteText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    binding.tvNoteText.alpha = 0.5f
                } else {
                    binding.tvNoteText.paintFlags = binding.tvNoteText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    binding.tvNoteText.alpha = 1.0f
                }

                if (note.reminderTime != null) {
                    binding.tvReminderTime.visibility = View.VISIBLE
                    val timeFormat = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                    binding.tvReminderTime.text = "Remind at ${timeFormat.format(java.util.Date(note.reminderTime))}"
                } else {
                    binding.tvReminderTime.visibility = View.GONE
                }

                binding.cbNote.setOnClickListener {
                    repo.toggleComplete(note.id)
                    refreshList()
                }

                binding.btnNoteMenu.setOnClickListener {
                    showNoteMenu(it, note)
                }
            }
        }
    }
}
