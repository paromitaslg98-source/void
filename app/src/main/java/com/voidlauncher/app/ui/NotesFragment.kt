package com.voidlauncher.app.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.voidlauncher.app.R
import com.voidlauncher.app.data.NoteItem
import com.voidlauncher.app.data.NoteRepository
import com.voidlauncher.app.databinding.FragmentNotesBinding
import com.voidlauncher.app.databinding.RowNoteItemBinding

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var repo: NoteRepository
    private lateinit var adapter: NoteAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = NoteRepository(requireContext())
        adapter = NoteAdapter()

        binding.rvNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotes.adapter = adapter

        // Swipe to delete
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

        refreshList()
    }

    private fun refreshList() {
        val notes = repo.getAllNotes()
        adapter.submitList(notes)
        val hasNotes = notes.isNotEmpty()
        binding.rvNotes.visibility = if (hasNotes) View.VISIBLE else View.GONE
        binding.tvNoNotes.visibility = if (hasNotes) View.GONE else View.VISIBLE
    }

    private fun showNoteMenu(anchor: View, note: NoteItem) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, R.string.edit)
        popup.menu.add(0, 2, 1, R.string.remind)
        popup.menu.add(0, 3, 2, R.string.delete_note)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    showEditDialog(note)
                    true
                }
                2 -> {
                    setReminder(note)
                    true
                }
                3 -> {
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

    private fun setReminder(note: NoteItem) {
        // Schedule a simple 30-minute reminder notification
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), android.content.BroadcastReceiver::class.java)
        intent.putExtra("note_text", note.text)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            note.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + 30 * 60 * 1000 // 30 minutes
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        requireContext().let {
            android.widget.Toast.makeText(it, R.string.reminder_set, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
