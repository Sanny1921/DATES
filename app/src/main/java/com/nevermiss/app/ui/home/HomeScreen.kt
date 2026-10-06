package com.nevermiss.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevermiss.app.data.Category
import com.nevermiss.app.data.Event
import com.nevermiss.app.data.EventStatus
import com.nevermiss.app.logic.EventBucketingLogic
import com.nevermiss.app.logic.EventLabels
import com.nevermiss.app.ui.theme.*

@Composable
fun HomeScreen(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    onToggleDone: (Event) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val buckets = remember(events) {
        EventBucketingLogic.bucketEvents(events)
    }
    val counts = remember(buckets) {
        EventBucketingLogic.calculateCounts(buckets)
    }

    val activeList = remember(events, selectedCategory, searchQuery) {
        events.filter { it.status != EventStatus.COMPLETED }
            .filter { selectedCategory == null || it.category == selectedCategory }
            .filter {
                if (searchQuery.isBlank()) true
                else it.title.contains(searchQuery, ignoreCase = true) ||
                        it.notes.contains(searchQuery, ignoreCase = true)
            }
    }

    val urgentAndToday = remember(activeList, buckets) {
        activeList.filter { it in buckets.overdue || it in buckets.today }
    }

    val upcoming = remember(activeList, buckets) {
        activeList.filter { it in buckets.upcoming }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = BrandOrange,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header: Branding & Offline Badge
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Never",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Miss",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = BrandOrange
                                )
                            }
                            Text(
                                text = "Your personal assistant for important dates",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        // Local Offline Badge
                        Surface(
                            shape = CircleShape,
                            color = UrgencyGreenBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, UrgencyGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(UrgencyGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "100% Offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = UrgencyGreen
                                )
                            }
                        }
                    }
                }
            }

            // Week Progress Ring / Card
            item {
                WeekProgressCard(
                    counts = counts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search events, notes...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedBorderColor = BrandOrange,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    singleLine = true
                )
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
                            label = { Text("All (${counts.totalCount - counts.completedCount})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(Category.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = if (selectedCategory == cat) null else cat
                            },
                            label = { Text(cat.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Urgent / Today Section
            if (urgentAndToday.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "NEEDS ATTENTION TODAY",
                        badgeCount = urgentAndToday.size,
                        badgeColor = UrgencyRed
                    )
                }
                items(urgentAndToday, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        isUrgent = true,
                        onEventClick = { onEventClick(event) },
                        onToggleDone = { onToggleDone(event) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }

            // Upcoming Section
            item {
                SectionHeader(
                    title = "UPCOMING DEADLINES",
                    badgeCount = upcoming.size,
                    badgeColor = UrgencyAmber
                )
            }

            if (upcoming.isEmpty() && urgentAndToday.isEmpty()) {
                item {
                    EmptyStateCard(onAddClick = onAddClick)
                }
            } else {
                items(upcoming, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        isUrgent = false,
                        onEventClick = { onEventClick(event) },
                        onToggleDone = { onToggleDone(event) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badgeCount: Int,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Surface(
            shape = CircleShape,
            color = badgeColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = "$badgeCount",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun WeekProgressCard(
    counts: com.nevermiss.app.logic.BucketCounts,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Weekly Momentum",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${counts.completedCount} of ${counts.totalCount} commitments completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { counts.completionRatePercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(8.dp)
                        .clip(CircleShape),
                    color = BrandOrange,
                    trackColor = BorderSubtle
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .background(BrandOrangeLight, CircleShape)
            ) {
                Text(
                    text = "${counts.completionRatePercent}%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = BrandOrangeDark
                )
            }
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    isUrgent: Boolean,
    onEventClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countdown = remember(event.targetEpochMillis, event.status) {
        EventLabels.relative(event)
    }

    Card(
        modifier = modifier.clickable { onEventClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUrgent) UrgencyRed.copy(alpha = 0.4f) else BorderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUrgent) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tactile Checkbox
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier
                    .size(32.dp)
                    .border(2.dp, if (isUrgent) UrgencyRed else BrandOrange, CircleShape)
            ) {
                if (event.status == EventStatus.COMPLETED) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Done",
                        tint = UrgencyGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = SurfaceSubtle
                    ) {
                        Text(
                            text = event.category.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (isUrgent) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = UrgencyRedBg
                        ) {
                            Text(
                                text = "URGENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = UrgencyRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (event.notes.isNotBlank()) {
                    Text(
                        text = event.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (isUrgent) UrgencyRed else BrandOrange
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${event.targetDate} • ${event.targetTime} ($countdown)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isUrgent) UrgencyRed else TextSecondary
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EmptyStateCard(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = UrgencyGreen,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "All Caught Up!",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = "No pending deadlines requiring your attention right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule Important Date")
            }
        }
    }
}
