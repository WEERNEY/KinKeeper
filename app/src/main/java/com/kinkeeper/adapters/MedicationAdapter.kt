package com.kinkeeper.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kinkeeper.databinding.ItemMedicationBinding
import com.kinkeeper.models.Medication

/**
 * Connects the medicines list to its RecyclerView.
 *
 * A RecyclerView creates only enough row views to fill the screen and reuses
 * them as you scroll. onCreateViewHolder inflates a row (called rarely),
 * onBindViewHolder fills an existing row with the data at a position (called
 * constantly), getItemCount says how many rows there are.
 *
 * It takes onEdit and onDelete as lambdas: the adapter knows when a button was
 * tapped, but not what editing means — that is the Activity's job, so it calls
 * back rather than navigating itself.
 */
class MedicationAdapter(
    private var items: MutableList<Medication>,
    private val onEdit: (Medication) -> Unit,
    private val onDelete: (Medication) -> Unit
) : RecyclerView.Adapter<MedicationAdapter.MedicationViewHolder>() {

    inner class MedicationViewHolder(val binding: ItemMedicationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val binding = ItemMedicationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MedicationViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        val medication = items[position]
        with(holder.binding) {
            textName.text = medication.name
            textDose.text = medication.dose.ifBlank { "No dose recorded" }
            textTimes.text = if (medication.times.isEmpty()) {
                "No times set"
            } else {
                medication.times.joinToString("   ")
            }
            textNotes.text = medication.notes
            textNotes.visibility = if (medication.notes.isBlank()) View.GONE else View.VISIBLE

            buttonEdit.setOnClickListener { onEdit(medication) }
            buttonDelete.setOnClickListener { onDelete(medication) }
            root.setOnClickListener { onEdit(medication) }
        }
    }

    fun replaceAll(newItems: MutableList<Medication>) {
        items = newItems
        notifyDataSetChanged()
    }
}
