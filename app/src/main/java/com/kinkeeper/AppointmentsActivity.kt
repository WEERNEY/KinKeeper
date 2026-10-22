package com.kinkeeper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kinkeeper.adapters.AppointmentAdapter
import com.kinkeeper.databinding.ActivityAppointmentsBinding
import com.kinkeeper.models.Appointment
import com.kinkeeper.services.StorageService

/**
 * Screen 4. The appointments list, built exactly like the medicines list: a
 * RecyclerView here, and the same Intent round trip out to the form and back.
 * Sorted soonest first by StorageService.
 */
class AppointmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppointmentsBinding
    private lateinit var adapter: AppointmentAdapter

    private val formLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val returned = result.data?.getSerializableExtra(
                AppointmentFormActivity.EXTRA_APPOINTMENT, Appointment::class.java
            )
            if (returned != null) {
                StorageService.saveAppointment(this, returned)
                refresh()
                Snackbar.make(binding.root, "${returned.title} saved.", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.appointments_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = AppointmentAdapter(
            mutableListOf(),
            onEdit = { appointment -> openForm(appointment) },
            onDelete = { appointment -> confirmDelete(appointment) }
        )
        binding.recyclerAppointments.layoutManager = LinearLayoutManager(this)
        binding.recyclerAppointments.adapter = adapter

        binding.buttonAddAppointment.setOnClickListener { openForm(null) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val appointments = StorageService.loadAppointments(this)
        adapter.replaceAll(appointments)
        binding.textEmptyAppointments.visibility =
            if (appointments.isEmpty()) View.VISIBLE else View.GONE
    }

    /** null means "new appointment", otherwise we send the existing one to be edited. */
    private fun openForm(appointment: Appointment?) {
        val intent = Intent(this, AppointmentFormActivity::class.java)
        if (appointment != null) {
            intent.putExtra(AppointmentFormActivity.EXTRA_APPOINTMENT, appointment)
        }
        formLauncher.launch(intent)
    }

    private fun confirmDelete(appointment: Appointment) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_appointment_question)
            .setMessage(appointment.title)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                StorageService.deleteAppointment(this, appointment.id)
                refresh()
            }
            .show()
    }
}
