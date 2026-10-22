package com.kinkeeper

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kinkeeper.databinding.ActivityMedicationFormBinding
import com.kinkeeper.databinding.RowTimeBinding
import com.kinkeeper.models.Medication
import java.util.Calendar
import java.util.Locale

/**
 * Screen 3. Add or edit one medicine.
 *
 * It deliberately saves nothing itself. It reads a Medication out of the
 * Intent, lets the user change it, and hands it back with setResult.
 * MedicationsActivity owns storage. The form knows how to edit a medicine; it
 * does not need to know where medicines are kept.
 *
 * Times are added with a TimePickerDialog and formatted "%02d:%02d", so "08:00"
 * rather than "8:00" — that leading zero is what makes sorting them as strings
 * give the correct chronological order.
 */
class MedicationFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEDICATION = "extra_medication"
    }

    private lateinit var binding: ActivityMedicationFormBinding
    private lateinit var medication: Medication
    private var isNew = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Read what the previous screen sent us, if anything.
        val incoming = intent.getSerializableExtra(EXTRA_MEDICATION, Medication::class.java)
        isNew = incoming == null
        medication = incoming ?: Medication()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title =
            getString(if (isNew) R.string.new_medication else R.string.edit_medication)
        binding.toolbar.setNavigationOnClickListener { cancel() }

        binding.inputName.setText(medication.name)
        binding.inputDose.setText(medication.dose)
        binding.inputNotes.setText(medication.notes)
        drawTimes()

        binding.buttonAddTime.setOnClickListener { pickTime() }
        binding.buttonSave.setOnClickListener { save() }
        binding.buttonCancel.setOnClickListener { cancel() }
    }

    /** Rebuilds the list of time rows under the "Times each day" heading. */
    private fun drawTimes() {
        binding.containerTimes.removeAllViews()
        medication.times.sort()

        binding.textNoTimes.visibility =
            if (medication.times.isEmpty()) View.VISIBLE else View.GONE

        medication.times.forEach { time ->
            val row = RowTimeBinding.inflate(
                LayoutInflater.from(this), binding.containerTimes, false
            )
            row.textTime.text = time
            row.buttonRemoveTime.setOnClickListener {
                medication.times.remove(time)
                drawTimes()
            }
            binding.containerTimes.addView(row.root)
        }
    }

    private fun pickTime() {
        val now = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                if (!medication.times.contains(formatted)) {
                    medication.times.add(formatted)
                    drawTimes()
                }
            },
            now.get(Calendar.HOUR_OF_DAY),
            0,
            true // 24-hour clock
        ).show()
    }

    private fun save() {
        val name = binding.inputName.text.toString().trim()

        if (name.isEmpty()) {
            binding.layoutName.error = getString(R.string.needs_name)
            return
        }
        binding.layoutName.error = null

        if (medication.times.isEmpty()) {
            Snackbar.make(binding.root, R.string.needs_time, Snackbar.LENGTH_LONG).show()
            return
        }

        medication.name = name
        medication.dose = binding.inputDose.text.toString().trim()
        medication.notes = binding.inputNotes.text.toString().trim()

        // Hand the finished object back to MedicationsActivity.
        val result = Intent().putExtra(EXTRA_MEDICATION, medication)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}
