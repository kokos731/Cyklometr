package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planned_routes")
data class PlannedRoute(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val distanceKm: Double,
    val estimatedTimeMins: Int,
    val pointsData: String
)
