package com.kinkeeper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.kinkeeper.databinding.ActivityEmergencyBinding
import com.kinkeeper.models.PatientProfile
import com.kinkeeper.services.StorageService

/**
 * Screen 8. Everything a paramedic or relative would need in thirty seconds,
 * with allergies highlighted.
 *
 * Two implicit intents: ACTION_DIAL hands the number to whatever dialler the
 * phone has, and ACTION_SEND shares the card as plain text. ACTION_DIAL rather
 * than ACTION_CALL is deliberate — it needs no permission and the user still
 * presses the call button themselves, which is why this app requests no
 * permissions at all.
 *
 * Sensor 2 of 2: the ambient light sensor. Below 20 lux the card switches to a
 * black background with large text; it only switches back above 30 lux. The
 * gap between the two thresholds is hysteresis — with a single threshold, a
 * reading hovering at the boundary would flip the screen several times a
 * second. Same principle as a thermostat's dead band.
 */
class EmergencyActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityEmergencyBinding
    private lateinit var profile: PatientProfile

    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var highContrast = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.emergency_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        profile = StorageService.loadProfile(this)
        fill()

        binding.buttonCall.setOnClickListener { callContact() }
        binding.buttonShare.setOnClickListener { shareCard() }
        binding.buttonCopy.setOnClickListener { copyCard() }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor == null) {
            binding.textLight.text = "No light sensor on this device."
        }

        // The profile switch can force large text on permanently.
        if (profile.alwaysLargeText) applyContrast(true)
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    private fun fill() {
        binding.textName.text = profile.name.ifBlank { "No name recorded" }

        val age = if (profile.age.isBlank()) "Age not recorded" else "Age ${profile.age}"
        binding.textAgeBlood.text = "$age   ·   Blood group ${profile.bloodGroup}"

        val allergies = profile.allergyList()
        binding.textAllergies.text =
            if (allergies.isEmpty()) getString(R.string.none_recorded)
            else allergies.joinToString("\n") { "• $it" }

        binding.textConditions.text =
            profile.conditions.ifBlank { getString(R.string.none_recorded) }
        binding.textDoctor.text = profile.doctor.ifBlank { getString(R.string.none_recorded) }
        binding.textContact.text = profile.contact.ifBlank { getString(R.string.none_recorded) }
    }

    private fun cardAsText(): String = buildString {
        appendLine("EMERGENCY CARD")
        appendLine(profile.name)
        appendLine("Age ${profile.age}, blood group ${profile.bloodGroup}")
        appendLine()
        appendLine("Allergies: ${profile.allergies.ifBlank { "none recorded" }}")
        appendLine("Conditions: ${profile.conditions.ifBlank { "none recorded" }}")
        appendLine("Doctor: ${profile.doctor.ifBlank { "none recorded" }}")
        appendLine("Emergency contact: ${profile.contact.ifBlank { "none recorded" }}")
    }

    /** Implicit intent: hands the number to whatever dialler the phone has. */
    private fun callContact() {
        if (profile.contact.isBlank()) {
            Snackbar.make(binding.root, R.string.no_number, Snackbar.LENGTH_LONG).show()
            return
        }
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${profile.contact}")))
    }

    /** Implicit intent: lets the user pick any app that accepts plain text. */
    private fun shareCard() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, cardAsText())
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun copyCard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("KinKeeper", cardAsText()))
        Snackbar.make(binding.root, R.string.copied, Snackbar.LENGTH_SHORT).show()
    }

    // ---------- light sensor ----------

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_LIGHT) return

        val lux = event.values[0]
        binding.textLight.text = String.format("Room light: %.0f lx", lux)

        // Below about 20 lux is a dim room. The 30 lux exit point stops the
        // layout flickering when the reading hovers near the boundary.
        val shouldBeHigh = profile.alwaysLargeText || lux < 20f
        val shouldBeLow = !profile.alwaysLargeText && lux > 30f

        if (shouldBeHigh && !highContrast) applyContrast(true)
        else if (shouldBeLow && highContrast) applyContrast(false)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this app.
    }

    private fun applyContrast(high: Boolean) {
        highContrast = high

        val nameSize = if (high) 34f else 26f
        val bodySize = if (high) 22f else 17f

        binding.textName.setTextSize(TypedValue.COMPLEX_UNIT_SP, nameSize)
        listOf(
            binding.textAgeBlood, binding.textAllergies, binding.textConditions,
            binding.textDoctor, binding.textContact
        ).forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, bodySize) }

        if (high) {
            binding.root.setBackgroundColor(Color.BLACK)
            binding.cardBody.setCardBackgroundColor(Color.BLACK)
            binding.textName.setTextColor(Color.WHITE)
            binding.textAgeBlood.setTextColor(Color.WHITE)
            binding.textConditions.setTextColor(Color.WHITE)
            binding.textDoctor.setTextColor(Color.WHITE)
            binding.textContact.setTextColor(Color.WHITE)
            binding.textAllergies.setTextColor(Color.parseColor("#FF6B5B"))
            binding.labelAllergies.setTextColor(Color.parseColor("#FF6B5B"))
            binding.textContrastState.text = "Dim room: large high-contrast text"
            binding.textContrastState.setTextColor(Color.WHITE)
            binding.textLight.setTextColor(Color.WHITE)
        } else {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.page))
            binding.cardBody.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface))
            binding.textName.setTextColor(ContextCompat.getColor(this, R.color.ink))
            binding.textAgeBlood.setTextColor(ContextCompat.getColor(this, R.color.slate))
            binding.textConditions.setTextColor(ContextCompat.getColor(this, R.color.ink))
            binding.textDoctor.setTextColor(ContextCompat.getColor(this, R.color.ink))
            binding.textContact.setTextColor(ContextCompat.getColor(this, R.color.ink))
            binding.textAllergies.setTextColor(ContextCompat.getColor(this, R.color.danger))
            binding.labelAllergies.setTextColor(ContextCompat.getColor(this, R.color.danger))
            binding.textContrastState.text = "Normal light: standard text"
            binding.textContrastState.setTextColor(ContextCompat.getColor(this, R.color.slate))
            binding.textLight.setTextColor(ContextCompat.getColor(this, R.color.slate))
        }

        binding.textContrastState.visibility = View.VISIBLE
    }
}
