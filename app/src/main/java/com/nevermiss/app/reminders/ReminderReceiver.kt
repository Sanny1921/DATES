package com.nevermiss.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nevermiss.app.logic.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ReminderReceiver: Triggered by Android AlarmManager when an exact reminder alarm fires.
 * Displays the notification and schedules the subsequent reminder node in the pipeline.
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return
        val reminderId = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_ID) ?: ""
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "NeverMiss Reminder"
        val note = intent.getStringExtra(AlarmScheduler.EXTRA_NOTE) ?: ""
        val severity = intent.getStringExtra(AlarmScheduler.EXTRA_SEVERITY) ?: "normal"
        val targetTime = intent.getStringExtra(AlarmScheduler.EXTRA_TARGET_TIME) ?: ""

        Log.d(TAG, "Alarm fired for event: $title (eventId: $eventId, reminderId: $reminderId)")

        AppGraph.init(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val event = AppGraph.repository.getEventById(eventId).first()
                if (event == null || event.status == com.nevermiss.app.data.EventStatus.COMPLETED) {
                    Log.d(TAG, "Event $eventId no longer active or already completed. Skipping reminder notification.")
                    return@launch
                }

                // 1. Post notification
                AppGraph.notificationHelper.showReminderNotification(
                    eventId = eventId,
                    reminderId = reminderId,
                    title = event.title,
                    note = event.notes,
                    severity = severity,
                    targetTime = event.targetTime
                )

                // 2. Mark handled and schedule next future reminder
                if (reminderId.isNotEmpty()) {
                    AppGraph.repository.markReminderDelivered(reminderId, eventId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
