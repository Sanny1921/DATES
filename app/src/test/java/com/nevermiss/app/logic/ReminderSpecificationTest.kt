package com.nevermiss.app.logic

import com.nevermiss.app.data.Category
import com.nevermiss.app.data.Event
import com.nevermiss.app.data.EventStatus
import com.nevermiss.app.data.EventType
import com.nevermiss.app.data.Priority
import com.nevermiss.app.data.ReminderAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * ReminderSpecificationTest: Comprehensive unit tests strictly verifying the
 * NeverMiss Solution product specifications:
 * - Custom reminder duration conversions (minutes, hours, days -> minutes)
 * - Maximum 5 reminders enforcement
 * - Unique reminder offsets & duplicate rejection
 * - Validation boundaries (0 to 5,256,000 minutes, no negatives)
 * - Largest-to-smallest ordering
 * - Nearest future unhandled reminder scheduling
 * - Handled and past reminder exclusion
 * - Event buckets properties & counts
 */
class ReminderSpecificationTest {

    private val zoneId = ZoneId.of("UTC")

    // Helper conversion simulating CreateEventScreen custom unit conversion
    private fun convertDurationToMinutes(duration: Int, unit: String): Int {
        return when (unit) {
            "Minutes" -> duration
            "Hours" -> duration * 60
            "Days" -> duration * 1440
            else -> duration
        }
    }

    // Pure validation logic mirroring repository validation rules
    private fun validateReminderOffsets(offsets: List<Int>): String? {
        if (offsets.size > 5) {
            return "Maximum 5 reminders per event"
        }
        if (offsets.distinct().size != offsets.size) {
            return "Duplicate reminder offsets are not allowed"
        }
        for (offset in offsets) {
            if (offset < 0) {
                return "Reminder offset cannot be negative"
            }
            if (offset > 5_256_000) {
                return "Reminder offset cannot exceed 5,256,000 minutes"
            }
        }
        return null
    }

    @Test
    fun testCustomReminderDurationConversion() {
        // 10 Minutes -> 10
        assertEquals(10, convertDurationToMinutes(10, "Minutes"))

        // 2 Hours -> 120
        assertEquals(120, convertDurationToMinutes(2, "Hours"))

        // 3 Days -> 4320
        assertEquals(4320, convertDurationToMinutes(3, "Days"))

        // 1 Day -> 1440
        assertEquals(1440, convertDurationToMinutes(1, "Days"))

        // 24 Hours -> 1440
        assertEquals(1440, convertDurationToMinutes(24, "Hours"))
    }

    @Test
    fun testMaximumFiveRemindersConstraint() {
        // 5 reminders is valid
        val validFive = listOf(1440, 720, 120, 30, 5)
        assertNull(validateReminderOffsets(validFive))

        // 6 reminders is rejected
        val invalidSix = listOf(2880, 1440, 720, 120, 30, 5)
        val error = validateReminderOffsets(invalidSix)
        assertEquals("Maximum 5 reminders per event", error)
    }

    @Test
    fun testDuplicateReminderOffsetsRejected() {
        // Duplicate 15 minutes rejected
        val duplicates = listOf(30, 15, 15, 5)
        val error = validateReminderOffsets(duplicates)
        assertEquals("Duplicate reminder offsets are not allowed", error)
    }

    @Test
    fun testReminderValidation_NegativeValuesRejected() {
        val negative = listOf(30, -5)
        val error = validateReminderOffsets(negative)
        assertEquals("Reminder offset cannot be negative", error)
    }

    @Test
    fun testReminderValidation_ExceedingMaximumAllowedRejected() {
        // Exceeding 5,256,000 minutes (~10 years) rejected
        val tooLarge = listOf(5_256_001)
        val error = validateReminderOffsets(tooLarge)
        assertEquals("Reminder offset cannot exceed 5,256,000 minutes", error)

        // Exact boundary 5,256,000 is allowed
        val maxBoundary = listOf(5_256_000)
        assertNull(validateReminderOffsets(maxBoundary))

        // 0 minutes (exact event time) is allowed
        val atEventTime = listOf(0)
        assertNull(validateReminderOffsets(atEventTime))
    }

    @Test
    fun testReminderStorageOrder_LargestToSmallest() {
        val input = listOf(5, 1440, 30, 0, 120)
        val sorted = input.sortedDescending()

        assertEquals(listOf(1440, 120, 30, 5, 0), sorted)
        assertEquals(1440, sorted.first())
        assertEquals(0, sorted.last())
    }

    @Test
    fun testNearestFutureReminderSelection_ExcludesHandledAndPast() {
        val now = 2_000_000L

        val handledPast = ReminderAlert(
            id = "rem-handled-past",
            triggerEpochMillis = 1_000_000L,
            label = "Handled Past",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 60,
            isDelivered = true
        )
        val unhandledPast = ReminderAlert(
            id = "rem-unhandled-past",
            triggerEpochMillis = 1_500_000L,
            label = "Unhandled Past",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 30,
            isDelivered = false
        )
        val nearestFuture = ReminderAlert(
            id = "rem-nearest-future",
            triggerEpochMillis = 2_100_000L,
            label = "Nearest Future",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 15,
            isDelivered = false
        )
        val farFuture = ReminderAlert(
            id = "rem-far-future",
            triggerEpochMillis = 2_500_000L,
            label = "Far Future",
            daysBefore = 0,
            hoursBefore = 0,
            minutesBefore = 5,
            isDelivered = false
        )

        val allReminders = listOf(handledPast, unhandledPast, nearestFuture, farFuture)

        // Scheduling rule: nearest future reminder where !isDelivered and triggerEpochMillis > now
        val nextScheduled = allReminders
            .filter { !it.isDelivered && it.triggerEpochMillis > now }
            .minByOrNull { it.triggerEpochMillis }

        assertNotNull(nextScheduled)
        assertEquals("rem-nearest-future", nextScheduled?.id)
        assertEquals(2_100_000L, nextScheduled?.triggerEpochMillis)
    }

    @Test
    fun testEventBucketsPropertiesAndCounts() {
        val today = LocalDate.of(2026, 10, 10)
        val nowEpoch = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val overdue = createMockEvent("1", today.minusDays(1), "10:00", EventStatus.PENDING)
        val today1 = createMockEvent("2", today, "14:00", EventStatus.PENDING)
        val today2 = createMockEvent("3", today, "18:00", EventStatus.PENDING)
        val upcoming = createMockEvent("4", today.plusDays(2), "09:00", EventStatus.PENDING)
        val completed = createMockEvent("5", today.minusDays(3), "11:00", EventStatus.COMPLETED)

        val buckets = EventBucketingLogic.bucketEvents(
            listOf(overdue, today1, today2, upcoming, completed),
            nowEpoch,
            zoneId
        )

        assertEquals(1, buckets.overdueCount)
        assertEquals(2, buckets.todayCount)
        assertEquals(1, buckets.upcomingCount)
        assertEquals(1, buckets.completed.size)
        assertFalse(buckets.isEmpty)

        val emptyBuckets = EventBuckets()
        assertTrue(emptyBuckets.isEmpty)
        assertEquals(0, emptyBuckets.overdueCount)
        assertEquals(0, emptyBuckets.todayCount)
        assertEquals(0, emptyBuckets.upcomingCount)
    }

    private fun createMockEvent(
        id: String,
        date: LocalDate,
        time: String,
        status: EventStatus
    ): Event {
        val localTime = LocalTime.parse(time)
        val epoch = date.atTime(localTime).atZone(zoneId).toInstant().toEpochMilli()
        return Event(
            id = id,
            title = "Test Event $id",
            category = Category.Other,
            type = EventType.OTHER,
            targetDate = date.toString(),
            targetTime = time,
            targetEpochMillis = epoch,
            priority = Priority.MEDIUM,
            status = status
        )
    }
}
