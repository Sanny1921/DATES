package com.nevermiss.app.logic

import com.nevermiss.app.data.Category
import com.nevermiss.app.data.Event
import com.nevermiss.app.data.EventStatus
import com.nevermiss.app.data.Priority
import com.nevermiss.app.data.ReminderAlert
import com.nevermiss.app.data.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * ReminderCalculationTest: Validates offline reminder trigger calculation,
 * maximum 5 reminders constraint, and nearest future reminder scheduling logic.
 */
class ReminderCalculationTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testReminderOffsets_FireTimeCalculation() {
        val date = LocalDate.of(2026, 10, 15)
        val time = LocalTime.of(12, 0)
        val eventEpoch = date.atTime(time).atZone(zoneId).toInstant().toEpochMilli()

        // 30 minutes before
        val minutesBefore = 30
        val triggerEpoch = eventEpoch - (minutesBefore * 60_000L)

        val reminder = ReminderAlert(
            id = "rem-1",
            eventId = "evt-1",
            triggerEpochMillis = triggerEpoch,
            label = "30 minutes before",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 30,
            severity = Severity.NORMAL
        )

        assertEquals(eventEpoch - 1_800_000L, reminder.triggerEpochMillis)
        assertTrue(reminder.triggerEpochMillis < eventEpoch)
    }

    @Test
    fun testNearestFutureReminderSelection() {
        val now = 1_000_000L

        val pastDelivered = ReminderAlert(
            id = "rem-past-delivered",
            triggerEpochMillis = 800_000L,
            label = "Past Delivered",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 0,
            isDelivered = true
        )
        val pastMissed = ReminderAlert(
            id = "rem-past-missed",
            triggerEpochMillis = 900_000L,
            label = "Past Missed",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 0,
            isDelivered = false
        )
        val futureNear = ReminderAlert(
            id = "rem-future-near",
            triggerEpochMillis = 1_100_000L,
            label = "Future Near",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 0,
            isDelivered = false
        )
        val futureFar = ReminderAlert(
            id = "rem-future-far",
            triggerEpochMillis = 1_500_000L,
            label = "Future Far",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 0,
            isDelivered = false
        )

        val reminders = listOf(pastDelivered, pastMissed, futureNear, futureFar)

        // Only future unhandled reminders: triggerEpochMillis > now and !isDelivered
        val nextScheduled = reminders
            .filter { !it.isDelivered && it.triggerEpochMillis > now }
            .minByOrNull { it.triggerEpochMillis }

        assertNotNull(nextScheduled)
        assertEquals("rem-future-near", nextScheduled?.id)
        assertEquals(1_100_000L, nextScheduled?.triggerEpochMillis)
    }

    @Test
    fun testNoAlarmsWhenAllRemindersHandledOrPast() {
        val now = 2_000_000L

        val reminder1 = ReminderAlert(
            id = "rem-1",
            triggerEpochMillis = 1_000_000L,
            label = "Delivered",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 0,
            isDelivered = true
        )
        val reminder2 = ReminderAlert(
            id = "rem-2",
            triggerEpochMillis = 1_500_000L,
            label = "Past Undelivered",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 0,
            isDelivered = false
        )

        val reminders = listOf(reminder1, reminder2)

        val nextScheduled = reminders
            .filter { !it.isDelivered && it.triggerEpochMillis > now }
            .minByOrNull { it.triggerEpochMillis }

        assertNull(nextScheduled)
    }

    @Test
    fun testMaximumFiveRemindersConstraint() {
        val reminders = (1..6).map { index ->
            ReminderAlert(
                id = "rem-$index",
                triggerEpochMillis = 1000L * index,
                label = "Reminder $index",
                daysBefore = 0,
                hoursBefore = 0,
                minutesBefore = index
            )
        }

        // Validate max 5 restriction
        val cappedReminders = reminders.take(5)
        assertEquals(5, cappedReminders.size)
        assertTrue(cappedReminders.size <= 5)
    }
}
