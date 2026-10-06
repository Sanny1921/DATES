package com.nevermiss.app.data

import com.nevermiss.app.reminders.AlarmMode

/**
 * SaveResult: Result returned when saving/inserting an event, containing the scheduled alarm mode.
 */
data class SaveResult(
    val alarmMode: AlarmMode,
    val event: Event? = null
)
