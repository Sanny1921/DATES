package com.nevermiss.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevermiss.app.data.Event
import com.nevermiss.app.data.EventStatus
import com.nevermiss.app.ui.home.EventCard
import com.nevermiss.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    onToggleDone: (Event) -> Unit,
    onAddDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Group events by target date string "YYYY-MM-DD"
    val eventsByDate = remember(events) {
        events.groupBy { it.targetDate }
    }

    val selectedDateStr = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    val selectedDayEvents = eventsByDate[selectedDateStr] ?: emptyList()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Screen Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Calendar Agenda",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "View and schedule commitments across the monthly timeline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Month Navigation Bar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = TextPrimary)
                            }

                            Text(
                                text = "${currentYearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentYearMonth.year}",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )

                            Row {
                                TextButton(onClick = {
                                    val now = LocalDate.now()
                                    currentYearMonth = YearMonth.from(now)
                                    selectedDate = now
                                }) {
                                    Text("Today", color = BrandOrange, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TextPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Day of week headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            for (day in daysOfWeek) {
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Month Grid Days
                        val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value // 1 (Mon) to 7 (Sun)
                        val lengthOfMonth = currentYearMonth.lengthOfMonth()
                        val totalCells = ((firstDayOfWeek - 1 + lengthOfMonth + 6) / 7) * 7

                        for (row in 0 until (totalCells / 7)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0..6) {
                                    val cellIndex = row * 7 + col
                                    val dayNumber = cellIndex - (firstDayOfWeek - 1) + 1

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayNumber in 1..lengthOfMonth) {
                                            val cellDate = currentYearMonth.atDay(dayNumber)
                                            val cellDateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            val isSelected = cellDate == selectedDate
                                            val isToday = cellDate == LocalDate.now()
                                            val dateEvents = eventsByDate[cellDateStr] ?: emptyList()

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) BrandOrange
                                                        else if (isToday) BrandOrangeLight
                                                        else Color.Transparent
                                                    )
                                                    .border(
                                                        width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                        color = if (isToday && !isSelected) BrandOrange else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { selectedDate = cellDate }
                                            ) {
                                                Text(
                                                    text = "$dayNumber",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) Color.White else TextPrimary
                                                )

                                                // Event Indicators
                                                if (dateEvents.isNotEmpty()) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    ) {
                                                        val hasUrgent = dateEvents.any { it.status != EventStatus.COMPLETED && it.priority.level >= 3 }
                                                        val dotColor = if (isSelected) Color.White
                                                        else if (hasUrgent) UrgencyRed
                                                        else BrandOrange

                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .background(dotColor, CircleShape)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Daily Agenda Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AGENDA FOR ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    IconButton(onClick = { onAddDateClick(selectedDate) }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add on this date", tint = BrandOrange)
                    }
                }
            }

            if (selectedDayEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No deadlines scheduled for this date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { onAddDateClick(selectedDate) }) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = BrandOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Event on this date", color = BrandOrange)
                            }
                        }
                    }
                }
            } else {
                items(selectedDayEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        isUrgent = event.priority.level >= 3,
                        onEventClick = { onEventClick(event) },
                        onToggleDone = { onToggleDone(event) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
