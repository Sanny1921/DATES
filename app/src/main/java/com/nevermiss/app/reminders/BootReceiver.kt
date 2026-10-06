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
 * BootReceiver: Re-registers all pending exact alarms after device reboot or app update.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Device rebooted or package replaced ($action). Re-scheduling all alarms.")
            AppGraph.init(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val events = AppGraph.repository.getAllEvents().first()
                    AppGraph.alarmScheduler.rescheduleAll(events)
                    Log.d(TAG, "Successfully rescheduled alarms for ${events.size} events.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reschedule alarms after reboot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
