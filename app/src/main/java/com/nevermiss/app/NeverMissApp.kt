package com.nevermiss.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nevermiss.app.data.EventDatabase
import com.nevermiss.app.logic.AppGraph
import com.nevermiss.app.reminders.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * NeverMissApp: Custom Application class initializing the offline-first graph and backup worker.
 */
class NeverMissApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize singletons, Room, notification channels
        AppGraph.init(this)

        // Callback trigger for seed event alarms on first database creation
        EventDatabase.onSeedComplete = {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AppGraph.repository.restoreFutureAlarms()
                    getSharedPreferences("nevermiss_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("initial_seeds_scheduled", true).apply()
                    Log.d("NeverMissApp", "Scheduled alarms for newly seeded events.")
                } catch (e: Exception) {
                    Log.e("NeverMissApp", "Failed to schedule initial seed alarms from callback", e)
                }
            }
        }

        // Verify initial seed events have alarms scheduled once after initialization
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = getSharedPreferences("nevermiss_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("initial_seeds_scheduled", false)) {
                try {
                    val db = AppGraph.database
                    db.seedInitialEvents()
                    AppGraph.repository.restoreFutureAlarms()
                    prefs.edit().putBoolean("initial_seeds_scheduled", true).apply()
                    Log.d("NeverMissApp", "Verified and scheduled initial seed event alarms.")
                } catch (e: Exception) {
                    Log.e("NeverMissApp", "Failed to initialize seed alarms on startup", e)
                }
            }
        }

        // Register periodic background fallback worker (runs every 6 hours)
        val backupWorkerRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            6, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupWorkerRequest
        )
    }
}
