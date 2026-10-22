package com.kinkeeper

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kinkeeper.databinding.ActivityProfileBinding
import com.kinkeeper.models.PatientProfile
import com.kinkeeper.services.StorageService

/**
 * Screen 7. The details of the person being cared for — everything the
 * emergency card displays comes from here.
 *
 * Uses a Spinner for blood group and a Switch to force large text on the
 * emergency card permanently, overriding the light sensor.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.profile_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val groups = resources.getStringArray(R.array.blood_groups)
        binding.spinnerBlood.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, groups
        )

        val profile = StorageService.loadProfile(this)
        binding.inputName.setText(profile.name)
        binding.inputAge.setText(profile.age)
        binding.inputAllergies.setText(profile.allergies)
        binding.inputConditions.setText(profile.conditions)
        binding.inputDoctor.setText(profile.doctor)
        binding.inputContact.setText(profile.contact)
        binding.switchLargeText.isChecked = profile.alwaysLargeText

        val index = groups.indexOf(profile.bloodGroup)
        binding.spinnerBlood.setSelection(if (index >= 0) index else 0)

        binding.buttonSave.setOnClickListener { save() }
    }

    private fun save() {
        val profile = PatientProfile(
            name = binding.inputName.text.toString().trim(),
            age = binding.inputAge.text.toString().trim(),
            bloodGroup = binding.spinnerBlood.selectedItem?.toString() ?: "Not known",
            allergies = binding.inputAllergies.text.toString().trim(),
            conditions = binding.inputConditions.text.toString().trim(),
            doctor = binding.inputDoctor.text.toString().trim(),
            contact = binding.inputContact.text.toString().trim(),
            alwaysLargeText = binding.switchLargeText.isChecked
        )
        StorageService.saveProfile(this, profile)
        Snackbar.make(binding.root, R.string.saved, Snackbar.LENGTH_SHORT).show()
        binding.root.postDelayed({ finish() }, 700)
    }
}
