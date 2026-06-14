package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GpsPoint
import com.example.data.Workout
import com.example.data.WorkoutRepository
import com.example.data.serializePoints
import com.google.android.gms.location.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class TrackingState {
    IDLE,
    TRACKING,
    PAUSED,
    STOPPED_SUMMARY
}

class TrackerViewModel(
    private val repository: WorkoutRepository,
    private val context: Context
) : ViewModel() {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // UI state flows
    private val _trackingState = MutableStateFlow(TrackingState.IDLE)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm.asStateFlow()

    private val _currentSpeedKmH = MutableStateFlow(0.0)
    val currentSpeedKmH: StateFlow<Double> = _currentSpeedKmH.asStateFlow()

    private val _avgSpeedKmH = MutableStateFlow(0.0)
    val avgSpeedKmH: StateFlow<Double> = _avgSpeedKmH.asStateFlow()

    private val _maxSpeedKmH = MutableStateFlow(0.0)
    val maxSpeedKmH: StateFlow<Double> = _maxSpeedKmH.asStateFlow()

    private val _elevationGainMeters = MutableStateFlow(0)
    val elevationGainMeters: StateFlow<Int> = _elevationGainMeters.asStateFlow()

    private val _currentAltitudeMeters = MutableStateFlow(200.0)
    val currentAltitudeMeters: StateFlow<Double> = _currentAltitudeMeters.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned.asStateFlow()

    private val _gpsPoints = MutableStateFlow<List<GpsPoint>>(emptyList())
    val gpsPoints: StateFlow<List<GpsPoint>> = _gpsPoints.asStateFlow()

    private val _rideType = MutableStateFlow("Szosowy")
    val rideType: StateFlow<String> = _rideType.asStateFlow()

    private val _isSimulatorMode = MutableStateFlow(true) // Default to true so it works in browser/emulator instantly!
    val isSimulatorMode: StateFlow<Boolean> = _isSimulatorMode.asStateFlow()

    // Database flow for previous rides
    val allWorkouts: StateFlow<List<Workout>> = repository.allWorkouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPlannedRoutes: StateFlow<List<com.example.data.PlannedRoute>> = repository.allPlannedRoutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalDistance: StateFlow<Double?> = repository.totalDistance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalDuration: StateFlow<Long?> = repository.totalDuration
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    // Internal trackers
    private var timerJob: Job? = null
    private var simulatorJob: Job? = null
    private var lastLocation: Location? = null
    private var startTimeMillis: Long = 0L
    private var isGpsRegistered = false

    // Scenic Route (Kraków to Tyniec Abbey along Wisła) Coordinates for simulation
    private val scenicRoute = listOf(
        GpsPoint(50.0541, 19.9354, 200.0), // Wawel Castle (Start)
        GpsPoint(50.0525, 19.9212, 199.5), // Dębnicki Bridge
        GpsPoint(50.0505, 19.9070, 202.0), // Zwierzyniec bend
        GpsPoint(50.0461, 19.8925, 201.2), // Przegorzały view
        GpsPoint(50.0392, 19.8732, 203.0), // Forest side
        GpsPoint(50.0354, 19.8450, 204.8), // Kryspinów area path
        GpsPoint(50.0336, 19.8155, 205.0), // Kolna Kayak Course
        GpsPoint(50.0210, 19.8080, 215.0), // Approaching forest hills
        GpsPoint(50.0152, 19.8021, 232.0)  // Tyniec Abbey (Finish)
    )

    private var currentSimIndex = 0
    private var simProgressFraction = 0.0

    fun setRideType(type: String) {
        _rideType.value = type
    }

    fun setSimulatorMode(enabled: Boolean) {
        _isSimulatorMode.value = enabled
        if (_trackingState.value == TrackingState.TRACKING) {
            // Re-apply tracking mode since it shifted
            stopTrackingServices()
            startTrackingServices()
        }
    }

    fun startRide() {
        if (_trackingState.value == TrackingState.IDLE) {
            _trackingState.value = TrackingState.TRACKING
            startTimeMillis = System.currentTimeMillis()
            _durationSeconds.value = 0
            _distanceKm.value = 0.0
            _currentSpeedKmH.value = 0.0
            _avgSpeedKmH.value = 0.0
            _maxSpeedKmH.value = 0.0
            _elevationGainMeters.value = 0
            _gpsPoints.value = emptyList()
            _caloriesBurned.value = 0
            _currentAltitudeMeters.value = 200.0

            currentSimIndex = 0
            simProgressFraction = 0.0
            lastLocation = null

            startTimer()
            startTrackingServices()
        }
    }

    fun pauseRide() {
        if (_trackingState.value == TrackingState.TRACKING) {
            _trackingState.value = TrackingState.PAUSED
            stopTimer()
            stopTrackingServices()
        }
    }

    fun resumeRide() {
        if (_trackingState.value == TrackingState.PAUSED) {
            _trackingState.value = TrackingState.TRACKING
            startTimer()
            startTrackingServices()
        }
    }

    fun stopRide() {
        if (_trackingState.value == TrackingState.TRACKING || _trackingState.value == TrackingState.PAUSED) {
            _trackingState.value = TrackingState.STOPPED_SUMMARY
            stopTimer()
            stopTrackingServices()
        }
    }

    fun saveRide() {
        viewModelScope.launch {
            val workout = Workout(
                startTime = startTimeMillis,
                endTime = System.currentTimeMillis(),
                durationSeconds = _durationSeconds.value,
                distanceKm = ( _distanceKm.value * 100.0).roundToInt() / 100.0,
                avgSpeedKmH = ( _avgSpeedKmH.value * 10.0).roundToInt() / 10.0,
                maxSpeedKmH = ( _maxSpeedKmH.value * 10.0).roundToInt() / 10.0,
                caloriesBurned = _caloriesBurned.value,
                elevationGainMeters = _elevationGainMeters.value,
                rideType = _rideType.value,
                gpsPathData = _gpsPoints.value.serializePoints()
            )
            repository.insert(workout)
            resetTracker()
        }
    }

    fun discardRide() {
        resetTracker()
    }

    fun deleteWorkout(id: Int) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun savePlannedRoute(route: com.example.data.PlannedRoute) {
        viewModelScope.launch {
            repository.insertPlannedRoute(route)
        }
    }

    fun deletePlannedRoute(id: Int) {
        viewModelScope.launch {
            repository.deletePlannedRoute(id)
        }
    }

    private fun resetTracker() {
        _trackingState.value = TrackingState.IDLE
        _durationSeconds.value = 0
        _distanceKm.value = 0.0
        _currentSpeedKmH.value = 0.0
        _avgSpeedKmH.value = 0.0
        _maxSpeedKmH.value = 0.0
        _elevationGainMeters.value = 0
        _caloriesBurned.value = 0
        _gpsPoints.value = emptyList()
        currentSimIndex = 0
        simProgressFraction = 0.0
        lastLocation = null
        stopTimer()
        stopTrackingServices()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _durationSeconds.value += 1
                recalculateStats()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun startTrackingServices() {
        if (_isSimulatorMode.value) {
            startSimulation()
        } else {
            startRealGps()
        }
    }

    private fun stopTrackingServices() {
        stopSimulation()
        stopRealGps()
    }

    private fun startSimulation() {
        simulatorJob?.cancel()
        simulatorJob = viewModelScope.launch {
            while (true) {
                // Every 2 seconds, move along the simulated path
                delay(2000)
                updateSimulatorPosition()
            }
        }
    }

    private fun stopSimulation() {
        simulatorJob?.cancel()
        simulatorJob = null
    }

    private fun updateSimulatorPosition() {
        if (currentSimIndex >= scenicRoute.size - 1) {
            // Loop back or complete
            currentSimIndex = 0
            simProgressFraction = 0.0
        }

        // Interpolate slightly
        simProgressFraction += 0.05
        if (simProgressFraction >= 1.0) {
            simProgressFraction = 0.0
            currentSimIndex++
        }

        val startPt = scenicRoute[currentSimIndex]
        val endPt = scenicRoute[(currentSimIndex + 1).coerceAtMost(scenicRoute.size - 1)]

        val currentLat = startPt.latitude + (endPt.latitude - startPt.latitude) * simProgressFraction
        val currentLng = startPt.longitude + (endPt.longitude - startPt.longitude) * simProgressFraction
        val currentAlt = startPt.altitude + (endPt.altitude - startPt.altitude) * simProgressFraction

        // Add a bit of natural random speed variations (20 km/h to 32 km/h)
        val variations = (Math.random() - 0.5) * 4.0
        val baseSpeed = when (_rideType.value) {
            "Szosowy" -> 26.0
            "Górski (MTB)" -> 18.0
            else -> 22.0
        }
        val targetSpeed = (baseSpeed + variations).coerceAtLeast(10.0)
        _currentSpeedKmH.value = targetSpeed

        val newPoint = GpsPoint(currentLat, currentLng, currentAlt)
        val currentList = _gpsPoints.value.toMutableList()

        if (currentList.isNotEmpty()) {
            val lastPt = currentList.last()
            val results = FloatArray(1)
            Location.distanceBetween(
                lastPt.latitude, lastPt.longitude,
                newPoint.latitude, newPoint.longitude,
                results
            )
            val stepDistKm = results[0] / 1000.0
            _distanceKm.value += stepDistKm

            // Check altitude details
            val diffAlt = newPoint.altitude - lastPt.altitude
            if (diffAlt > 0) {
                _elevationGainMeters.value += diffAlt.roundToInt()
            }
        } else {
            _currentAltitudeMeters.value = currentAlt
        }

        currentList.add(newPoint)
        _gpsPoints.value = currentList
        _currentAltitudeMeters.value = currentAlt
    }

    @SuppressLint("MissingPermission")
    private fun startRealGps() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(1500)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isGpsRegistered = true
        } catch (e: Exception) {
            Log.e("TrackerViewModel", "Failed to start live GPS: ${e.message}")
        }
    }

    private fun stopRealGps() {
        if (isGpsRegistered) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isGpsRegistered = false
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                handleNewLocation(location)
            }
        }
    }

    private fun handleNewLocation(location: Location) {
        val speedKmh = location.speed * 3.6
        _currentSpeedKmH.value = if (speedKmh > 1.0) speedKmh else 0.0
        _currentAltitudeMeters.value = location.altitude

        val newPoint = GpsPoint(location.latitude, location.longitude, location.altitude)
        val currentList = _gpsPoints.value.toMutableList()

        if (currentList.isNotEmpty()) {
            val lastPt = currentList.last()
            val results = FloatArray(1)
            Location.distanceBetween(
                lastPt.latitude, lastPt.longitude,
                newPoint.latitude, newPoint.longitude,
                results
            )
            val stepDistKm = results[0] / 1000.0
            _distanceKm.value += stepDistKm

            val diffAlt = newPoint.altitude - lastPt.altitude
            if (diffAlt > 0) {
                _elevationGainMeters.value += diffAlt.roundToInt()
            }
        }

        currentList.add(newPoint)
        _gpsPoints.value = currentList
    }

    private fun recalculateStats() {
        val duration = _durationSeconds.value
        val dist = _distanceKm.value

        if (duration > 0) {
            val avgSpeed = (dist / (duration / 3600.0))
            _avgSpeedKmH.value = if (avgSpeed.isFinite() && avgSpeed > 0.0) avgSpeed else 0.0
        }

        val curSpeed = _currentSpeedKmH.value
        if (curSpeed > _maxSpeedKmH.value) {
            _maxSpeedKmH.value = curSpeed
        }

        // Calories estimate: Standard cyclocomputer calculation: 
        // ~35 kcal per km for Szosowy, 45 kcal per km for MTB, 38 kcal for City
        val factor = when (_rideType.value) {
            "Szosowy" -> 35.0
            "Górski (MTB)" -> 45.0
            else -> 38.0
        }
        _caloriesBurned.value = (dist * factor).roundToInt()
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        stopTrackingServices()
    }
}

class TrackerViewModelFactory(
    private val repository: WorkoutRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            return TrackerViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
