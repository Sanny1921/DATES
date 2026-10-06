package com.nevermiss.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nevermiss.app.data.Event
import com.nevermiss.app.logic.AppGraph
import com.nevermiss.app.reminders.NotificationHelper
import com.nevermiss.app.ui.calendar.CalendarScreen
import com.nevermiss.app.ui.create.CreateEventScreen
import com.nevermiss.app.ui.detail.EventDetailScreen
import com.nevermiss.app.ui.history.HistoryScreen
import com.nevermiss.app.ui.home.HomeScreen
import com.nevermiss.app.ui.theme.BrandOrange
import com.nevermiss.app.ui.theme.NeverMissTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppScreen {
    HOME,
    CALENDAR,
    CREATE,
    HISTORY,
    DETAIL
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    private var initialTargetEventIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(this)

        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Check if opened from notification tap
        handleIncomingIntent(intent)

        setContent {
            NeverMissTheme {
                NeverMissMainApp(
                    initialTargetEventId = initialTargetEventIdState.value,
                    onClearTargetEventId = { initialTargetEventIdState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val targetEventId = intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_EVENT_ID)
        if (!targetEventId.isNullOrBlank()) {
            initialTargetEventIdState.value = targetEventId
        }
    }
}

@Composable
fun NeverMissMainApp(
    initialTargetEventId: String?,
    onClearTargetEventId: () -> Unit
) {
    val repository = remember { AppGraph.repository }
    val events by repository.getAllEvents().collectAsStateWithLifecycle(initialValue = emptyList())

    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var createInitialDate by remember { mutableStateOf<LocalDate?>(null) }

    // React to notification deep link target event ID
    LaunchedEffect(initialTargetEventId, events) {
        if (!initialTargetEventId.isNullOrBlank() && events.isNotEmpty()) {
            val target = events.find { it.id == initialTargetEventId }
            if (target != null) {
                selectedEvent = target
                currentScreen = AppScreen.DETAIL
                onClearTargetEventId()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != AppScreen.CREATE && currentScreen != AppScreen.DETAIL) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HOME,
                        onClick = { currentScreen = AppScreen.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandOrange,
                            selectedTextColor = BrandOrange,
                            indicatorColor = BrandOrange.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.CALENDAR,
                        onClick = { currentScreen = AppScreen.CALENDAR },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                        label = { Text("Calendar") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandOrange,
                            selectedTextColor = BrandOrange,
                            indicatorColor = BrandOrange.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.CREATE,
                        onClick = {
                            createInitialDate = null
                            currentScreen = AppScreen.CREATE
                        },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Add") },
                        label = { Text("Add") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandOrange,
                            selectedTextColor = BrandOrange,
                            indicatorColor = BrandOrange.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HISTORY,
                        onClick = { currentScreen = AppScreen.HISTORY },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandOrange,
                            selectedTextColor = BrandOrange,
                            indicatorColor = BrandOrange.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.HOME -> {
                HomeScreen(
                    events = events,
                    onEventClick = { event ->
                        selectedEvent = event
                        currentScreen = AppScreen.DETAIL
                    },
                    onToggleDone = { event ->
                        coroutineScope.launch {
                            val isDone = event.status == com.nevermiss.app.data.EventStatus.COMPLETED
                            repository.markDone(event.id, !isDone)
                        }
                    },
                    onAddClick = {
                        createInitialDate = null
                        currentScreen = AppScreen.CREATE
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppScreen.CALENDAR -> {
                CalendarScreen(
                    events = events,
                    onEventClick = { event ->
                        selectedEvent = event
                        currentScreen = AppScreen.DETAIL
                    },
                    onToggleDone = { event ->
                        coroutineScope.launch {
                            val isDone = event.status == com.nevermiss.app.data.EventStatus.COMPLETED
                            repository.markDone(event.id, !isDone)
                        }
                    },
                    onAddDateClick = { date ->
                        createInitialDate = date
                        currentScreen = AppScreen.CREATE
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppScreen.CREATE -> {
                CreateEventScreen(
                    initialDate = createInitialDate,
                    onSaveEvent = { newEvent ->
                        coroutineScope.launch {
                            repository.saveEvent(newEvent)
                            currentScreen = AppScreen.HOME
                        }
                    },
                    onBack = { currentScreen = AppScreen.HOME }
                )
            }

            AppScreen.HISTORY -> {
                HistoryScreen(
                    events = events,
                    onRestoreEvent = { event ->
                        coroutineScope.launch {
                            repository.markDone(event.id, false)
                        }
                    },
                    onDeleteEvent = { event ->
                        coroutineScope.launch {
                            repository.deleteEvent(event.id)
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            AppScreen.DETAIL -> {
                val currentEvent = events.find { it.id == selectedEvent?.id } ?: selectedEvent
                if (currentEvent != null) {
                    EventDetailScreen(
                        event = currentEvent,
                        onBack = { currentScreen = AppScreen.HOME },
                        onToggleDone = { event ->
                            coroutineScope.launch {
                                val isDone = event.status == com.nevermiss.app.data.EventStatus.COMPLETED
                                repository.markDone(event.id, !isDone)
                            }
                        },
                        onDeleteEvent = { event ->
                            coroutineScope.launch {
                                repository.deleteEvent(event.id)
                                currentScreen = AppScreen.HOME
                            }
                        }
                    )
                } else {
                    currentScreen = AppScreen.HOME
                }
            }
        }
    }
}
