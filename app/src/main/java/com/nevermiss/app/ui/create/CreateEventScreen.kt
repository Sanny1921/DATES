package com.nevermiss.app.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevermiss.app.data.*
import com.nevermiss.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class CustomUnit(val label: String, val multiplier: Int) {
    MINUTES("Minutes", 1),
    HOURS("Hours", 60),
    DAYS("Days", 1440)
}

data class ReminderSelection(
    val minutesBefore: Int,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    initialDate: LocalDate? = null,
    eventToEdit: Event? = null,
    onSaveEvent: (Event) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditing = eventToEdit != null

    var title by remember { mutableStateOf(eventToEdit?.title ?: "") }
    var selectedType by remember { mutableStateOf(eventToEdit?.type ?: EventType.OTHER) }
    var selectedCategory by remember { mutableStateOf(eventToEdit?.category ?: Category.Other) }

    var targetDate by remember {
        mutableStateOf(
            if (eventToEdit != null && eventToEdit.targetDate.isNotBlank()) {
                try { LocalDate.parse(eventToEdit.targetDate) } catch (e: Exception) { LocalDate.now().plusDays(1) }
            } else {
                initialDate ?: LocalDate.now().plusDays(1)
            }
        )
    }

    var targetTime by remember {
        mutableStateOf(
            if (eventToEdit != null && eventToEdit.targetTime.isNotBlank()) {
                try { LocalTime.parse(eventToEdit.targetTime) } catch (e: Exception) { LocalTime.of(12, 0) }
            } else {
                LocalTime.of(12, 0)
            }
        )
    }

    var selectedPriority by remember { mutableStateOf(eventToEdit?.priority ?: Priority.HIGH) }
    var notes by remember { mutableStateOf(eventToEdit?.notes ?: "") }

    // Selected reminders list: maximum 5, unique minutesBefore
    var selectedReminders by remember {
        mutableStateOf(
            if (eventToEdit != null) {
                eventToEdit.reminders.map {
                    ReminderSelection(
                        minutesBefore = it.minutesBefore,
                        label = formatReminderLabel(it.minutesBefore)
                    )
                }.distinctBy { it.minutesBefore }.sortedByDescending { it.minutesBefore }.take(5)
            } else {
                listOf(
                    ReminderSelection(30, "30 minutes before"),
                    ReminderSelection(10, "10 minutes before")
                )
            }
        )
    }

    // Custom reminder modal state
    var showCustomDialog by remember { mutableStateOf(false) }
    var customQuantityText by remember { mutableStateOf("1") }
    var selectedCustomUnit by remember { mutableStateOf(CustomUnit.HOURS) }
    var customError by remember { mutableStateOf<String?>(null) }

    var showError by remember { mutableStateOf<String?>(null) }

    val presetOptions = listOf(
        30 to "30 minutes",
        15 to "15 minutes",
        10 to "10 minutes",
        5 to "5 minutes"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Important Date" else "New Important Date",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
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
                        val trimmedTitle = title.trim()
                        if (trimmedTitle.isBlank()) {
                            showError = "Please enter an event or deadline title"
                            return@Button
                        }
                        if (trimmedTitle.length > 80) {
                            showError = "Title cannot exceed 80 characters (currently ${trimmedTitle.length})"
                            return@Button
                        }

                        val zone = ZoneId.systemDefault()
                        val targetDateTime = targetDate.atTime(targetTime)
                        val targetEpoch = targetDateTime.atZone(zone).toInstant().toEpochMilli()
                        val now = System.currentTimeMillis()

                        // Validation: new events must be in the future
                        if (!isEditing && targetEpoch <= now) {
                            showError = "Event date and time must be set in the future"
                            return@Button
                        }

                        // Validation: maximum 5 reminders
                        if (selectedReminders.size > 5) {
                            showError = "Maximum 5 reminders allowed per event"
                            return@Button
                        }

                        val eventId = eventToEdit?.id ?: "event-${System.currentTimeMillis()}"

                        // Sort reminders largest to smallest according to specification
                        val sortedSelections = selectedReminders.sortedByDescending { it.minutesBefore }

                        val calculatedReminders = sortedSelections.map { sel ->
                            val triggerEpoch = targetEpoch - (sel.minutesBefore.toLong() * 60 * 1000)
                            val severity = when {
                                sel.minutesBefore <= 15 -> Severity.CRITICAL
                                sel.minutesBefore <= 60 -> Severity.WARN
                                else -> Severity.NORMAL
                            }
                            ReminderAlert(
                                id = UUID.randomUUID().toString(),
                                eventId = eventId,
                                triggerEpochMillis = triggerEpoch,
                                label = sel.label,
                                daysBefore = sel.minutesBefore / 1440,
                                hoursBefore = (sel.minutesBefore % 1440) / 60,
                                minutesBefore = sel.minutesBefore % 60,
                                severity = severity,
                                isDelivered = false
                            )
                        }

                        val newEvent = Event(
                            id = eventId,
                            title = trimmedTitle,
                            type = selectedType,
                            category = selectedCategory,
                            targetDate = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            targetTime = targetTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                            targetEpochMillis = targetEpoch,
                            priority = selectedPriority,
                            notes = notes.trim(),
                            status = eventToEdit?.status ?: EventStatus.PENDING,
                            completedAtEpochMillis = eventToEdit?.completedAtEpochMillis,
                            createdAtEpochMillis = eventToEdit?.createdAtEpochMillis ?: now,
                            updatedAtEpochMillis = now,
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
                        text = if (isEditing) "Save Changes" else "Save Important Date",
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
            // Error Alert Banner
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

            // Title input with 80 character limit
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("EVENT / DEADLINE TITLE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = "${title.length}/80",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (title.length > 80) UrgencyRed else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                if (it.length <= 80) {
                                    title = it
                                    showError = null
                                }
                            },
                            placeholder = { Text("e.g. Physics Final Exam, Rent Payment", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandOrange,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )
                    }
                }
            }

            // Event Type selector (The 5 delivered specification values)
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("EVENT TYPE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(EventType.entries) { type ->
                                val isSelected = selectedType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedType = type
                                        // Auto-map category if compatible
                                        selectedCategory = when (type) {
                                            EventType.BIRTHDAY -> Category.Birthday
                                            EventType.EXAM -> Category.Education
                                            EventType.ASSIGNMENT -> Category.Work
                                            EventType.MEETING -> Category.Work
                                            EventType.OTHER -> Category.Other
                                        }
                                    },
                                    label = { Text(type.name) },
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

            // Target Date & Time Picker
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TARGET DATE & TIME", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Date card (tap to cycle next days or select)
                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        targetDate = targetDate.plusDays(1)
                                        showError = null
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

                            // Time card (tap to cycle hours)
                            OutlinedCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        targetTime = targetTime.plusHours(1)
                                        showError = null
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

            // Multi-Stage Reminder Selector (Presets + Custom, Max 5)
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
                            Text("REMINDER NOTIFICATIONS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = "${selectedReminders.size} / 5 Selected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedReminders.size >= 5) UrgencyAmber else BrandOrange
                            )
                        }

                        if (selectedReminders.size >= 5) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Maximum 5 reminders reached. Deselect or remove one to choose another.",
                                fontSize = 12.sp,
                                color = UrgencyAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preset chips row: 30m, 15m, 10m, 5m + Custom button
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(presetOptions) { (minutes, label) ->
                                val isSelected = selectedReminders.any { it.minutesBefore == minutes }
                                val canSelect = isSelected || selectedReminders.size < 5

                                FilterChip(
                                    selected = isSelected,
                                    enabled = canSelect,
                                    onClick = {
                                        if (isSelected) {
                                            selectedReminders = selectedReminders.filter { it.minutesBefore != minutes }
                                        } else if (selectedReminders.size < 5) {
                                            selectedReminders = (selectedReminders + ReminderSelection(minutes, "$label before"))
                                                .sortedByDescending { it.minutesBefore }
                                        }
                                        showError = null
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            // Custom reminder trigger chip
                            item {
                                OutlinedButton(
                                    onClick = {
                                        customQuantityText = "1"
                                        selectedCustomUnit = CustomUnit.HOURS
                                        customError = null
                                        showCustomDialog = true
                                    },
                                    enabled = selectedReminders.size < 5,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Custom", fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // List of active chosen reminders with remove button
                        Text("ACTIVE REMINDERS TIMELINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedReminders.isEmpty()) {
                            Text(
                                text = "No reminders selected. You can add up to 5 reminders.",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedReminders.forEachIndexed { index, rem ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SurfaceSubtle, MaterialTheme.shapes.small)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .background(BrandOrange, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${index + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = rem.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                selectedReminders = selectedReminders.filter { it.minutesBefore != rem.minutesBefore }
                                                showError = null
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Priority level selector
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

            // Optional notes
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("OPTIONAL NOTES / SUBJECT DETAILS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Exam topic, room code, payment amount...", color = TextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
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

    // Custom reminder input dialog
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Add Custom Reminder", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter a duration before the event (converts to minutes):",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = customQuantityText,
                        onValueChange = {
                            customQuantityText = it.filter { char -> char.isDigit() }
                            customError = null
                        },
                        label = { Text("Duration number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Unit:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = selectedCustomUnit == unit,
                                onClick = { selectedCustomUnit = unit },
                                label = { Text(unit.label) }
                            )
                        }
                    }

                    if (customError != null) {
                        Text(customError!!, color = UrgencyRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val quantity = customQuantityText.toIntOrNull()
                        if (quantity == null || quantity <= 0) {
                            customError = "Please enter a positive whole number"
                            return@Button
                        }

                        val calculatedMinutes = quantity * selectedCustomUnit.multiplier
                        if (calculatedMinutes > 5_256_000) {
                            customError = "Reminder cannot exceed 5,256,000 minutes (10 years)"
                            return@Button
                        }

                        if (selectedReminders.any { it.minutesBefore == calculatedMinutes }) {
                            customError = "A reminder for this exact duration already exists"
                            return@Button
                        }

                        if (selectedReminders.size >= 5) {
                            customError = "Maximum 5 reminders already reached"
                            return@Button
                        }

                        val customLabel = "$quantity ${selectedCustomUnit.label.lowercase()} before"
                        selectedReminders = (selectedReminders + ReminderSelection(calculatedMinutes, customLabel))
                            .sortedByDescending { it.minutesBefore }

                        showCustomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Text("Add Reminder", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

private fun formatReminderLabel(minutes: Int): String {
    return when {
        minutes == 0 -> "At event time"
        minutes % 1440 == 0 -> {
            val days = minutes / 1440
            "$days ${if (days == 1) "day" else "days"} before"
        }
        minutes % 60 == 0 -> {
            val hours = minutes / 60
            "$hours ${if (hours == 1) "hour" else "hours"} before"
        }
        else -> "$minutes minutes before"
    }
}
