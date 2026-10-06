package com.nevermiss.app.logic

import com.nevermiss.app.data.Category
import com.nevermiss.app.data.Event
import com.nevermiss.app.data.EventStatus
import com.nevermiss.app.data.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * EventLabelsTest: Unit tests for pure calculation logic in EventBucketingLogic.
 * Tests label generation ("4 days left", "Today", "Tomorrow", "Overdue by X days"),
 * event bucketing into Overdue/Today/Upcoming/Completed, and stats calculations.
 */
class EventLabelsTest {

    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testCountdownLabel_FourDaysLeft() {
        val today = LocalDate.of(2026, 10, 10)
        val nowEpoch = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val target = LocalDate.of(2026, 10, 14)
        val targetEpoch = target.atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli()

        val label = EventBucketingLogic.formatCountdownLabel(targetEpoch, nowEpoch, zoneId)
        assertEquals("4 days left", label)
    }

    @Test
    fun testCountdownLabel_Tomorrow() {
        val today = LocalDate.of(2026, 10, 10)
        val nowEpoch = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val target = LocalDate.of(2026, 10, 11)
        val targetEpoch = target.atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli()

        val label = EventBucketingLogic.formatCountdownLabel(targetEpoch, nowEpoch, zoneId)
        assertEquals("Tomorrow", label)
    }

    @Test
    fun testCountdownLabel_Today() {
        val today = LocalDate.of(2026, 10, 10)
        val nowEpoch = today.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli()

        val target = LocalDate.of(2026, 10, 10)
        val targetEpoch = target.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val label = EventBucketingLogic.formatCountdownLabel(targetEpoch, nowEpoch, zoneId)
        assertEquals("Due in 2 hours", label)
    }

    @Test
    fun testCountdownLabel_OverdueByTwoDays() {
        val today = LocalDate.of(2026, 10, 10)
        val nowEpoch = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val target = LocalDate.of(2026, 10, 8)
        val targetEpoch = target.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val label = EventBucketingLogic.formatCountdownLabel(targetEpoch, nowEpoch, zoneId)
        assertEquals("Overdue by 2 days", label)
    }

    @Test
    fun testCountdownLabel_OverdueByOneDay() {
        val today = LocalDate.of(2026, 10, 10)
        val nowEpoch = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val target = LocalDate.of(2026, 10, 9)
        val targetEpoch = target.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val label = EventBucketingLogic.formatCountdownLabel(targetEpoch, nowEpoch, zoneId)
        assertEquals("Overdue by 1 day", label)
    }

    @Test
    fun testEventBucketing_PartitionsCorrectly() {
        val today = LocalDate.of(2026, 10, 10)
        val nowEpoch = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val overdueEvent = createMockEvent("1", today.minusDays(2), "10:00", EventStatus.PENDING)
        val todayEvent = createMockEvent("2", today, "18:00", EventStatus.PENDING)
        val upcomingEvent = createMockEvent("3", today.plusDays(4), "15:00", EventStatus.PENDING)
        val completedEvent = createMockEvent("4", today.minusDays(5), "09:00", EventStatus.COMPLETED)

        val events = listOf(overdueEvent, todayEvent, upcomingEvent, completedEvent)
        val buckets = EventBucketingLogic.bucketEvents(events, nowEpoch, zoneId)

        assertEquals(1, buckets.overdue.size)
        assertEquals("1", buckets.overdue.first().id)

        assertEquals(1, buckets.today.size)
        assertEquals("2", buckets.today.first().id)

        assertEquals(1, buckets.upcoming.size)
        assertEquals("3", buckets.upcoming.first().id)

        assertEquals(1, buckets.completed.size)
        assertEquals("4", buckets.completed.first().id)
    }

    @Test
    fun testBucketCountsAndCompletionRate() {
        val today = LocalDate.of(2026, 10, 10)
        val nowEpoch = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

        val overdueEvent = createMockEvent("1", today.minusDays(1), "10:00", EventStatus.PENDING)
        val todayEvent = createMockEvent("2", today, "18:00", EventStatus.PENDING)
        val completed1 = createMockEvent("3", today.minusDays(2), "09:00", EventStatus.COMPLETED)
        val completed2 = createMockEvent("4", today.minusDays(3), "09:00", EventStatus.COMPLETED)

        val buckets = EventBucketingLogic.bucketEvents(listOf(overdueEvent, todayEvent, completed1, completed2), nowEpoch, zoneId)
        val counts = EventBucketingLogic.calculateCounts(buckets)

        assertEquals(1, counts.overdueCount)
        assertEquals(1, counts.todayCount)
        assertEquals(0, counts.upcomingCount)
        assertEquals(2, counts.completedCount)
        assertEquals(4, counts.totalCount)
        assertEquals(50, counts.completionRatePercent) // 2 / 4 = 50%
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
            category = Category.Payment,
            targetDate = date.toString(),
            targetTime = time,
            targetEpochMillis = epoch,
            priority = Priority.HIGH,
            status = status
        )
    }
}
