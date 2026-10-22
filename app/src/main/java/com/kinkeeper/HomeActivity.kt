package com.kinkeeper

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.kinkeeper.adapters.DoseAdapter
import com.kinkeeper.databinding.ActivityHomeBinding
import com.kinkeeper.models.DoseItem
import com.kinkeeper.models.PatientProfile
import com.kinkeeper.services.StorageService
import kotlin.math.sqrt

/**
 * Screen 1, the dashboard and the app's launcher Activity.
 *
 * Shows the custom dose ring, today's schedule in a RecyclerView, the next
 * appointment and the latest care note, plus buttons to every other screen.
 *
 * Sensor 1 of 2: the accelerometer, used for fall detection. The three axis
 * readings are combined with Pythagoras and divided by gravity to give a value
 * in g — about 1.0 when still. A fall has a two-part signature: free fall
 * (under 0.4 g, because nothing pushes back on a falling sensor) followed
 * within 1.2 seconds by an impact (over 2.5 g). Requiring both is what
 * separates a real fall from the phone being set down firmly, which produces a
 * single spike on its own.
 *
 * The sensor is registered in onResume and unregistered in onPause, because a
 * registered listener keeps the sensor powered even when the screen is hidden.
 * refresh() in onResume is also why edits made on other screens appear here.
 */
class HomeActivity : AppCompatActivity(), SensorEventListener {

    private companion object {
        // Below this the phone is close to weightless: it is falling.
        const val FREE_FALL_G = 0.4f

        // Above this it has hit something.
        const val IMPACT_G = 2.5f

        // The impact has to follow the free fall within this many milliseconds,
        // otherwise the two readings are unrelated events.
        const val FALL_WINDOW_MS = 1200L

        // Long enough that one fall cannot raise the alert twice.
        const val ALERT_COOLDOWN_MS = 8000L
    }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var doseAdapter: DoseAdapter
    private var profile: PatientProfile = PatientProfile()

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var freeFallAt = 0L
    private var lastAlertAt = 0L
    private var alertShowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        doseAdapter = DoseAdapter(mutableListOf()) { dose, isChecked ->
            onDoseToggled(dose, isChecked)
        }
        binding.recyclerDoses.layoutManager = LinearLayoutManager(this)
        binding.recyclerDoses.adapter = doseAdapter
        binding.recyclerDoses.isNestedScrollingEnabled = false

        binding.buttonMedications.setOnClickListener {
            startActivity(Intent(this, MedicationsActivity::class.java))
        }
        binding.buttonAppointments.setOnClickListener {
            startActivity(Intent(this, AppointmentsActivity::class.java))
        }
        binding.cardNextAppointment.setOnClickListener {
            startActivity(Intent(this, AppointmentsActivity::class.java))
        }
        binding.buttonCareLog.setOnClickListener {
            startActivity(Intent(this, CareLogActivity::class.java))
        }
        binding.cardLatestNote.setOnClickListener {
            startActivity(Intent(this, CareLogActivity::class.java))
        }
        binding.buttonProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.buttonEmergency.setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            binding.textFallHint.text = getString(R.string.no_accelerometer)
        }

        // Dropping a phone to demonstrate the feature is not sensible, so a
        // long press on the live reading runs the same alert the sensor would.
        binding.textFallReading.setOnLongClickListener {
            onFallDetected()
            true
        }
    }

    // Reload every time the screen comes back to the front, so edits made on
    // other screens show up here.
    override fun onResume() {
        super.onResume()
        refresh()
        accelerometer?.let {
            // Faster than SENSOR_DELAY_UI, because the free fall part of a
            // real fall can last only a fraction of a second.
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    // Unregister when the screen is hidden, otherwise the sensor keeps
    // draining battery in the background.
    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    private fun refresh() {
        profile = StorageService.loadProfile(this)
        if (profile.name.isBlank()) {
            binding.textPatient.text = getString(R.string.no_patient)
            binding.textInitials.text = "?"
        } else {
            binding.textPatient.text = profile.name
            binding.textInitials.text = profile.initials()
        }

        val schedule = StorageService.todaySchedule(this)
        updateProgress(schedule)

        doseAdapter.replaceAll(schedule)
        binding.textEmptySchedule.visibility =
            if (schedule.isEmpty()) View.VISIBLE else View.GONE

        val appointment = StorageService.nextAppointment(this)
        if (appointment == null) {
            binding.cardNextAppointment.visibility = View.GONE
        } else {
            binding.cardNextAppointment.visibility = View.VISIBLE
            binding.textNextAppointment.text = appointment.title
            binding.textNextAppointmentWhen.text =
                "${appointment.formattedDate()} at ${appointment.formattedTime()}"
        }

        val note = StorageService.latestNote(this)
        if (note == null) {
            binding.cardLatestNote.visibility = View.GONE
        } else {
            binding.cardLatestNote.visibility = View.VISIBLE
            binding.textLatestNote.text = note.text
            binding.textLatestNoteDate.text = note.formattedDate()
        }
    }

    private fun updateProgress(schedule: List<DoseItem>) {
        val taken = schedule.count { it.taken }
        binding.doseRing.setProgress(taken, schedule.size)
        binding.progressAdherence.progress =
            if (schedule.isEmpty()) 0 else taken * 100 / schedule.size
    }

    private fun onDoseToggled(dose: DoseItem, isChecked: Boolean) {
        StorageService.setDoseTaken(this, dose.medicationId, dose.time, isChecked)
        updateProgress(StorageService.todaySchedule(this))
    }

    // ---------- accelerometer: fall detection ----------

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Length of the acceleration vector, in g. A phone lying still reads
        // about 1.0, because gravity is always pulling on the sensor. In free
        // fall the sensor and its case accelerate together, so it reads near
        // 0. On impact it spikes well above 1.
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()

        if (gForce < FREE_FALL_G) {
            // Remember when the fall started; the impact still has to follow.
            freeFallAt = now
            binding.textFallReading.text =
                String.format("Movement: %.2f g — falling", gForce)
            return
        }

        binding.textFallReading.text = String.format("Movement: %.2f g", gForce)

        val impactAfterFreeFall = gForce > IMPACT_G &&
            freeFallAt > 0L &&
            now - freeFallAt <= FALL_WINDOW_MS

        if (impactAfterFreeFall && now - lastAlertAt > ALERT_COOLDOWN_MS) {
            lastAlertAt = now
            freeFallAt = 0L
            onFallDetected()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this app.
    }

    /**
     * Asks whether the patient is alright and offers a way to act. The app
     * never rings anyone by itself: a dropped phone is far more likely than
     * a dropped person, so the carer stays in charge of what happens next.
     */
    private fun onFallDetected() {
        if (alertShowing) return
        alertShowing = true

        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.fall_title)
            .setMessage(R.string.fall_message)
            .setCancelable(false)
            .setNegativeButton(R.string.fall_all_fine) { dialog, _ -> dialog.dismiss() }
            .setNeutralButton(R.string.open_emergency) { _, _ ->
                startActivity(Intent(this, EmergencyActivity::class.java))
            }

        // Only offer the call button when there is a number to ring.
        if (profile.contact.isNotBlank()) {
            builder.setPositiveButton(R.string.call) { _, _ ->
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.contact}")))
            }
        }

        val dialog = builder.create()
        dialog.setOnDismissListener { alertShowing = false }
        dialog.show()
    }
}
