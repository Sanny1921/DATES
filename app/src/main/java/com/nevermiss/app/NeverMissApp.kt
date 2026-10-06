package com.nevermiss.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nevermiss.app.logic.AppGraph
import com.nevermiss.app.reminders.ReminderWorker
import java.util.concurrent.TimeUnit

/**
 * NeverMissApp: Custom Application class initializing the offline-first graph and backup worker.
 */
class NeverMissApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize singletons, Room, notification channels
        AppGraph.init(this)

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
