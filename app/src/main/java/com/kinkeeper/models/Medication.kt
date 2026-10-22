package com.kinkeeper.models

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable
import java.util.UUID

/**
 * One medicine the patient takes: name, dose, the times it is due each day,
 * and a note for the carer.
 *
 * Implements Serializable so a whole Medication can be put inside an Intent.
 * That is how MedicationsActivity hands one to the form and gets it back.
 *
 * toJson/fromJson convert it to and from text, because SharedPreferences can
 * only store simple values, not objects.
 *
 * The id is a val, generated once: it is how StorageService knows whether to
 * replace an existing medicine or append a new one.
 */
data class Medication(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var dose: String = "",
    var times: ArrayList<String> = arrayListOf(),
    var notes: String = ""
) : Serializable {

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("name", name)
        o.put("dose", dose)
        o.put("notes", notes)
        val arr = JSONArray()
        times.forEach { arr.put(it) }
        o.put("times", arr)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Medication {
            val list = ArrayList<String>()
            val arr = o.optJSONArray("times") ?: JSONArray()
            for (i in 0 until arr.length()) list.add(arr.getString(i))
            list.sort()
            return Medication(
                id = o.optString("id", UUID.randomUUID().toString()),
                name = o.optString("name"),
                dose = o.optString("dose"),
                times = list,
                notes = o.optString("notes")
            )
        }
    }
}
