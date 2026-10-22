package com.kinkeeper

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kinkeeper.databinding.ActivityAppointmentFormBinding
import com.kinkeeper.models.Appointment
import java.util.Calendar

/**
 * Screen 5. Add or edit one appointment. Like the medicine form it saves
 * nothing itself and returns the edited object with setResult.
 *
 * The date and the time are chosen with two separate pickers but stored as one
 * Long, so a Calendar holds the working value while the user edits: the date
 * picker sets year/month/day, the time picker sets hour/minute, each leaving
 * the other fields alone.
 */
class AppointmentFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_APPOINTMENT = "extra_appointment"
    }

    private lateinit var binding: ActivityAppointmentFormBinding
    private lateinit var appointment: Appointment
    private val calendar: Calendar = Calendar.getInstance()
    private var isNew = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val incoming = intent.getSerializableExtra(EXTRA_APPOINTMENT, Appointment::class.java)
        isNew = incoming == null
        appointment = incoming ?: Appointment()
        calendar.timeInMillis = appointment.whenMillis

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title =
            getString(if (isNew) R.string.new_appointment else R.string.edit_appointment)
        binding.toolbar.setNavigationOnClickListener { cancel() }

        binding.inputTitle.setText(appointment.title)
        binding.inputLocation.setText(appointment.location)
        binding.inputApptNotes.setText(appointment.notes)
        showDateAndTime()

        binding.buttonPickDate.setOnClickListener { pickDate() }
        binding.buttonPickTime.setOnClickListener { pickTime() }
        binding.buttonSaveAppointment.setOnClickListener { save() }
        binding.buttonCancelAppointment.setOnClickListener { cancel() }
    }

    private fun showDateAndTime() {
        val preview = Appointment(whenMillis = calendar.timeInMillis)
        binding.buttonPickDate.text = preview.formattedDate()
        binding.buttonPickTime.text = preview.formattedTime()
    }

    private fun pickDate() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                showDateAndTime()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickTime() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                showDateAndTime()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24-hour clock
        ).show()
    }

    private fun save() {
        val title = binding.inputTitle.text.toString().trim()

        if (title.isEmpty()) {
            binding.layoutTitle.error = getString(R.string.needs_title)
            return
        }
        binding.layoutTitle.error = null

        appointment.title = title
        appointment.location = binding.inputLocation.text.toString().trim()
        appointment.notes = binding.inputApptNotes.text.toString().trim()
        appointment.whenMillis = calendar.timeInMillis

        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_APPOINTMENT, appointment))
        finish()
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}
