package com.nevermiss.app.data

import com.nevermiss.app.reminders.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * EventRepository: Single door for all Event operations in the application.
 * Handles validation, Room persistence, and triggers AlarmScheduler.
 */
class EventRepository(
    private val eventDao: EventDao,
    private val alarmScheduler: AlarmScheduler
) {

    /**
     * Observable stream of all events with their cascaded reminders.
     */
    fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAllEventsWithReminders().map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Observable stream of a single event by ID.
     */
    fun getEventById(id: String): Flow<Event?> {
        return eventDao.getEventWithRemindersById(id).map { it?.toDomain() }
    }

    /**
     * Validates and saves an Event, updating Room and scheduling the next alarm.
     */
    suspend fun saveEvent(event: Event): Result<Event> {
        // Validation rules
        if (event.title.trim().isBlank()) {
            return Result.failure(IllegalArgumentException("Event title cannot be empty"))
        }
        if (event.targetDate.isBlank()) {
            return Result.failure(IllegalArgumentException("Target date must be specified"))
        }
        if (event.targetEpochMillis <= 0) {
            return Result.failure(IllegalArgumentException("Invalid target timestamp"))
        }

        // Persist to Room
        val eventEntity = event.toEntity()
        val reminderEntities = event.reminders.map { it.toEntity(event.id) }
        eventDao.upsertEventWithReminders(eventEntity, reminderEntities)

        // Schedule next alarm if active
        if (event.status != EventStatus.COMPLETED) {
            alarmScheduler.scheduleNextAlarmForEvent(event)
        } else {
            alarmScheduler.cancelAlarmsForEvent(event.id)
        }

        return Result.success(event)
    }

    /**
     * Toggles or sets the completion state of an event.
     */
    suspend fun markDone(id: String, isDone: Boolean) {
        val now = System.currentTimeMillis()
        if (isDone) {
            eventDao.updateStatus(id, EventStatus.COMPLETED.name.lowercase(), now)
            alarmScheduler.cancelAlarmsForEvent(id)
        } else {
            eventDao.updateStatus(id, EventStatus.PENDING.name.lowercase(), null)
            val updated = eventDao.getEventWithRemindersByIdSync(id)?.toDomain()
            if (updated != null) {
                alarmScheduler.scheduleNextAlarmForEvent(updated)
            }
        }
    }

    /**
     * Deletes an event and cancels any associated alarm alerts.
     */
    suspend fun deleteEvent(id: String) {
        alarmScheduler.cancelAlarmsForEvent(id)
        eventDao.deleteEventById(id)
    }

    /**
     * Marks a specific reminder alert as delivered.
     */
    suspend fun markReminderDelivered(reminderId: String, eventId: String) {
        eventDao.markReminderDelivered(reminderId)
        val event = eventDao.getEventWithRemindersByIdSync(eventId)?.toDomain()
        if (event != null && event.status != EventStatus.COMPLETED) {
            alarmScheduler.scheduleNextAlarmForEvent(event)
        }
    }
}
