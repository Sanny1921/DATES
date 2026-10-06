package com.nevermiss.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

import androidx.room.TypeConverters

/**
 * EventDatabase: Local persistent SQLite storage via Android Jetpack Room.
 * Handles database creation and default seed events.
 */
@Database(
    entities = [EventEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(EventConverters::class)
abstract class EventDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: EventDatabase? = null

        /**
         * Optional listener invoked when initial seed events have been inserted for the first time.
         * Keeping this decoupled prevents circular architectural dependencies between EventDatabase and AppGraph.
         */
        @Volatile
        var onSeedComplete: (() -> Unit)? = null

        fun getInstance(context: Context): EventDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    "nevermiss_offline_events.db"
                )
                    .addCallback(DatabaseSeedCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseSeedCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val dbInstance = getInstance(context)
                val seeded = dbInstance.seedInitialEvents()
                if (seeded) {
                    onSeedComplete?.invoke()
                }
            }
        }
    }

    suspend fun seedInitialEvents(): Boolean {
        val dao = eventDao()
        if (dao.getCount() > 0) return false

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        // Seed item 1: Electricity Bill (Due in 3 days, Urgent)
        val billDate = today.plusDays(3)
        val billTime = LocalTime.of(23, 59)
        val billEpoch = billDate.atTime(billTime).atZone(zone).toInstant().toEpochMilli()

        val billEntity = EventEntity(
            id = "item-electricity-bill",
            title = "Electricity Bill",
            category = "Payment",
            targetDate = billDate.toString(),
            targetTime = "23:59",
            targetEpochMillis = billEpoch,
            priority = "urgent",
            notes = "Power & Utility Account #4928-11. Pay before 11:59 PM to avoid disconnection penalty.",
            status = "attention"
        )
        val billReminders = listOf(
            ReminderEntity(
                id = "rem-bill-1",
                eventId = "item-electricity-bill",
                triggerEpochMillis = billEpoch - (10L * 24 * 3600 * 1000),
                label = "10 Days Before",
                daysBefore = 10,
                hoursBefore = 0,
                minutesBefore = 0,
                exactTime = "10:00",
                note = "Early planning brief & logistics ping",
                severity = "normal"
            ),
            ReminderEntity(
                id = "rem-bill-2",
                eventId = "item-electricity-bill",
                triggerEpochMillis = billEpoch - (1L * 24 * 3600 * 1000),
                label = "1 Day Before",
                daysBefore = 1,
                hoursBefore = 0,
                minutesBefore = 0,
                exactTime = "20:00",
                note = "Urgent final payment verification alert",
                severity = "warn"
            ),
            ReminderEntity(
                id = "rem-bill-3",
                eventId = "item-electricity-bill",
                triggerEpochMillis = billEpoch - (1L * 3600 * 1000),
                label = "1 Hour Before (Imminent)",
                daysBefore = 0,
                hoursBefore = 1,
                minutesBefore = 0,
                exactTime = "22:59",
                note = "High-priority alarm: pay bill now",
                severity = "critical"
            )
        )
        dao.upsertEventWithReminders(billEntity, billReminders)

        // Seed item 2: Annual Health Screening (Due Today)
        val healthEpoch = today.atTime(14, 30).atZone(zone).toInstant().toEpochMilli()
        val healthEntity = EventEntity(
            id = "item-health-checkup",
            title = "Annual Health Screening",
            category = "Health",
            targetDate = today.toString(),
            targetTime = "14:30",
            targetEpochMillis = healthEpoch,
            priority = "high",
            notes = "Fasting required 8 hours prior. Bring previous lab results.",
            status = "pending"
        )
        val healthReminders = listOf(
            ReminderEntity(
                id = "rem-health-1",
                eventId = "item-health-checkup",
                triggerEpochMillis = healthEpoch - (2L * 3600 * 1000),
                label = "2 Hours Before",
                daysBefore = 0,
                hoursBefore = 2,
                minutesBefore = 0,
                note = "Traffic buffer departure alert",
                severity = "warn"
            )
        )
        dao.upsertEventWithReminders(healthEntity, healthReminders)

        // Seed item 3: Mom's 60th Birthday Celebration (Due in 6 days)
        val birthdayDate = today.plusDays(6)
        val birthdayEpoch = birthdayDate.atTime(19, 0).atZone(zone).toInstant().toEpochMilli()
        val birthdayEntity = EventEntity(
            id = "item-moms-birthday",
            title = "Mom's 60th Birthday Celebration",
            category = "Birthday",
            targetDate = birthdayDate.toString(),
            targetTime = "19:00",
            targetEpochMillis = birthdayEpoch,
            priority = "high",
            notes = "Dinner reservation confirmed at Osteria. Flower delivery arrives at 3 PM.",
            status = "pending"
        )
        dao.upsertEventWithReminders(birthdayEntity, emptyList())

        // Seed item 4: Completed Item (Past milestone)
        val pastDate = today.minusDays(2)
        val pastEpoch = pastDate.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val completedEntity = EventEntity(
            id = "item-cloud-renewal",
            title = "Cloud Server Subscription Renewal",
            category = "Work",
            targetDate = pastDate.toString(),
            targetTime = "10:00",
            targetEpochMillis = pastEpoch,
            priority = "medium",
            notes = "Verified corporate invoice and transaction receipt.",
            status = "completed",
            completedAtEpochMillis = pastEpoch + 3600000
        )
        dao.upsertEventWithReminders(completedEntity, emptyList())
        return true
    }
}
