package com.nevermiss.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.nevermiss.app.data.Event
import com.nevermiss.app.data.EventStatus

import android.net.Uri
import android.provider.Settings

/**
 * AlarmScheduler: Manages Android AlarmManager exact and low-latency alarms.
 * Schedules the single next upcoming reminder alert per event.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        const val ACTION_TRIGGER_REMINDER = "com.nevermiss.app.TRIGGER_REMINDER"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTE = "extra_note"
        const val EXTRA_SEVERITY = "extra_severity"
        const val EXTRA_TARGET_TIME = "extra_target_time"
    }

    /**
     * Checks whether the application has permission to schedule exact alarms.
     */
    fun canUseExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    /**
     * Intent to open the Android system settings page for exact alarm permission.
     */
    fun exactAlarmPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * Determines and sets the nearest upcoming alarm for an event.
     * Returns the precision mode (EXACT, INEXACT, or NONE) used.
     */
    fun scheduleNextAlarmForEvent(event: Event): AlarmMode {
        if (event.status == EventStatus.COMPLETED) {
            cancelAlarmsForEvent(event.id)
            return AlarmMode.NONE
        }

        val now = System.currentTimeMillis()

        // Find the earliest undelivered reminder scheduled in the future
        val nextReminder = event.reminders
            .filter { !it.isDelivered && it.triggerEpochMillis > now }
            .minByOrNull { it.triggerEpochMillis }

        if (nextReminder == null) {
            Log.d(TAG, "No upcoming pending reminders for event: ${event.title}. Cancelling any lingering alarms.")
            cancelAlarmsForEvent(event.id)
            return AlarmMode.NONE
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_EVENT_ID, event.id)
            putExtra(EXTRA_REMINDER_ID, nextReminder.id)
            putExtra(EXTRA_TITLE, event.title)
            putExtra(
                EXTRA_NOTE,
                if (nextReminder.note.isNotBlank()) nextReminder.note else nextReminder.label
            )
            putExtra(EXTRA_SEVERITY, nextReminder.severity.name)
            putExtra(EXTRA_TARGET_TIME, "${event.targetDate} • ${event.targetTime}")
        }

        val requestCode = event.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager?.canScheduleExactAlarms() == true) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextReminder.triggerEpochMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for ${event.title} at ${nextReminder.triggerEpochMillis}")
                    AlarmMode.EXACT
                } else {
                    alarmManager?.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextReminder.triggerEpochMillis,
                        pendingIntent
                    )
                    AlarmMode.INEXACT
                }
            } else {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextReminder.triggerEpochMillis,
                    pendingIntent
                )
                AlarmMode.EXACT
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm, falling back to inexact", se)
            // Fallback non-exact
            alarmManager?.set(
                AlarmManager.RTC_WAKEUP,
                nextReminder.triggerEpochMillis,
                pendingIntent
            )
            AlarmMode.INEXACT
        }
    }

    /**
     * Cancels any scheduled alarm for the specified event ID.
     */
    fun cancelAlarmsForEvent(eventId: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val requestCode = eventId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for eventId: $eventId")
        }
    }

    /**
     * Reschedules next alarms for a batch of active events (e.g. after reboot).
     */
    fun rescheduleAll(events: List<Event>) {
        for (event in events) {
            if (event.status != EventStatus.COMPLETED) {
                scheduleNextAlarmForEvent(event)
            }
        }
    }
}
