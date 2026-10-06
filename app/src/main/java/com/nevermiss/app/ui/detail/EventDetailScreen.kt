package com.nevermiss.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nevermiss.app.data.Event
import com.nevermiss.app.data.EventStatus
import com.nevermiss.app.data.Severity
import com.nevermiss.app.logic.EventBucketingLogic
import com.nevermiss.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: Event,
    onBack: () -> Unit,
    onToggleDone: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val countdown = remember(event.targetEpochMillis) {
        EventBucketingLogic.formatCountdownLabel(event.targetEpochMillis)
    }
    val badge = remember(event.targetEpochMillis, event.status) {
        EventBucketingLogic.getUrgencyBadge(event.targetEpochMillis, event.status)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CanvasBackground,
        topBar = {
            TopAppBar(
                title = { Text("Event Details", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onDeleteEvent(event) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Event", tint = UrgencyRed)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isCompleted = event.status == EventStatus.COMPLETED
                    Button(
                        onClick = { onToggleDone(event) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) UrgencyGreen else BrandOrange
                        )
                    ) {
                        Icon(
                            if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCompleted) "Completed (Tap to Reopen)" else "Mark as Completed",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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
            // Main Overview Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = BrandOrangeLight
                            ) {
                                Text(
                                    text = event.category.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandOrangeDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when (badge.tier) {
                                    com.nevermiss.app.logic.UrgencyTier.OVERDUE,
                                    com.nevermiss.app.logic.UrgencyTier.TODAY -> UrgencyRedBg
                                    com.nevermiss.app.logic.UrgencyTier.SOON -> UrgencyAmberBg
                                    com.nevermiss.app.logic.UrgencyTier.UPCOMING,
                                    com.nevermiss.app.logic.UrgencyTier.COMPLETED -> UrgencyGreenBg
                                }
                            ) {
                                Text(
                                    text = badge.text,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when (badge.tier) {
                                        com.nevermiss.app.logic.UrgencyTier.OVERDUE,
                                        com.nevermiss.app.logic.UrgencyTier.TODAY -> UrgencyRed
                                        com.nevermiss.app.logic.UrgencyTier.SOON -> UrgencyAmber
                                        com.nevermiss.app.logic.UrgencyTier.UPCOMING,
                                        com.nevermiss.app.logic.UrgencyTier.COMPLETED -> UrgencyGreen
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceSubtle, MaterialTheme.shapes.medium)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TARGET DATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text(event.targetDate, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DUE TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text(event.targetTime, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PRIORITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Text(event.priority.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandOrange)
                            }
                        }
                    }
                }
            }

            // Multi-Stage Reminder Cascade Pipeline
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OFFLINE ALARM PIPELINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${event.reminders.size} Scheduled Stages",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandOrange
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (event.reminders.isEmpty()) {
                            Text(
                                text = "No reminder triggers attached to this date.",
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                event.reminders.forEachIndexed { index, rem ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (rem.isDelivered) SurfaceSubtle else BrandOrangeLight.copy(alpha = 0.5f),
                                                MaterialTheme.shapes.medium
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(
                                                    if (rem.isDelivered) UrgencyGreen else BrandOrange,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (rem.isDelivered) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            } else {
                                                Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rem.label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            if (rem.note.isNotBlank()) {
                                                Text(
                                                    text = rem.note,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = when (rem.severity) {
                                                Severity.CRITICAL -> UrgencyRedBg
                                                Severity.WARN -> UrgencyOrangeBg
                                                Severity.NORMAL -> SurfaceWhite
                                            }
                                        ) {
                                            Text(
                                                text = if (rem.isDelivered) "Delivered" else rem.severity.name.lowercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rem.isDelivered) UrgencyGreen else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notes Section
            if (event.notes.isNotBlank()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("NOTES & DETAILS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = event.notes,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
