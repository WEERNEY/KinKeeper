package com.kinkeeper.models

import org.json.JSONObject
import java.io.Serializable

/**
 * The person being cared for. Everything the emergency card shows comes from
 * here. There is no id because there is exactly one profile per phone.
 *
 * allergyList() splits the typed "a, b, c" into separate entries for the
 * emergency card's bullet list.
 * initials() turns "Akshit Singh" into "AS" for the circle on the home screen.
 */
data class PatientProfile(
    var name: String = "",
    var age: String = "",
    var bloodGroup: String = "Not known",
    var allergies: String = "",
    var conditions: String = "",
    var doctor: String = "",
    var contact: String = "",
    var alwaysLargeText: Boolean = false
) : Serializable {

    fun allergyList(): List<String> =
        allergies.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /** First letters of the first two words, for the circle on the home screen. */
    fun initials(): String {
        val parts = name.trim().split(" ").filter { it.isNotEmpty() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("age", age)
        put("bloodGroup", bloodGroup)
        put("allergies", allergies)
        put("conditions", conditions)
        put("doctor", doctor)
        put("contact", contact)
        put("alwaysLargeText", alwaysLargeText)
    }

    companion object {
        fun fromJson(o: JSONObject) = PatientProfile(
            name = o.optString("name"),
            age = o.optString("age"),
            bloodGroup = o.optString("bloodGroup", "Not known"),
            allergies = o.optString("allergies"),
            conditions = o.optString("conditions"),
            doctor = o.optString("doctor"),
            contact = o.optString("contact"),
            alwaysLargeText = o.optBoolean("alwaysLargeText", false)
        )
    }
}
