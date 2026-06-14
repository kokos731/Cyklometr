package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.WorkoutRepository
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.TrackerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.TrackerViewModel
import com.example.ui.viewmodel.TrackerViewModelFactory

import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val repository = remember { WorkoutRepository(database.workoutDao()) }
            
            // Create ViewModel with Custom Factory inside Compose context
            val factory = remember { TrackerViewModelFactory(repository, context.applicationContext) }
            val viewModel: TrackerViewModel = viewModel(factory = factory)

            var activeTab by rememberSaveable { mutableStateOf(0) }
            var currentThemeName by rememberSaveable { mutableStateOf(com.example.ui.theme.AppTheme.NEON.name) }
            val currentTheme = com.example.ui.theme.AppTheme.valueOf(currentThemeName)

            MyApplicationTheme(selectedTheme = currentTheme) {
                Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .testTag("bottom_navigation"),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = activeTab == 0,
                                onClick = { activeTab = 0 },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == 0) Icons.Default.DirectionsBike else Icons.Outlined.DirectionsBike,
                                        contentDescription = "Pulpit"
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.tab_tracker),
                                        fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("tab_tracker")
                            )

                            NavigationBarItem(
                                selected = activeTab == 1,
                                onClick = { activeTab = 1 },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == 1) Icons.Default.History else Icons.Outlined.History,
                                        contentDescription = "Historia"
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.tab_history),
                                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("tab_history")
                            )

                            NavigationBarItem(
                                selected = activeTab == 2,
                                onClick = { activeTab = 2 },
                                icon = {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Map,
                                        contentDescription = "Planer"
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.tab_planner),
                                        fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("tab_planner")
                            )

                            NavigationBarItem(
                                selected = activeTab == 3,
                                onClick = { activeTab = 3 },
                                icon = {
                                    Icon(
                                        imageVector = if (activeTab == 3) Icons.Default.BarChart else Icons.Outlined.BarChart,
                                        contentDescription = "Statystyki"
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.tab_stats),
                                        fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("tab_stats")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                    ) {
                        when (activeTab) {
                            0 -> TrackerScreen(viewModel = viewModel)
                            1 -> HistoryScreen(viewModel = viewModel)
                            2 -> PlannerScreen(viewModel = viewModel)
                            3 -> StatsScreen(
                                viewModel = viewModel,
                                currentTheme = currentTheme,
                                onThemeChange = { currentThemeName = it.name }
                            )
                        }
                    }
                }
            }
        }
    }
}
