package com.kinkeeper.models

import org.json.JSONObject
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * One appointment: title, place, when it is, and a note.
 *
 * The date and time are stored as a single Long (milliseconds since 1970)
 * rather than separate fields. That makes sorting and the isPast() check one
 * comparison instead of a chain of conditions.
 *
 * formattedDate() and formattedTime() live here so the list, the form and the
 * home screen card all display it the same way.
 */
data class Appointment(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var location: String = "",
    var whenMillis: Long = System.currentTimeMillis(),
    var notes: String = ""
) : Serializable {

    fun formattedDate(): String =
        SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault()).format(Date(whenMillis))

    fun formattedTime(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(whenMillis))

    fun isPast(): Boolean = whenMillis < System.currentTimeMillis()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("location", location)
        put("whenMillis", whenMillis)
        put("notes", notes)
    }

    companion object {
        fun fromJson(o: JSONObject) = Appointment(
            id = o.optString("id", UUID.randomUUID().toString()),
            title = o.optString("title"),
            location = o.optString("location"),
            whenMillis = o.optLong("whenMillis", System.currentTimeMillis()),
            notes = o.optString("notes")
        )
    }
}
