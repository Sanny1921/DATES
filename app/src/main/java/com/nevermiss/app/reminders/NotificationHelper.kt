package com.nevermiss.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nevermiss.app.MainActivity
import com.nevermiss.app.R

import android.net.Uri
import com.nevermiss.app.data.Event

/**
 * NotificationHelper: Creates notification channels and builds rich tactile local notifications.
 * Tapping a notification opens MainActivity directly focused on the target event.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_URGENT = "nevermiss_urgent_channel"
        const val CHANNEL_NORMAL = "nevermiss_normal_channel"
        const val EXTRA_TARGET_EVENT_ID = "extra_event_id"

        /**
         * Checks if the app has notification permissions granted.
         */
        fun canNotify(context: Context): Boolean {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        /**
         * Extracts target event ID from incoming notification intent or deep-link URI.
         */
        fun eventIdFromIntent(intent: Intent?): String? {
            if (intent == null) return null
            val fromExtra = intent.getStringExtra(EXTRA_TARGET_EVENT_ID)
            if (!fromExtra.isNullOrBlank()) return fromExtra

            val data = intent.data
            if (data != null && data.scheme == "offline-events" && data.host == "event") {
                return data.lastPathSegment
            }
            return null
        }

        /**
         * Creates notification channels statically.
         */
        fun createChannel(context: Context) {
            NotificationHelper(context).createNotificationChannels()
        }

        /**
         * Formats body text for notifications according to specification.
         */
        fun body(event: Event): String {
            return if (event.notes.isNotBlank()) {
                "${event.notes} • Due: ${event.targetDate} at ${event.targetTime}"
            } else {
                "Due: ${event.targetDate} at ${event.targetTime}"
            }
        }
    }

    /**
     * Initializes notification channels required for Android 8.0 (API 26) and above.
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT,
                context.getString(R.string.notification_channel_urgent),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_urgent_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 500)
                enableLights(true)
            }

            val normalChannel = NotificationChannel(
                CHANNEL_NORMAL,
                context.getString(R.string.notification_channel_normal),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_normal_desc)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(urgentChannel)
            notificationManager.createNotificationChannel(normalChannel)
        }
    }

    /**
     * Builds and displays the local notification.
     */
    fun showReminderNotification(
        eventId: String,
        reminderId: String,
        title: String,
        note: String,
        severity: String,
        targetTime: String
    ) {
        val channelId = if (severity.equals("critical", ignoreCase = true) || severity.equals("warn", ignoreCase = true)) {
            CHANNEL_URGENT
        } else {
            CHANNEL_NORMAL
        }

        // Tap opens MainActivity with target event ID and deep-link URI: offline-events://event/<id>
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.nevermiss.app.OPEN_EVENT"
            data = Uri.parse("offline-events://event/$eventId")
            putExtra(EXTRA_TARGET_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ $title")
            .setContentText(if (note.isNotBlank()) "$note ($targetTime)" else "Due: $targetTime")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$note\nDeadline: $targetTime\n• Local Offline Alert")
            )
            .setPriority(
                if (channelId == CHANNEL_URGENT) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)

        if (channelId == CHANNEL_URGENT) {
            builder.setVibrate(longArrayOf(0, 300, 150, 300, 150, 500))
        }

        try {
            val notificationId = (eventId.hashCode() xor reminderId.hashCode())
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (se: SecurityException) {
            // Android 13+ runtime POST_NOTIFICATIONS permission not granted
        }
    }
}
