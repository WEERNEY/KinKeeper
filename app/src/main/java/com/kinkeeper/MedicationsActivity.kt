package com.kinkeeper

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kinkeeper.adapters.MedicationAdapter
import com.kinkeeper.databinding.ActivityMedicationsBinding
import com.kinkeeper.models.Medication
import com.kinkeeper.services.StorageService
import java.util.Locale

/**
 * Screen 2. The medicines list in a RecyclerView, with a search box that
 * filters by name or dose on every keystroke.
 *
 * This is the "data to the next Activity and back" requirement. openForm()
 * puts a Medication into an Intent as a Serializable extra and launches the
 * form; formLauncher, registered with registerForActivityResult, receives the
 * edited one back and is what actually saves it.
 *
 * Passing null to openForm() means "new medicine" — no extra is attached — so
 * one function handles both Add and Edit.
 */
class MedicationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicationsBinding
    private lateinit var adapter: MedicationAdapter

    // The full list stays here; the RecyclerView only ever shows the matches.
    private var allMedications = mutableListOf<Medication>()

    // Registers a callback for the result before the screen is even shown.
    // This replaces the old startActivityForResult / onActivityResult pair.
    private val formLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val returned = result.data?.getSerializableExtra(
                MedicationFormActivity.EXTRA_MEDICATION, Medication::class.java
            )
            if (returned != null) {
                StorageService.saveMedication(this, returned)
                refresh()
                Snackbar.make(binding.root, "${returned.name} saved.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.medications_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MedicationAdapter(
            mutableListOf(),
            onEdit = { medication -> openForm(medication) },
            onDelete = { medication -> confirmDelete(medication) }
        )
        binding.recyclerMedications.layoutManager = LinearLayoutManager(this)
        binding.recyclerMedications.adapter = adapter

        binding.buttonAdd.setOnClickListener { openForm(null) }

        // A TextWatcher fires on every keystroke in the search box.
        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString() ?: "")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        allMedications = StorageService.loadMedications(this)
        applyFilter(binding.inputSearch.text?.toString() ?: "")
    }

    /** Keeps medicines whose name or dose contains the typed text. */
    private fun applyFilter(query: String) {
        val trimmed = query.trim().lowercase(Locale.getDefault())
        val matches = if (trimmed.isEmpty()) {
            allMedications.toMutableList()
        } else {
            allMedications.filter {
                it.name.lowercase(Locale.getDefault()).contains(trimmed) ||
                    it.dose.lowercase(Locale.getDefault()).contains(trimmed)
            }.toMutableList()
        }

        adapter.replaceAll(matches)

        // Two different empty states: nothing saved at all, or nothing matching.
        when {
            allMedications.isEmpty() -> {
                binding.textEmpty.visibility = View.VISIBLE
                binding.textEmpty.text = getString(R.string.no_medications)
            }
            matches.isEmpty() -> {
                binding.textEmpty.visibility = View.VISIBLE
                binding.textEmpty.text = getString(R.string.no_matches)
            }
            else -> binding.textEmpty.visibility = View.GONE
        }
    }

    /** null means "new medicine", otherwise we send the existing one over to be edited. */
    private fun openForm(medication: Medication?) {
        val intent = Intent(this, MedicationFormActivity::class.java)
        if (medication != null) {
            intent.putExtra(MedicationFormActivity.EXTRA_MEDICATION, medication)
        }
        formLauncher.launch(intent)
    }

    private fun confirmDelete(medication: Medication) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_question)
            .setMessage(R.string.delete_body)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                StorageService.deleteMedication(this, medication.id)
                refresh()
                Snackbar.make(
                    binding.root, "${medication.name} deleted.", Snackbar.LENGTH_SHORT
                ).show()
            }
            .show()
    }
}
