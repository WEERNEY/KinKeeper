package com.kinkeeper.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kinkeeper.databinding.ItemCareNoteBinding
import com.kinkeeper.models.CareNote

/**
 * Connects the care log to its RecyclerView. The simplest of the four: a date
 * badge, the note text and a delete button. Only one callback, because notes
 * can be written and deleted but not edited.
 */
class CareNoteAdapter(
    private var items: MutableList<CareNote>,
    private val onDelete: (CareNote) -> Unit
) : RecyclerView.Adapter<CareNoteAdapter.CareNoteViewHolder>() {

    inner class CareNoteViewHolder(val binding: ItemCareNoteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CareNoteViewHolder {
        val binding = ItemCareNoteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CareNoteViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: CareNoteViewHolder, position: Int) {
        val note = items[position]
        with(holder.binding) {
            textNote.text = note.text
            textDate.text = note.formattedDate()
            buttonDeleteNote.setOnClickListener { onDelete(note) }
        }
    }

    fun replaceAll(newItems: MutableList<CareNote>) {
        items = newItems
        notifyDataSetChanged()
    }
}
