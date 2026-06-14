package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun serialize(): String = "$latitude,$longitude,$altitude,$timestamp"

    companion object {
        fun deserialize(str: String): GpsPoint? {
            val parts = str.split(",")
            if (parts.size < 2) return null
            return GpsPoint(
                latitude = parts[0].toDoubleOrNull() ?: 0.0,
                longitude = parts[1].toDoubleOrNull() ?: 0.0,
                altitude = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0,
                timestamp = parts.getOrNull(3)?.toLongOrNull() ?: 0L
            )
        }
    }
}

// Helper to serialize list of points
fun List<GpsPoint>.serializePoints(): String = joinToString(";") { it.serialize() }
fun String.deserializePoints(): List<GpsPoint> {
    if (isEmpty()) return emptyList()
    return split(";").mapNotNull { GpsPoint.deserialize(it) }
}

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceKm: Double,
    val avgSpeedKmH: Double,
    val maxSpeedKmH: Double,
    val caloriesBurned: Int,
    val elevationGainMeters: Int,
    val rideType: String, // Road, MTB, City
    val gpsPathData: String // Serialized GPS points
)
