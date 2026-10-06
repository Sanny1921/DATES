package com.nevermiss.app.reminders

import android.content.Context
import android.util.Log
import androidx.work.*
import com.nevermiss.app.logic.AppGraph

/**
 * RescheduleWorker: Background worker to restore future pending alarms
 * after device reboots, app updates, clock/timezone changes, or permission updates.
 */
class RescheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "RescheduleWorker"
        const val WORK_NAME = "NeverMissRescheduleWorker"

        /**
         * Enqueues an immediate one-time alarm restore task.
         */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<RescheduleWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Restoring future alarms...")
        AppGraph.init(applicationContext)
        return try {
            AppGraph.repository.restoreFutureAlarms()
            Log.d(TAG, "Successfully restored future alarms.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore future alarms", e)
            Result.retry()
        }
    }
}
