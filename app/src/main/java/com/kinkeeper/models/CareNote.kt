package com.kinkeeper.models

import org.json.JSONObject
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * One dated observation about the patient, for the care log.
 *
 * createdAt defaults to the current time, so writing CareNote(text = "...")
 * stamps itself automatically — no code anywhere sets the timestamp.
 */
data class CareNote(
    val id: String = UUID.randomUUID().toString(),
    var text: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {

    fun formattedDate(): String =
        SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(createdAt))

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(o: JSONObject) = CareNote(
            id = o.optString("id", UUID.randomUUID().toString()),
            text = o.optString("text"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
