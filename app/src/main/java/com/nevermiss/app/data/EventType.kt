package com.nevermiss.app.data

/**
 * EventType: The 5 official delivered event types in the NeverMiss Solution specification.
 */
enum class EventType(val iconKey: String) {
    BIRTHDAY("cake"),
    EXAM("school"),
    ASSIGNMENT("book"),
    MEETING("people"),
    OTHER("event");

    companion object {
        fun fromString(value: String): EventType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}
