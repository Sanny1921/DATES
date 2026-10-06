package com.nevermiss.app.logic

import com.nevermiss.app.data.Event
import com.nevermiss.app.data.EventStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * EventBuckets: Pure calculation engine.
 * No Android framework dependencies.
 * Partitions events into Overdue, Today, Upcoming, and Completed buckets.
 */
data class EventBuckets(
    val overdue: List<Event>,
    val today: List<Event>,
    val upcoming: List<Event>,
    val completed: List<Event>
)

data class BucketCounts(
    val overdueCount: Int,
    val todayCount: Int,
    val upcomingCount: Int,
    val completedCount: Int,
    val totalCount: Int,
    val completionRatePercent: Int
)

enum class UrgencyTier {
    OVERDUE,
    TODAY,
    SOON, // 1 to 3 days
    UPCOMING, // > 3 days
    COMPLETED
}

data class UrgencyBadge(
    val tier: UrgencyTier,
    val text: String,
    val dotColorHex: String
)

object EventBucketingLogic {

    /**
     * Splits a list of events into Overdue, Today, Upcoming, and Completed.
     */
    fun bucketEvents(
        events: List<Event>,
        nowEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): EventBuckets {
        val todayDate = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()

        val completed = mutableListOf<Event>()
        val overdue = mutableListOf<Event>()
        val today = mutableListOf<Event>()
        val upcoming = mutableListOf<Event>()

        for (event in events) {
            if (event.status == EventStatus.COMPLETED) {
                completed.add(event)
                continue
            }

            val eventDate = try {
                LocalDate.parse(event.targetDate)
            } catch (e: Exception) {
                Instant.ofEpochMilli(event.targetEpochMillis).atZone(zoneId).toLocalDate()
            }

            when {
                // If the target date was before today or deadline already elapsed
                eventDate.isBefore(todayDate) || (eventDate == todayDate && event.targetEpochMillis < nowEpochMillis) -> {
                    overdue.add(event)
                }
                eventDate == todayDate -> {
                    today.add(event)
                }
                else -> {
                    upcoming.add(event)
                }
            }
        }

        return EventBuckets(
            overdue = overdue.sortedBy { it.targetEpochMillis },
            today = today.sortedBy { it.targetEpochMillis },
            upcoming = upcoming.sortedBy { it.targetEpochMillis },
            completed = completed.sortedByDescending { it.completedAtEpochMillis ?: it.targetEpochMillis }
        )
    }

    /**
     * Aggregates counts and computes overall completion rate.
     */
    fun calculateCounts(buckets: EventBuckets): BucketCounts {
        val overdue = buckets.overdue.size
        val today = buckets.today.size
        val upcoming = buckets.upcoming.size
        val completed = buckets.completed.size
        val total = overdue + today + upcoming + completed

        val rate = if (total > 0) {
            ((completed.toDouble() / total.toDouble()) * 100).toInt()
        } else {
            0
        }

        return BucketCounts(
            overdueCount = overdue,
            todayCount = today,
            upcomingCount = upcoming,
            completedCount = completed,
            totalCount = total,
            completionRatePercent = rate
        )
    }

    /**
     * Formats human-readable countdown labels like "4 days left", "Today", "Tomorrow", "Overdue by 2 days".
     */
    fun formatCountdownLabel(
        targetEpochMillis: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val nowDate = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
        val targetDate = Instant.ofEpochMilli(targetEpochMillis).atZone(zoneId).toLocalDate()

        val daysDiff = ChronoUnit.DAYS.between(nowDate, targetDate)
        val millisDiff = targetEpochMillis - nowEpochMillis

        return when {
            millisDiff < 0 -> {
                val overdueDays = ChronoUnit.DAYS.between(targetDate, nowDate)
                if (overdueDays > 0) {
                    "Overdue by $overdueDays ${if (overdueDays == 1L) "day" else "days"}"
                } else {
                    val overdueHours = (-millisDiff) / (1000 * 3600)
                    if (overdueHours > 0) {
                        "Overdue by $overdueHours ${if (overdueHours == 1L) "hour" else "hours"}"
                    } else {
                        "Overdue"
                    }
                }
            }
            daysDiff == 0L -> {
                val hoursRemaining = millisDiff / (1000 * 3600)
                if (hoursRemaining in 1..4) {
                    "Due in $hoursRemaining ${if (hoursRemaining == 1L) "hour" else "hours"}"
                } else {
                    "Today"
                }
            }
            daysDiff == 1L -> "Tomorrow"
            daysDiff in 2..30 -> "$daysDiff days left"
            else -> {
                val weeks = daysDiff / 7
                if (weeks > 0) "$weeks ${if (weeks == 1L) "week" else "weeks"} left" else "$daysDiff days left"
            }
        }
    }

    /**
     * Determines urgency tier and badge formatting.
     */
    fun getUrgencyBadge(
        targetEpochMillis: Long,
        status: EventStatus,
        nowEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): UrgencyBadge {
        if (status == EventStatus.COMPLETED) {
            return UrgencyBadge(UrgencyTier.COMPLETED, "Completed", "#10B981")
        }

        val nowDate = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
        val targetDate = Instant.ofEpochMilli(targetEpochMillis).atZone(zoneId).toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(nowDate, targetDate)

        return when {
            targetEpochMillis < nowEpochMillis || daysDiff < 0 -> {
                UrgencyBadge(UrgencyTier.OVERDUE, "Overdue", "#EF4444")
            }
            daysDiff == 0L -> {
                UrgencyBadge(UrgencyTier.TODAY, "Due Today", "#EF4444")
            }
            daysDiff in 1..2 -> {
                UrgencyBadge(UrgencyTier.SOON, "Due in $daysDiff ${if (daysDiff == 1L) "Day" else "Days"}", "#F97316")
            }
            daysDiff in 3..7 -> {
                UrgencyBadge(UrgencyTier.SOON, "$daysDiff Days Left", "#F59E0B")
            }
            else -> {
                UrgencyBadge(UrgencyTier.UPCOMING, "$daysDiff Days Left", "#10B981")
            }
        }
    }
}
