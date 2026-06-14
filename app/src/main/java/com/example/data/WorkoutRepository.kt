package com.example.data

import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {
    val allWorkouts: Flow<List<Workout>> = workoutDao.getAllWorkouts()
    val totalDistance: Flow<Double?> = workoutDao.getTotalDistance()
    val totalDuration: Flow<Long?> = workoutDao.getTotalDuration()
    val allPlannedRoutes: Flow<List<PlannedRoute>> = workoutDao.getAllPlannedRoutes()

    suspend fun insert(workout: Workout) {
        workoutDao.insertWorkout(workout)
    }

    suspend fun delete(id: Int) {
        workoutDao.deleteWorkout(id)
    }

    suspend fun insertPlannedRoute(route: PlannedRoute) {
        workoutDao.insertPlannedRoute(route)
    }

    suspend fun deletePlannedRoute(id: Int) {
        workoutDao.deletePlannedRoute(id)
    }
}
