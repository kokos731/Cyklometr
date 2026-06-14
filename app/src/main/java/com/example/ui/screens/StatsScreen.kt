package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.Workout
import com.example.ui.components.StatsCanvas
import com.example.ui.theme.AppTheme
import com.example.ui.viewmodel.TrackerViewModel
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    viewModel: TrackerViewModel,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val workouts by viewModel.allWorkouts.collectAsStateWithLifecycle()
    val totalDistanceSum by viewModel.totalDistance.collectAsStateWithLifecycle()
    val totalDurationSum by viewModel.totalDuration.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    // Calculated totals
    val totalRidesCount = workouts.size
    val distanceKm = totalDistanceSum ?: 0.0
    val durationSec = totalDurationSum ?: 0L
    val totalCalories = workouts.sumOf { it.caloriesBurned }
    val totalElevation = workouts.sumOf { it.elevationGainMeters }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Selection Section
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Personalizacja",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppTheme.entries.forEach { theme ->
                    val isSelected = currentTheme == theme
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onThemeChange(theme) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Podsumowanie Treningów",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 2x2 Grid for Overall Cumulative Stats
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OverallStatCard(
                    title = "Suma Treningów",
                    value = "$totalRidesCount",
                    icon = Icons.Default.DirectionsBike,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                OverallStatCard(
                    title = "Całkowity dystans",
                    value = String.format("%.1f km", distanceKm),
                    icon = Icons.Default.Navigation,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OverallStatCard(
                    title = "Czas w siodle",
                    value = formatTotalDuration(durationSec),
                    icon = Icons.Default.Timer,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                OverallStatCard(
                    title = "Spalone kalorie",
                    value = "$totalCalories kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color.Red,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // CUSTOM BAR CHART CANVAS
        Text(
            text = "Historia Aktywności Wykres",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        StatsCanvas(
            workouts = workouts,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        // ACHIEVEMENTS / REWARDS BADGES
        Text(
            text = "Osiągnięcia Rowerowe",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Achievement 1: First Spin
            AchievementItem(
                title = "Pierwszy obrót",
                description = "Ukończ swój pierwszy trening rowerowy",
                unlocked = totalRidesCount >= 1,
                icon = Icons.Default.DirectionsBike
            )
            // Achievement 2: Distance Champion single ride (>= 15km)
            val hasSingle15Km = workouts.any { it.distanceKm >= 15.0 }
            AchievementItem(
                title = "Dystansowy Wojownik",
                description = "Przejedź dystans co najmniej 15 km w jednym treningu",
                unlocked = hasSingle15Km,
                icon = Icons.Default.EmojiEvents
            )
            // Achievement 3: Max Speed (>= 30 km/h)
            val hasSpeedyRide = workouts.any { it.maxSpeedKmH >= 30.0 }
            AchievementItem(
                title = "Demon Prędkości",
                description = "Osiągnij prędkość maksymalną powyżej 30 km/h",
                unlocked = hasSpeedyRide,
                icon = Icons.Default.Speed
            )
            // Achievement 4: Total Elevation (>= 100m)
            AchievementItem(
                title = "Górski Pogromca",
                description = "Pokonaj łącznie ponad 100 m przewyższenia",
                unlocked = totalElevation >= 100,
                icon = Icons.Default.FilterHdr
            )
            // Achievement 5: Endurance ride count >= 3
            AchievementItem(
                title = "Mistrz Wytrwałości",
                description = "Ukończ co najmniej 3 pełne zapisane trasy",
                unlocked = totalRidesCount >= 3,
                icon = Icons.Default.WorkspacePremium
            )
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun OverallStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BoxDefaults.thinOutlineBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AchievementItem(
    title: String,
    description: String,
    unlocked: Boolean,
    icon: ImageVector
) {
    val alpha = if (unlocked) 1.0f else 0.4f
    val strokeColor = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val circleBg = if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f)
    val iconTint = if (unlocked) MaterialTheme.colorScheme.primary else Color.Gray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (unlocked) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .border(1.dp, strokeColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("achievement_${title.replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(circleBg, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unlocked) icon else Icons.Default.Lock,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }

            if (unlocked) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "UKOŃCZONE",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

private fun formatTotalDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    return if (hrs > 0) {
        "${hrs}h ${mins}m"
    } else {
        "${mins} min"
    }
}
