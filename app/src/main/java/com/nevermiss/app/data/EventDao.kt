package com.nevermiss.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * EventDao: The database interface providing read/write operations for Events and Reminders.
 */
@Dao
interface EventDao {

    @Transaction
    @Query("SELECT * FROM events ORDER BY targetEpochMillis ASC")
    fun getAllEventsWithReminders(): Flow<List<EventWithReminders>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventWithRemindersById(id: String): Flow<EventWithReminders?>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventWithRemindersByIdSync(id: String): EventWithReminders?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("UPDATE events SET status = :status, completedAtEpochMillis = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, completedAt: Long?)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: String)

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteRemindersForEvent(eventId: String)

    @Query("SELECT * FROM reminders WHERE triggerEpochMillis > :nowEpochMillis AND isDelivered = 0 ORDER BY triggerEpochMillis ASC")
    suspend fun getUpcomingPendingReminders(nowEpochMillis: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE triggerEpochMillis <= :nowEpochMillis AND isDelivered = 0 ORDER BY triggerEpochMillis ASC")
    suspend fun getOverduePendingReminders(nowEpochMillis: Long): List<ReminderEntity>

    @Query("UPDATE reminders SET isDelivered = 1 WHERE id = :reminderId")
    suspend fun markReminderDelivered(reminderId: String)

    @Transaction
    suspend fun upsertEventWithReminders(event: EventEntity, reminders: List<ReminderEntity>) {
        insertEvent(event)
        deleteRemindersForEvent(event.id)
        if (reminders.isNotEmpty()) {
            insertReminders(reminders)
        }
    }

    @Query("SELECT COUNT(*) FROM events")
    suspend fun getCount(): Int
}
