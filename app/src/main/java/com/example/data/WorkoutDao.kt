package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startTime DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Int)

    @Query("SELECT SUM(distanceKm) FROM workouts")
    fun getTotalDistance(): Flow<Double?>

    @Query("SELECT SUM(durationSeconds) FROM workouts")
    fun getTotalDuration(): Flow<Long?>

    @Query("SELECT * FROM planned_routes ORDER BY id DESC")
    fun getAllPlannedRoutes(): Flow<List<PlannedRoute>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedRoute(route: PlannedRoute)

    @Query("DELETE FROM planned_routes WHERE id = :id")
    suspend fun deletePlannedRoute(id: Int)
}
