package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Workout
import com.example.data.deserializePoints
import com.example.ui.components.GpsPathCanvas
import com.example.ui.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun HistoryScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val workouts by viewModel.allWorkouts.collectAsStateWithLifecycle()
    var selectedWorkoutIdForMap by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (workouts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("empty_history_column"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = "No tracks",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.empty_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.empty_history_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Twoje Ostatnie Trasy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(workouts, key = { it.id }) { workout ->
                    val isExpanded = selectedWorkoutIdForMap == workout.id

                    WorkoutHistoryItem(
                        workout = workout,
                        isExpanded = isExpanded,
                        onExpandToggle = {
                            selectedWorkoutIdForMap = if (isExpanded) null else workout.id
                        },
                        onDelete = {
                            viewModel.deleteWorkout(workout.id)
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryItem(
    workout: Workout,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy, HH:mm", Locale("pl", "PL"))
    val dateText = sdf.format(Date(workout.startTime))

    val rideIcon = when (workout.rideType) {
        "Szosowy" -> Icons.Default.DirectionsBike
        "Górski (MTB)" -> Icons.Default.Terrain
        else -> Icons.Default.ElectricBike
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onExpandToggle() }
            .testTag("workout_item_${workout.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BoxDefaults.thinOutlineBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = rideIcon,
                        contentDescription = workout.rideType,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (workout.rideType.isNotEmpty()) workout.rideType else "Trening rowerowy",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.tagDeleteButton(workout.id)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Workout",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HistoryMiniStat(
                    label = stringResource(R.string.lbl_distance),
                    value = String.format("%.2f", workout.distanceKm) + " ${stringResource(R.string.distance_unit)}"
                )
                HistoryMiniStat(
                    label = stringResource(R.string.lbl_duration),
                    value = formatHistoryDuration(workout.durationSeconds)
                )
                HistoryMiniStat(
                    label = "Śr. Prędkość",
                    value = String.format("%.1f", workout.avgSpeedKmH) + " ${stringResource(R.string.speed_unit)}"
                )
            }

            // Expand Map Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Mapa przebytej trasy:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val points = remember(workout.gpsPathData) {
                        workout.gpsPathData.deserializePoints()
                    }

                    GpsPathCanvas(
                        points = points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HistoryExpansionStat(label = "Spalone kalorie", value = "${workout.caloriesBurned} kcal", icon = Icons.Default.LocalFireDepartment)
                        HistoryExpansionStat(label = "Suma podjazdów", value = "${workout.elevationGainMeters} m", icon = Icons.Default.FilterHdr)
                        HistoryExpansionStat(label = "Maks. prędkość", value = "${workout.maxSpeedKmH} km/h", icon = Icons.Default.Speed)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryMiniStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun HistoryExpansionStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
            Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
        }
    }
}

private fun formatHistoryDuration(seconds: Long): String {
    val hrs = TimeUnit.SECONDS.toHours(seconds)
    val mins = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val secs = seconds % 60
    return if (hrs > 0) {
        "${hrs}h ${mins}m"
    } else {
        "${mins}m ${secs}s"
    }
}

fun Modifier.tagDeleteButton(id: Int): Modifier = this.testTag("delete_workout_$id")
