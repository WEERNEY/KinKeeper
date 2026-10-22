package com.kinkeeper.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kinkeeper.databinding.ItemDoseBinding
import com.kinkeeper.models.DoseItem

/**
 * Connects today's schedule to the RecyclerView on the home screen.
 *
 * Note the checkbox listener is set to null before isChecked is assigned. Row
 * views are recycled and may still carry the listener from the medicine they
 * showed previously; without clearing it first, setting the state fires that
 * old listener and marks the wrong dose as taken.
 */
class DoseAdapter(
    private var items: MutableList<DoseItem>,
    private val onToggle: (DoseItem, Boolean) -> Unit
) : RecyclerView.Adapter<DoseAdapter.DoseViewHolder>() {

    inner class DoseViewHolder(val binding: ItemDoseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoseViewHolder {
        val binding = ItemDoseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DoseViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: DoseViewHolder, position: Int) {
        val dose = items[position]
        with(holder.binding) {
            textTime.text = dose.time
            textMedication.text = dose.medicationName
            textDose.text = dose.dose

            // Clear the listener first, otherwise recycled rows fire it while binding.
            checkTaken.setOnCheckedChangeListener(null)
            checkTaken.isChecked = dose.taken
            checkTaken.setOnCheckedChangeListener { _, isChecked ->
                dose.taken = isChecked
                onToggle(dose, isChecked)
            }
        }
    }

    fun replaceAll(newItems: MutableList<DoseItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
