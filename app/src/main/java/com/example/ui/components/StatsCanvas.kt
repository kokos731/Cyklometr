package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Workout
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatsCanvas(
    workouts: List<Workout>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (workouts.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Brak danych wykresu",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Wykonaj treningi, aby zobaczyć historię",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            // Take last 7 rides for chart
            val lastRides = workouts.take(7).reversed()
            val maxDistance = lastRides.maxOfOrNull { it.distanceKm }?.coerceAtLeast(5.0) ?: 10.0

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ostatnie treningi (Dystans w km)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 16.dp, bottom = 24.dp, start = 24.dp, end = 16.dp)
                ) {
                    val spacing = size.width / (lastRides.size.coerceAtLeast(1))
                    val barWidth = (spacing * 0.4f).coerceIn(16.dp.toPx(), 48.dp.toPx())

                    // Draw grid markers
                    val steps = 4
                    for (step in 0..steps) {
                        val frac = step / steps.toFloat()
                        val y = size.height * (1f - frac)
                        val valLabel = (maxDistance * frac)

                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )

                        // Draw label text via Android Native Canvas
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format("%.1f", valLabel),
                            -16.dp.toPx(),
                            y + 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 10.sp.toPx()
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )
                    }

                    // Draw bars
                    lastRides.forEachIndexed { index, workout ->
                        val barHeight = (workout.distanceKm / maxDistance * size.height).toFloat().coerceAtLeast(4f)
                        val x = index * spacing + (spacing - barWidth) / 2
                        val y = size.height - barHeight

                        // Vertical bar gradients
                        val gradientBrush = Brush.verticalGradient(
                            colors = listOf(secondary, primary),
                            startY = y,
                            endY = size.height
                        )

                        drawRoundRect(
                            brush = gradientBrush,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Draw distance label inside / above bar
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format("%.1f", workout.distanceKm),
                            x + barWidth / 2,
                            y - 6.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 9.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                        )

                        // Draw date label
                        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
                        val dateFormatted = sdf.format(Date(workout.startTime))

                        drawContext.canvas.nativeCanvas.drawText(
                            dateFormatted,
                            x + barWidth / 2,
                            size.height + 15.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 9.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}
