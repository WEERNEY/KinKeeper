package com.kinkeeper.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kinkeeper.databinding.ItemAppointmentBinding
import com.kinkeeper.models.Appointment

/**
 * Connects the appointments list to its RecyclerView.
 *
 * Appointments already in the past are drawn at 45% opacity so the next one
 * coming up is what the eye lands on. The alpha is set in both branches, not
 * just the past one — on a recycled row, forgetting to reset it would leave a
 * future appointment faded.
 */
class AppointmentAdapter(
    private var items: MutableList<Appointment>,
    private val onEdit: (Appointment) -> Unit,
    private val onDelete: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    inner class AppointmentViewHolder(val binding: ItemAppointmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemAppointmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AppointmentViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = items[position]
        with(holder.binding) {
            textTitle.text = appointment.title
            textDate.text = appointment.formattedDate()
            textTime.text = appointment.formattedTime()

            textLocation.text = appointment.location
            textLocation.visibility =
                if (appointment.location.isBlank()) View.GONE else View.VISIBLE

            textNotes.text = appointment.notes
            textNotes.visibility = if (appointment.notes.isBlank()) View.GONE else View.VISIBLE

            // Appointments that have already happened fade back so the next
            // one coming up is what the eye lands on.
            root.alpha = if (appointment.isPast()) 0.45f else 1f

            buttonEdit.setOnClickListener { onEdit(appointment) }
            buttonDelete.setOnClickListener { onDelete(appointment) }
            root.setOnClickListener { onEdit(appointment) }
        }
    }

    fun replaceAll(newItems: MutableList<Appointment>) {
        items = newItems
        notifyDataSetChanged()
    }
}
