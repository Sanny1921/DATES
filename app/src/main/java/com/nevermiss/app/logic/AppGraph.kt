package com.nevermiss.app.logic

import android.annotation.SuppressLint
import android.content.Context
import com.nevermiss.app.data.EventDatabase
import com.nevermiss.app.data.EventRepository
import com.nevermiss.app.reminders.AlarmScheduler
import com.nevermiss.app.reminders.NotificationHelper

/**
 * AppGraph: Dependency injection / service locator that wires the application components together once.
 * Provides singleton access to Database, Repository, AlarmScheduler, and NotificationHelper.
 */
@SuppressLint("StaticFieldLeak")
object AppGraph {

    private lateinit var appContext: Context

    val database: EventDatabase by lazy {
        checkInitialized()
        EventDatabase.getInstance(appContext)
    }

    val notificationHelper: NotificationHelper by lazy {
        checkInitialized()
        NotificationHelper(appContext)
    }

    val alarmScheduler: AlarmScheduler by lazy {
        checkInitialized()
        AlarmScheduler(appContext)
    }

    val repository: EventRepository by lazy {
        checkInitialized()
        EventRepository(
            eventDao = database.eventDao(),
            alarmScheduler = alarmScheduler
        )
    }

    fun init(context: Context) {
        if (!::appContext.isInitialized) {
            appContext = context.applicationContext
            notificationHelper.createNotificationChannels()
        }
    }

    private fun checkInitialized() {
        if (!::appContext.isInitialized) {
            throw IllegalStateException("AppGraph must be initialized by calling AppGraph.init(context)")
        }
    }
}
