package com.nevermiss.app.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevermiss.app.data.*
import com.nevermiss.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    initialDate: LocalDate? = null,
    onSaveEvent: (Event) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.Payment) }
    var targetDate by remember { mutableStateOf(initialDate ?: LocalDate.now().plusDays(3)) }
    var targetTime by remember { mutableStateOf(LocalTime.of(18, 0)) }
    var selectedPriority by remember { mutableStateOf(Priority.HIGH) }
    var notes by remember { mutableStateOf("") }

    // Pre-configured reminder presets
    var selectedPreset by remember { mutableStateOf("Normal") }

    // List of reminder stages in pipeline
    var remindersList by remember {
        mutableStateOf(
            listOf(
                ReminderAlert(
                    id = UUID.randomUUID().toString(),
                    triggerEpochMillis = 0,
                    label = "3 Days Before",
                    daysBefore = 3,
                    hoursBefore = 0,
                    minutesBefore = 0,
                    severity = Severity.NORMAL
                ),
                ReminderAlert(
                    id = UUID.randomUUID().toString(),
                    triggerEpochMillis = 0,
                    label = "1 Day Before",
                    daysBefore = 1,
                    hoursBefore = 0,
                    minutesBefore = 0,
                    severity = Severity.WARN
                ),
                ReminderAlert(
                    id = UUID.randomUUID().toString(),
                    triggerEpochMillis = 0,
                    label = "1 Hour Before",
                    daysBefore = 0,
                    hoursBefore = 1,
                    minutesBefore = 0,
                    severity = Severity.CRITICAL
                )
            )
        )
    }

    var showError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasBackground,
        topBar = {
            TopAppBar(
                title = { Text("New Important Date", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasBackground)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        if (title.trim().isBlank()) {
                            showError = "Please enter an event or deadline title"
                            return@Button
                        }

                        val zone = ZoneId.systemDefault()
                        val targetDateTime = targetDate.atTime(targetTime)
                        val targetEpoch = targetDateTime.atZone(zone).toInstant().toEpochMilli()

                        val newEventId = "event-${System.currentTimeMillis()}"

                        // Calculate trigger epochs for reminders
                        val calculatedReminders = remindersList.map { rem ->
                            val triggerEpoch = targetEpoch - (rem.daysBefore.toLong() * 24 * 3600 * 1000) -
                                    (rem.hoursBefore.toLong() * 3600 * 1000) -
                                    (rem.minutesBefore.toLong() * 60 * 1000)
                            rem.copy(
                                eventId = newEventId,
                                triggerEpochMillis = triggerEpoch
                            )
                        }

                        val newEvent = Event(
                            id = newEventId,
                            title = title.trim(),
                            category = selectedCategory,
                            targetDate = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            targetTime = targetTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            targetEpochMillis = targetEpoch,
                            priority = selectedPriority,
                            notes = notes.trim(),
                            status = EventStatus.PENDING,
                            reminders = calculatedReminders
                        )

                        onSaveEvent(newEvent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Important Date",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error Alert
            if (showError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = UrgencyRedBg),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = UrgencyRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(showError!!, color = UrgencyRed, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Title input
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("EVENT / DEADLINE TITLE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                showError = null
                            },
                            placeholder = { Text("e.g. Electricity Bill, Passport Expiry", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandOrange,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )
                    }
                }
            }

            // Category picker
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CATEGORY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(Category.entries) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Target Date & Time
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TARGET DATE & TIME", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Date display/picker
                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        // Advance date cyclically for quick interaction
                                        targetDate = targetDate.plusDays(1)
                                    },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Date", fontSize = 11.sp, color = TextMuted)
                                        Text(targetDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }

                            // Time display/picker
                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        targetTime = targetTime.plusHours(1)
                                    },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Time", fontSize = 11.sp, color = TextMuted)
                                        Text(targetTime.format(DateTimeFormatter.ofPattern("hh:mm a")), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Priority level
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PRIORITY LEVEL", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Priority.entries.forEach { p ->
                                val isSelected = selectedPriority == p
                                val color = when (p) {
                                    Priority.LOW -> UrgencyGreen
                                    Priority.MEDIUM -> UrgencyAmber
                                    Priority.HIGH -> UrgencyOrange
                                    Priority.URGENT -> UrgencyRed
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { selectedPriority = p }
                                        .background(if (isSelected) color else SurfaceSubtle),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = p.displayName,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Multi-Stage Reminder Pipeline
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ALERT PIPELINE PRESET", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("${remindersList.size} Local Alerts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandOrange)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preset choices
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Gentle", "Normal", "Frequent").forEach { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    onClick = {
                                        selectedPreset = preset
                                        remindersList = when (preset) {
                                            "Gentle" -> listOf(
                                                ReminderAlert(UUID.randomUUID().toString(), "", 0, "1 Day Before", 1, 0, 0, severity = Severity.NORMAL)
                                            )
                                            "Normal" -> listOf(
                                                ReminderAlert(UUID.randomUUID().toString(), "", 0, "3 Days Before", 3, 0, 0, severity = Severity.NORMAL),
                                                ReminderAlert(UUID.randomUUID().toString(), "", 0, "1 Day Before", 1, 0, 0, severity = Severity.WARN),
                                                ReminderAlert(UUID.randomUUID().toString(), "", 0, "1 Hour Before", 0, 1, 0, severity = Severity.CRITICAL)
                                            )
                                            else -> listOf(
                                                ReminderAlert(UUID.randomUUID().toString(), "", 0, "7 Days Before", 7, 0, 0, severity = Severity.NORMAL),
                                                ReminderAlert(UUID.randomUUID().toString(), "", 0, "3 Days Before", 3, 0, 0, severity = Severity.NORMAL),
                                                ReminderAlert(UUID.randomUUID().toString(), "", 0, "1 Day Before", 1, 0, 0, severity = Severity.WARN),
                                                ReminderAlert(UUID.randomUUID().toString(), "", 0, "1 Hour Before", 0, 1, 0, severity = Severity.CRITICAL)
                                            )
                                        }
                                    },
                                    label = { Text(preset, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chrono Rail Timeline Visual
                        Text("SCHEDULED TIMELINE PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            remindersList.forEachIndexed { index, rem ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceSubtle, MaterialTheme.shapes.small)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                when (rem.severity) {
                                                    Severity.CRITICAL -> UrgencyRed
                                                    Severity.WARN -> UrgencyOrange
                                                    Severity.NORMAL -> BrandOrange
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(rem.label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = rem.severity.name.lowercase(),
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notes textarea
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("OPTIONAL NOTES / ACCOUNT DETAILS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Account number, reference link, checklist...", color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandOrange,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )
                    }
                }
            }
        }
    }
}
