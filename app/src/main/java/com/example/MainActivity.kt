package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.scene3d.Interactive3DGlobeScreen
import com.example.ui.scene3d.Weather3DSceneView
import com.example.ui.screens.Atmos3DSplashOverlay
import com.example.ui.screens.ForecastsScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.SavedLocationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeatherAnalyticsScreen
import com.example.ui.sensor.rememberDeviceTiltState
import com.example.ui.theme.Atmos3DTheme
import com.example.viewmodel.AtmosDestination
import com.example.viewmodel.AtmosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: AtmosViewModel = viewModel(
                factory = AtmosViewModel.Factory(context)
            )
            Atmos3DApp(viewModel = viewModel)
        }
    }
}

@Composable
fun Atmos3DApp(
    viewModel: AtmosViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val effectiveScene = uiState.activeSceneOverride ?: uiState.weatherBundle.current.sceneType
    val tiltState by rememberDeviceTiltState(enabled = !uiState.showSplash)

    // Check existing location permission state on startup
    LaunchedEffect(context) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateLocationPermissionStatus(hasFine || hasCoarse)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.updateLocationPermissionStatus(granted)
        if (granted) {
            viewModel.detectDeviceGpsLocation(context)
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val requestLocationFlow = {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.detectDeviceGpsLocation(context)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // BackHandler for non-dashboard destinations or immersive mode
    BackHandler(enabled = uiState.isImmersiveMode || uiState.currentDestination != AtmosDestination.DASHBOARD) {
        if (uiState.isImmersiveMode) {
            viewModel.toggleImmersiveMode()
        } else {
            viewModel.navigateTo(AtmosDestination.DASHBOARD)
        }
    }

    Atmos3DTheme(
        themeMode = uiState.settings.themeMode,
        sceneType = effectiveScene
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 640.dp

            // Full-bleed real-time 3D weather environment backdrop
            Weather3DSceneView(
                sceneType = effectiveScene,
                graphicsQuality = uiState.settings.graphicsQuality,
                tiltVector = tiltState,
                interactiveTouch = uiState.isImmersiveMode || uiState.currentDestination == AtmosDestination.DASHBOARD,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("weather_3d_scene_backdrop")
            )

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isWideScreen && !uiState.isImmersiveMode) {
                        AtmosBottomNavigationBar(
                            currentDestination = uiState.currentDestination,
                            accentColor = Color(effectiveScene.accentColorHex),
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen && !uiState.isImmersiveMode) {
                        AtmosSideNavigationRail(
                            currentDestination = uiState.currentDestination,
                            accentColor = Color(effectiveScene.accentColorHex),
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Crossfade(
                            targetState = uiState.currentDestination,
                            label = "destination_crossfade"
                        ) { destination ->
                            when (destination) {
                                AtmosDestination.DASHBOARD -> {
                                    MainDashboardScreen(
                                        weatherBundle = uiState.weatherBundle,
                                        savedLocations = uiState.savedLocations,
                                        settings = uiState.settings,
                                        activeSceneOverride = uiState.activeSceneOverride,
                                        isRefreshing = uiState.isRefreshing,
                                        isImmersiveMode = uiState.isImmersiveMode,
                                        errorMessage = uiState.errorMessage,
                                        onToggleImmersiveMode = { viewModel.toggleImmersiveMode() },
                                        onRefreshWeather = { viewModel.refreshCurrentWeather() },
                                        onRequestLocationWithRationale = requestLocationFlow,
                                        onSelectSavedLocation = { viewModel.selectLocation(it) },
                                        onOverrideScene = { viewModel.setSceneOverride(it) },
                                        onNavigateToForecasts = { viewModel.navigateTo(AtmosDestination.FORECASTS) },
                                        onNavigateToCities = { viewModel.navigateTo(AtmosDestination.LOCATIONS) }
                                    )
                                }
                                AtmosDestination.FORECASTS -> {
                                    ForecastsScreen(
                                        weatherBundle = uiState.weatherBundle,
                                        settings = uiState.settings,
                                        onPreviewSceneOnDashboard = { scene ->
                                            viewModel.setSceneOverride(scene)
                                            viewModel.navigateTo(AtmosDestination.DASHBOARD)
                                        }
                                    )
                                }
                                AtmosDestination.GLOBE_3D -> {
                                    Interactive3DGlobeScreen(
                                        savedLocations = uiState.savedLocations,
                                        activeLocation = uiState.weatherBundle.location,
                                        tempUnit = uiState.settings.tempUnit,
                                        onSelectAndLoadCity = { city ->
                                            viewModel.selectLocation(city, navigateToDashboard = true)
                                        },
                                        onSaveCityToFavorites = { city ->
                                            viewModel.saveLocationToFavorites(city)
                                        }
                                    )
                                }
                                AtmosDestination.ANALYTICS -> {
                                    WeatherAnalyticsScreen(
                                        weatherBundle = uiState.weatherBundle,
                                        settings = uiState.settings,
                                        activeSceneOverride = uiState.activeSceneOverride,
                                        onSelectSceneOverride = { viewModel.setSceneOverride(it) }
                                    )
                                }
                                AtmosDestination.LOCATIONS -> {
                                    SavedLocationsScreen(
                                        savedLocations = uiState.savedLocations,
                                        activeLocationId = uiState.weatherBundle.location.id,
                                        searchQuery = uiState.searchQuery,
                                        searchResults = uiState.searchResults,
                                        isSearching = uiState.isSearching,
                                        settings = uiState.settings,
                                        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                                        onSelectCity = { city ->
                                            viewModel.selectLocation(city, navigateToDashboard = true)
                                        },
                                        onSaveCity = { city ->
                                            viewModel.saveLocationToFavorites(city)
                                        },
                                        onRemoveCity = { id ->
                                            viewModel.removeSavedLocation(id)
                                        },
                                        onDetectCurrentLocation = requestLocationFlow
                                    )
                                }
                                AtmosDestination.SETTINGS -> {
                                    SettingsScreen(
                                        settings = uiState.settings,
                                        hasLocationPermission = uiState.hasLocationPermission,
                                        onTempUnitChange = { viewModel.setTempUnit(it) },
                                        onWindUnitChange = { viewModel.setWindUnit(it) },
                                        onThemeModeChange = { viewModel.setThemeMode(it) },
                                        onGraphicsQualityChange = { viewModel.setGraphicsQuality(it) },
                                        onAutoRefreshChange = { viewModel.setAutoRefreshMinutes(it) },
                                        onNotificationsChange = { viewModel.setNotificationsEnabled(it) },
                                        onRequestLocationPermission = requestLocationFlow
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3D Splash Overlay on Launch
            AnimatedVisibility(
                visible = uiState.showSplash,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Atmos3DSplashOverlay(
                    onSplashFinished = { viewModel.dismissSplash() }
                )
            }
        }
    }
}

@Composable
private fun AtmosBottomNavigationBar(
    currentDestination: AtmosDestination,
    accentColor: Color,
    onNavigate: (AtmosDestination) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF070F1E).copy(alpha = 0.88f),
        contentColor = Color.White,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("atmos_bottom_nav")
    ) {
        AtmosDestination.entries.forEach { dest ->
            val selected = currentDestination == dest
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest) },
                icon = {
                    Icon(
                        imageVector = destinationIcon(dest, selected),
                        contentDescription = dest.label
                    )
                },
                label = {
                    Text(text = dest.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF041322),
                    selectedTextColor = accentColor,
                    indicatorColor = accentColor,
                    unselectedIconColor = Color.White.copy(alpha = 0.68f),
                    unselectedTextColor = Color.White.copy(alpha = 0.68f)
                ),
                modifier = Modifier.testTag("nav_item_${dest.route}")
            )
        }
    }
}

@Composable
private fun AtmosSideNavigationRail(
    currentDestination: AtmosDestination,
    accentColor: Color,
    onNavigate: (AtmosDestination) -> Unit
) {
    NavigationRail(
        containerColor = Color(0xFF070F1E).copy(alpha = 0.85f),
        contentColor = Color.White
    ) {
        AtmosDestination.entries.forEach { dest ->
            val selected = currentDestination == dest
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(dest) },
                icon = {
                    Icon(
                        imageVector = destinationIcon(dest, selected),
                        contentDescription = dest.label
                    )
                },
                label = { Text(dest.label) },
                modifier = Modifier.testTag("rail_item_${dest.route}")
            )
        }
    }
}

private fun destinationIcon(destination: AtmosDestination, selected: Boolean): ImageVector {
    return when (destination) {
        AtmosDestination.DASHBOARD -> if (selected) Icons.Filled.WbSunny else Icons.Outlined.WbSunny
        AtmosDestination.FORECASTS -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
        AtmosDestination.GLOBE_3D -> if (selected) Icons.Filled.Public else Icons.Outlined.Public
        AtmosDestination.ANALYTICS -> if (selected) Icons.Filled.Analytics else Icons.Outlined.Analytics
        AtmosDestination.LOCATIONS -> if (selected) Icons.Filled.LocationCity else Icons.Outlined.LocationCity
        AtmosDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }
}
