package com.nevermiss.app.data

import androidx.room.TypeConverter

/**
 * EventConverters: Room type converters for storing reminder offset lists as comma-separated text.
 */
class EventConverters {

    @TypeConverter
    fun fromReminderMinutes(minutes: List<Int>?): String {
        return minutes?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toReminderMinutes(data: String?): List<Int> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
    }
}
