package com.kinkeeper.services

import android.content.Context
import android.content.SharedPreferences
import com.kinkeeper.models.Appointment
import com.kinkeeper.models.CareNote
import com.kinkeeper.models.DoseItem
import com.kinkeeper.models.Medication
import com.kinkeeper.models.PatientProfile
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The only file in the app that reads or writes SharedPreferences. Every
 * screen goes through here and none of them touch storage directly, so
 * swapping SharedPreferences for a database would change this file alone.
 *
 * Declared as an 'object', which in Kotlin means a singleton created
 * automatically — there is one storage area, so one thing manages it.
 *
 * Each collection is stored as a JSON array of text under its own key.
 *
 * Two things worth noting:
 *  - Ticked doses are saved together with today's date. If that date is not
 *    today's, the set reads as empty, so ticks expire by being ignored rather
 *    than through any timer or background job.
 *  - todaySchedule() is computed on demand, never stored, so editing a
 *    medicine cannot leave a stale schedule behind.
 */
object StorageService {

    private const val PREFS = "kinkeeper_prefs"
    private const val KEY_MEDICATIONS = "medications"
    private const val KEY_PROFILE = "profile"
    private const val KEY_TAKEN = "taken_doses"
    private const val KEY_NOTES = "care_notes"
    private const val KEY_APPOINTMENTS = "appointments"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ---------- medicines ----------

    fun loadMedications(context: Context): MutableList<Medication> {
        val raw = prefs(context).getString(KEY_MEDICATIONS, null) ?: return mutableListOf()
        val list = mutableListOf<Medication>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) list.add(Medication.fromJson(arr.getJSONObject(i)))
        } catch (e: Exception) {
            return mutableListOf()
        }
        list.sortBy { it.name.lowercase(Locale.getDefault()) }
        return list
    }

    fun saveMedications(context: Context, medications: List<Medication>) {
        val arr = JSONArray()
        medications.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(KEY_MEDICATIONS, arr.toString()).apply()
    }

    /** Replaces the medicine with the same id, or appends it if it is new. */
    fun saveMedication(context: Context, medication: Medication) {
        val list = loadMedications(context)
        val index = list.indexOfFirst { it.id == medication.id }
        if (index >= 0) list[index] = medication else list.add(medication)
        saveMedications(context, list)
    }

    fun deleteMedication(context: Context, id: String) {
        val list = loadMedications(context)
        list.removeAll { it.id == id }
        saveMedications(context, list)
    }

    // ---------- profile ----------

    fun loadProfile(context: Context): PatientProfile {
        val raw = prefs(context).getString(KEY_PROFILE, null) ?: return PatientProfile()
        return try {
            PatientProfile.fromJson(JSONObject(raw))
        } catch (e: Exception) {
            PatientProfile()
        }
    }

    fun saveProfile(context: Context, profile: PatientProfile) {
        prefs(context).edit().putString(KEY_PROFILE, profile.toJson().toString()).apply()
    }

    // ---------- care notes ----------

    /** Newest note first, which is the order the care log shows them in. */
    fun loadNotes(context: Context): MutableList<CareNote> {
        val raw = prefs(context).getString(KEY_NOTES, null) ?: return mutableListOf()
        val list = mutableListOf<CareNote>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) list.add(CareNote.fromJson(arr.getJSONObject(i)))
        } catch (e: Exception) {
            return mutableListOf()
        }
        list.sortByDescending { it.createdAt }
        return list
    }

    private fun saveNotes(context: Context, notes: List<CareNote>) {
        val arr = JSONArray()
        notes.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(KEY_NOTES, arr.toString()).apply()
    }

    fun addNote(context: Context, note: CareNote) {
        val list = loadNotes(context)
        list.add(0, note)
        saveNotes(context, list)
    }

    fun deleteNote(context: Context, id: String) {
        val list = loadNotes(context)
        list.removeAll { it.id == id }
        saveNotes(context, list)
    }

    fun latestNote(context: Context): CareNote? = loadNotes(context).firstOrNull()

    // ---------- appointments ----------

    /** Soonest first, so the top of the list is what happens next. */
    fun loadAppointments(context: Context): MutableList<Appointment> {
        val raw = prefs(context).getString(KEY_APPOINTMENTS, null) ?: return mutableListOf()
        val list = mutableListOf<Appointment>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) list.add(Appointment.fromJson(arr.getJSONObject(i)))
        } catch (e: Exception) {
            return mutableListOf()
        }
        list.sortBy { it.whenMillis }
        return list
    }

    fun saveAppointments(context: Context, appointments: List<Appointment>) {
        val arr = JSONArray()
        appointments.forEach { arr.put(it.toJson()) }
        prefs(context).edit().putString(KEY_APPOINTMENTS, arr.toString()).apply()
    }

    /** Replaces the appointment with the same id, or appends it if it is new. */
    fun saveAppointment(context: Context, appointment: Appointment) {
        val list = loadAppointments(context)
        val index = list.indexOfFirst { it.id == appointment.id }
        if (index >= 0) list[index] = appointment else list.add(appointment)
        saveAppointments(context, list)
    }

    fun deleteAppointment(context: Context, id: String) {
        val list = loadAppointments(context)
        list.removeAll { it.id == id }
        saveAppointments(context, list)
    }

    /** The next appointment still in the future, or null if there is none. */
    fun nextAppointment(context: Context): Appointment? =
        loadAppointments(context).firstOrNull { !it.isPast() }

    // ---------- which doses were taken today ----------

    private fun takenKeys(context: Context): MutableSet<String> {
        val raw = prefs(context).getString(KEY_TAKEN, null) ?: return mutableSetOf()
        return try {
            val o = JSONObject(raw)
            // A new day wipes the list clean.
            if (o.optString("date") != today()) return mutableSetOf()
            val arr = o.optJSONArray("keys") ?: JSONArray()
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) set.add(arr.getString(i))
            set
        } catch (e: Exception) {
            mutableSetOf()
        }
    }

    private fun writeTakenKeys(context: Context, keys: Set<String>) {
        val arr = JSONArray()
        keys.forEach { arr.put(it) }
        val o = JSONObject().put("date", today()).put("keys", arr)
        prefs(context).edit().putString(KEY_TAKEN, o.toString()).apply()
    }

    fun setDoseTaken(context: Context, medicationId: String, time: String, taken: Boolean) {
        val keys = takenKeys(context)
        val key = "$medicationId@$time"
        if (taken) keys.add(key) else keys.remove(key)
        writeTakenKeys(context, keys)
    }

    /** Flattens every medicine and every one of its times into one list sorted by clock time. */
    fun todaySchedule(context: Context): MutableList<DoseItem> {
        val taken = takenKeys(context)
        val doses = mutableListOf<DoseItem>()
        loadMedications(context).forEach { med ->
            med.times.forEach { time ->
                doses.add(
                    DoseItem(
                        medicationId = med.id,
                        medicationName = med.name,
                        dose = med.dose,
                        time = time,
                        taken = taken.contains("${med.id}@$time")
                    )
                )
            }
        }
        doses.sortBy { it.time }
        return doses
    }
}
