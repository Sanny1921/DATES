package com.nevermiss.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevermiss.app.data.Category
import com.nevermiss.app.data.Event
import com.nevermiss.app.data.EventStatus
import com.nevermiss.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    events: List<Event>,
    onRestoreEvent: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val completedEvents = remember(events, selectedCategory) {
        events.filter { it.status == EventStatus.COMPLETED }
            .filter { selectedCategory == null || it.category == selectedCategory }
    }

    val totalCompletedCount = remember(events) {
        events.count { it.status == EventStatus.COMPLETED }
    }

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
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COMPLETION ARCHIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = UrgencyGreen
                            )
                            Text(
                                text = "Past Accomplishments",
                                style = MaterialTheme.typography.headlineLarge,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = UrgencyGreenBg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = UrgencyGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$totalCompletedCount Done",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = UrgencyGreen
                                )
                            }
                        }
                    }
                }
            }

            // Stats Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(UrgencyGreenBg, MaterialTheme.shapes.medium)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$totalCompletedCount",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = UrgencyGreen
                            )
                            Text("Resolved", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(BrandOrangeLight, MaterialTheme.shapes.medium)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "100%",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandOrangeDark
                            )
                            Text("Local Safe", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(SurfaceSubtle, MaterialTheme.shapes.medium)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "0 kB",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text("Cloud Leak", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        }
                    }
                }
            }

            // Category Filter Row
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All ($totalCompletedCount)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UrgencyGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(Category.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UrgencyGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Completed List Items
            if (completedEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No archived events yet", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                            Text("Completed milestones will appear here for reference.", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                }
            } else {
                items(completedEvents, key = { it.id }) { event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = UrgencyGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                val completedTimeStr = remember(event.completedAtEpochMillis) {
                                    event.completedAtEpochMillis?.let {
                                        Instant.ofEpochMilli(it)
                                            .atZone(ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a"))
                                    } ?: event.targetDate
                                }
                                Text(
                                    text = "Completed: $completedTimeStr",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            // Actions: Restore / Delete
                            IconButton(onClick = { onRestoreEvent(event) }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore to pending", tint = BrandOrange)
                            }
                            IconButton(onClick = { onDeleteEvent(event) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
