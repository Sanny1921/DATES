package com.nevermiss.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

/**
 * EventEntity: Persistent Room database table for scheduled deadlines, events, and milestones.
 */
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val targetDate: String, // ISO date string e.g. "2026-10-15"
    val targetTime: String, // 24-hr time e.g. "23:59"
    val targetEpochMillis: Long,
    val priority: String, // "low", "medium", "high", "urgent"
    val notes: String = "",
    val status: String = "pending", // "pending", "attention", "completed"
    val completedAtEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

/**
 * ReminderEntity: Persistent Room database table for multi-stage trigger points attached to an Event.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["eventId"]),
        Index(value = ["triggerEpochMillis"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val eventId: String,
    val triggerEpochMillis: Long,
    val label: String, // e.g. "10 Days Before", "1 Hour Before"
    val daysBefore: Int,
    val hoursBefore: Int,
    val minutesBefore: Int,
    val exactTime: String? = null,
    val note: String = "",
    val severity: String = "normal", // "normal", "warn", "critical"
    val isDelivered: Boolean = false
)

/**
 * Room Relation data class uniting an Event with its cascaded child Reminders.
 */
data class EventWithReminders(
    @Embedded
    val event: EventEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "eventId"
    )
    val reminders: List<ReminderEntity> = emptyList()
)
