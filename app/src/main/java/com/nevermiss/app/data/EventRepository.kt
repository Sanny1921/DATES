package com.nevermiss.app.data

import com.nevermiss.app.logic.EventBuckets
import com.nevermiss.app.logic.EventBucketingLogic
import com.nevermiss.app.reminders.AlarmMode
import com.nevermiss.app.reminders.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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
     * Observable stream of events grouped into Overdue, Today, Upcoming, and Completed buckets.
     */
    fun homeBuckets(): Flow<EventBuckets> {
        return getAllEvents().map { events ->
            EventBucketingLogic.bucketEvents(events)
        }
    }

    /**
     * Returns events occurring within a timestamp window [from, until).
     */
    fun eventsBetween(from: Long, until: Long): Flow<List<Event>> {
        return getAllEvents().map { events ->
            events.filter { it.targetEpochMillis in from until until }
        }
    }

    /**
     * Returns events occurring on a specific local date.
     */
    fun eventsOn(date: LocalDate): Flow<List<Event>> {
        val dateString = date.toString()
        return getAllEvents().map { events ->
            events.filter { it.targetDate == dateString }
        }
    }

    /**
     * Validates and saves an Event, updating Room and scheduling the next alarm.
     * Enforces all NeverMiss specification rules:
     * - Title 1 to 80 chars
     * - Max 5 reminders
     * - Unique reminder offsets in [0, 5256000] minutes
     * - Reminders saved sorted largest to smallest
     */
    suspend fun saveEvent(event: Event): Result<Event> {
        // Validation rules
        val trimmedTitle = event.title.trim()
        if (trimmedTitle.isBlank()) {
            return Result.failure(IllegalArgumentException("Event title cannot be empty"))
        }
        if (trimmedTitle.length > 80) {
            return Result.failure(IllegalArgumentException("Event title cannot exceed 80 characters"))
        }
        if (event.targetDate.isBlank()) {
            return Result.failure(IllegalArgumentException("Target date must be specified"))
        }
        if (event.targetEpochMillis <= 0) {
            return Result.failure(IllegalArgumentException("Invalid target timestamp"))
        }

        // Reminders validation
        if (event.reminders.size > 5) {
            return Result.failure(IllegalArgumentException("Maximum 5 reminders per event"))
        }

        val offsets = event.reminders.map { it.minutesBefore }
        if (offsets.distinct().size != offsets.size) {
            return Result.failure(IllegalArgumentException("Duplicate reminder offsets are not allowed"))
        }
        for (offset in offsets) {
            if (offset < 0 || offset > 5_256_000) {
                return Result.failure(IllegalArgumentException("Reminder offset must be between 0 and 5,256,000 minutes"))
            }
        }

        // Check future requirement for newly created events
        val existing = eventDao.getEventWithRemindersByIdSync(event.id)
        val now = System.currentTimeMillis()
        if (existing == null && event.targetEpochMillis < now) {
            return Result.failure(IllegalArgumentException("New event must be in the future"))
        }

        // Sort reminders largest to smallest according to specification
        val sortedReminders = event.reminders.sortedByDescending { it.minutesBefore }
        val normalizedEvent = event.copy(title = trimmedTitle, reminders = sortedReminders)

        // Persist to Room
        val eventEntity = normalizedEvent.toEntity()
        val reminderEntities = normalizedEvent.reminders.map { it.toEntity(normalizedEvent.id) }
        eventDao.upsertEventWithReminders(eventEntity, reminderEntities)

        // Schedule next alarm if active
        if (normalizedEvent.status != EventStatus.COMPLETED) {
            alarmScheduler.scheduleNextAlarmForEvent(normalizedEvent)
        } else {
            alarmScheduler.cancelAlarmsForEvent(normalizedEvent.id)
        }

        return Result.success(normalizedEvent)
    }

    /**
     * Inserts an event and returns a SaveResult with the alarm scheduling mode.
     */
    suspend fun insert(event: Event): SaveResult {
        val result = saveEvent(event)
        val saved = result.getOrNull()
        val mode = if (saved != null && saved.status != EventStatus.COMPLETED) {
            alarmScheduler.scheduleNextAlarmForEvent(saved)
        } else {
            AlarmMode.NONE
        }
        return SaveResult(alarmMode = mode, event = saved)
    }

    /**
     * Updates an event and returns a SaveResult with the alarm scheduling mode.
     */
    suspend fun update(event: Event): SaveResult {
        return insert(event)
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

    /**
     * Reschedules next upcoming alarms for all active events.
     * Used after initial seed insertion, device reboot, app updates, or timezone changes.
     */
    suspend fun restoreFutureAlarms() {
        val events = getAllEvents().first()
        alarmScheduler.rescheduleAll(events)
    }

    // Repository specification method aliases
    fun observeAll(): Flow<List<Event>> = getAllEvents()
    fun getById(id: String): Flow<Event?> = getEventById(id)
    suspend fun setDone(id: String, done: Boolean) = markDone(id, done)
    suspend fun delete(id: String) = deleteEvent(id)
}
