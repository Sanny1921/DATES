package com.nevermiss.app.reminders

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nevermiss.app.logic.AppGraph
import kotlinx.coroutines.flow.first

/**
 * ReminderWorker: WorkManager background fallback worker.
 * Runs periodic reconciliation to catch any missed reminders (e.g. if device was off or in deep battery doze).
 */
class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ReminderWorker"
        const val WORK_NAME = "NeverMissBackupReminderWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running periodic reminder sanity check...")
        AppGraph.init(applicationContext)

        return try {
            val now = System.currentTimeMillis()
            val events = AppGraph.repository.getAllEvents().first()

            for (event in events) {
                // Find any overdue undelivered reminders that might have been missed
                val missedReminders = event.reminders.filter {
                    !it.isDelivered && it.triggerEpochMillis in (now - 24 * 3600 * 1000)..now
                }

                if (missedReminders.isNotEmpty()) {
                    for (missed in missedReminders) {
                        Log.w(TAG, "Found missed reminder for event: ${event.title}, alerting now.")
                        AppGraph.notificationHelper.showReminderNotification(
                            eventId = event.id,
                            reminderId = missed.id,
                            title = event.title,
                            note = "Missed alert: ${missed.note.ifBlank { missed.label }}",
                            severity = missed.severity.name,
                            targetTime = "${event.targetDate} • ${event.targetTime}"
                        )
                        // markReminderDelivered updates Room and schedules the next alarm using fresh database state
                        AppGraph.repository.markReminderDelivered(missed.id, event.id)
                    }
                    // Do not call scheduleNextAlarmForEvent using the stale Event object after markReminderDelivered()
                } else if (event.status != com.nevermiss.app.data.EventStatus.COMPLETED) {
                    // No reminders modified for this event; verify upcoming alarm is active
                    AppGraph.alarmScheduler.scheduleNextAlarmForEvent(event)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during reminder worker reconciliation", e)
            Result.retry()
        }
    }
}
