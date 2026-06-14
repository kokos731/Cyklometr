package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PlannedRoute
import com.example.ui.viewmodel.TrackerViewModel
import kotlin.math.sqrt
import kotlin.math.roundToInt

import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.events.MapEventsReceiver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PlannerScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val plannedRoutes by viewModel.allPlannedRoutes.collectAsStateWithLifecycle()
    var isPlanningMode by remember { mutableStateOf(false) }

    var currentPoints by remember { mutableStateOf(listOf<GeoPoint>()) }
    var routeName by remember { mutableStateOf("Nowa Trasa") }

    fun calculateDistance(points: List<GeoPoint>): Double {
        var distMeters = 0.0
        for (i in 0 until points.size - 1) {
            distMeters += points[i].distanceToAsDouble(points[i + 1])
        }
        return distMeters / 1000.0
    }

    if (isPlanningMode) {
        val dist = calculateDistance(currentPoints)
        val estTime = kotlin.math.max(1, (dist * 2.5).roundToInt())
        
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kreator Trasy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { isPlanningMode = false; currentPoints = emptyList() }) {
                    Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            OutlinedTextField(
                value = routeName,
                onValueChange = { routeName = it },
                label = { Text("Nazwa trasy") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dystans: ${String.format("%.1f", dist)} km", color = MaterialTheme.colorScheme.onSurface)
                Text("Szacz. czas: $estTime min", color = MaterialTheme.colorScheme.onSurface)
            }

            val context = LocalContext.current
            val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
            val mapView = remember {
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(13.0)
                    // Set an initial center (approximate center of Poland or last location)
                    controller.setCenter(GeoPoint(52.2297, 21.0122)) // Warsaw
                }
            }

            DisposableEffect(mapView) {
                mapView.onResume()
                onDispose { mapView.onPause() }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize(),
                    update = { map ->
                        map.overlays.clear()
                        
                        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                currentPoints = currentPoints + p
                                return true
                            }
                            override fun longPressHelper(p: GeoPoint): Boolean {
                                return false
                            }
                        })
                        map.overlays.add(eventsOverlay)

                        if (currentPoints.isNotEmpty()) {
                            val polyline = Polyline()
                            polyline.setPoints(currentPoints)
                            polyline.color = primaryColorArgb
                            polyline.width = 10f
                            map.overlays.add(polyline)

                            currentPoints.forEachIndexed { index, point ->
                                val marker = Marker(map)
                                marker.position = point
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                marker.title = if (index == 0) "Start" else "Punkt ${index + 1}"
                                map.overlays.add(marker)
                            }
                        }
                        map.invalidate()
                    }
                )
                if (currentPoints.isEmpty()) {
                    Text(
                        "Dotknij mapy, aby dodać punkty",
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color.White.copy(alpha=0.8f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { currentPoints = emptyList() }, modifier = Modifier.weight(1f)) {
                    Text("Wyczyść")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = currentPoints.size >= 2,
                    onClick = {
                        val route = PlannedRoute(
                            name = routeName,
                            distanceKm = dist,
                            estimatedTimeMins = estTime,
                            pointsData = currentPoints.joinToString(";") { "${it.latitude},${it.longitude}" }
                        )
                        viewModel.savePlannedRoute(route)
                        isPlanningMode = false
                        currentPoints = emptyList()
                        routeName = "Nowa Trasa"
                    }
                ) {
                    Text("Zapisz trasę")
                }
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { isPlanningMode = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj trasę")
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).padding(16.dp).fillMaxSize()) {
                Text(
                    text = "Planer Tras",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (plannedRoutes.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha=0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Brak zaplanowanych tras", color = MaterialTheme.colorScheme.onSurface)
                            Text("Dotknij +, aby zaplanować pierwszą trasę", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(plannedRoutes, key = { it.id }) { route ->
                            PlannedRouteCard(route, onDelete = { viewModel.deletePlannedRoute(route.id) })
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun PlannedRouteCard(route: PlannedRoute, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(route.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(String.format("%.1f km", route.distanceKm), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${route.estimatedTimeMins} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
