package com.nevermiss.app.data

/**
 * Domain Event model used cleanly by the Compose UI and business logic.
 */
data class Event(
    val id: String,
    val title: String,
    val category: Category,
    val targetDate: String, // YYYY-MM-DD
    val targetTime: String, // HH:mm
    val targetEpochMillis: Long,
    val priority: Priority,
    val notes: String = "",
    val status: EventStatus = EventStatus.PENDING,
    val completedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val reminders: List<ReminderAlert> = emptyList()
)

data class ReminderAlert(
    val id: String,
    val eventId: String = "",
    val triggerEpochMillis: Long,
    val label: String,
    val daysBefore: Int,
    val hoursBefore: Int,
    val minutesBefore: Int,
    val exactTime: String? = null,
    val note: String = "",
    val severity: Severity = Severity.NORMAL,
    val isDelivered: Boolean = false
)

enum class Category(val displayName: String, val iconTag: String) {
    Payment("Payment", "receipt"),
    Birthday("Birthday", "cake"),
    Anniversary("Anniversary", "favorite"),
    Health("Health", "health"),
    Work("Work", "work"),
    Travel("Travel", "flight"),
    Renewal("Renewal", "autorenew"),
    Education("Education", "school"),
    Event("Event", "event"),
    Personal("Personal", "person"),
    Other("Other", "tag");

    companion object {
        fun fromString(value: String): Category {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Other
        }
    }
}

enum class Priority(val displayName: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    companion object {
        fun fromString(value: String): Priority {
            return when (value.lowercase()) {
                "low" -> LOW
                "medium" -> MEDIUM
                "high" -> HIGH
                "urgent", "critical" -> URGENT
                else -> MEDIUM
            }
        }
    }
}

enum class EventStatus {
    PENDING,
    ATTENTION,
    COMPLETED;

    companion object {
        fun fromString(value: String): EventStatus {
            return when (value.lowercase()) {
                "completed" -> COMPLETED
                "attention" -> ATTENTION
                else -> PENDING
            }
        }
    }
}

enum class Severity {
    NORMAL,
    WARN,
    CRITICAL;

    companion object {
        fun fromString(value: String): Severity {
            return when (value.lowercase()) {
                "critical" -> CRITICAL
                "warn", "warning" -> WARN
                else -> NORMAL
            }
        }
    }
}

// Extension mappers between Domain & Entities
fun EventWithReminders.toDomain(): Event {
    return Event(
        id = event.id,
        title = event.title,
        category = Category.fromString(event.category),
        targetDate = event.targetDate,
        targetTime = event.targetTime,
        targetEpochMillis = event.targetEpochMillis,
        priority = Priority.fromString(event.priority),
        notes = event.notes,
        status = EventStatus.fromString(event.status),
        completedAtEpochMillis = event.completedAtEpochMillis,
        createdAtEpochMillis = event.createdAtEpochMillis,
        reminders = reminders.map { it.toDomain() }
    )
}

fun ReminderEntity.toDomain(): ReminderAlert {
    return ReminderAlert(
        id = id,
        eventId = eventId,
        triggerEpochMillis = triggerEpochMillis,
        label = label,
        daysBefore = daysBefore,
        hoursBefore = hoursBefore,
        minutesBefore = minutesBefore,
        exactTime = exactTime,
        note = note,
        severity = Severity.fromString(severity),
        isDelivered = isDelivered
    )
}

fun Event.toEntity(): EventEntity {
    return EventEntity(
        id = id,
        title = title,
        category = category.name,
        targetDate = targetDate,
        targetTime = targetTime,
        targetEpochMillis = targetEpochMillis,
        priority = priority.name.lowercase(),
        notes = notes,
        status = status.name.lowercase(),
        completedAtEpochMillis = completedAtEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis
    )
}

fun ReminderAlert.toEntity(parentEventId: String): ReminderEntity {
    return ReminderEntity(
        id = id,
        eventId = if (eventId.isNotEmpty()) eventId else parentEventId,
        triggerEpochMillis = triggerEpochMillis,
        label = label,
        daysBefore = daysBefore,
        hoursBefore = hoursBefore,
        minutesBefore = minutesBefore,
        exactTime = exactTime,
        note = note,
        severity = severity.name.lowercase(),
        isDelivered = isDelivered
    )
}
