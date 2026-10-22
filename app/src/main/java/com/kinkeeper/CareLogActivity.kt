package com.kinkeeper

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kinkeeper.adapters.CareNoteAdapter
import com.kinkeeper.databinding.ActivityCareLogBinding
import com.kinkeeper.models.CareNote
import com.kinkeeper.services.StorageService

/**
 * Screen 6. A running diary of how the patient has been, so the carer can
 * answer "how has she been sleeping?" at the next appointment.
 *
 * Form and list on one screen, since notes are short. Each note is stamped
 * with the moment it was written and listed newest first.
 */
class CareLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCareLogBinding
    private lateinit var adapter: CareNoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCareLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.care_log_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = CareNoteAdapter(mutableListOf()) { note -> confirmDelete(note) }
        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter

        binding.buttonAddNote.setOnClickListener { addNote() }

        refresh()
    }

    private fun refresh() {
        val notes = StorageService.loadNotes(this)
        adapter.replaceAll(notes)
        binding.textEmptyNotes.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun addNote() {
        val text = binding.inputNote.text.toString().trim()
        if (text.isEmpty()) {
            binding.layoutNote.error = getString(R.string.needs_note)
            return
        }
        binding.layoutNote.error = null

        // The timestamp comes from the CareNote default, so the note records
        // when it was written rather than when it is read.
        StorageService.addNote(this, CareNote(text = text))
        binding.inputNote.setText("")
        hideKeyboard()
        refresh()
        binding.recyclerNotes.scrollToPosition(0)
        Snackbar.make(binding.root, R.string.note_added, Snackbar.LENGTH_SHORT).show()
    }

    private fun confirmDelete(note: CareNote) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_note_question)
            .setMessage(note.text)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                StorageService.deleteNote(this, note.id)
                refresh()
            }
            .show()
    }

    private fun hideKeyboard() {
        val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(binding.inputNote.windowToken, 0)
    }
}
