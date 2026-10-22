package com.kinkeeper.models

/**
 * One medicine at one clock time — a single row of today's schedule.
 *
 * The only model that is never saved and never travels in an Intent, which is
 * why it has no toJson and no Serializable. StorageService.todaySchedule()
 * builds these fresh each time from the medicines and their times.
 */
data class DoseItem(
    val medicationId: String,
    val medicationName: String,
    val dose: String,
    val time: String,
    var taken: Boolean
)
