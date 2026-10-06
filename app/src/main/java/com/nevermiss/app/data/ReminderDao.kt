package com.nevermiss.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * ReminderDao: Dedicated Room Data Access Object for reminders table.
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE eventId = :eventId ORDER BY triggerEpochMillis ASC")
    suspend fun getRemindersForEvent(eventId: String): List<ReminderEntity>

    @Query("UPDATE reminders SET isDelivered = 1 WHERE id = :reminderId")
    suspend fun markReminderDelivered(reminderId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteRemindersForEvent(eventId: String)
}
