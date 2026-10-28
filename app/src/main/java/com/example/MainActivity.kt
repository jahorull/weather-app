package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.SmartFarmingAndAlertsEngine
import com.example.ai.configureAppCheck
import com.example.model.WeatherSceneType
import com.example.ui.components.LocalDeviceTilt
import com.example.ui.scene3d.Interactive3DGlobeScreen
import com.example.ui.scene3d.Weather3DSceneView
import com.example.ui.screens.AiCopilotScreen
import com.example.ui.screens.Atmos3DSplashOverlay
import com.example.ui.screens.ForecastsScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.SavedLocationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeatherAnalyticsScreen
import com.example.ui.sensor.TiltVector
import com.example.ui.theme.Atmos3DTheme
import com.example.viewmodel.AtmosDestination
import com.example.viewmodel.AtmosViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MainActivity implements [SensorEventListener] directly to track device rotation and tilt
 * (via Rotation Vector / Game Rotation Vector / Accelerometer sensors) and feeds the resulting
 * 3D orientation vector into the 3D weather scene camera perspective and glass UI layers.
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var rotationSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var smoothedPitch = 0f
    private var smoothedRoll = 0f
    private var smoothedAzimuth = 0f

    private val _deviceTiltFlow = MutableStateFlow(TiltVector())
    val deviceTiltFlow: StateFlow<TiltVector> = _deviceTiltFlow.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationSensor == null) {
            accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }

        setContent {
            val context = LocalContext.current
            val viewModel: AtmosViewModel = viewModel(
                factory = AtmosViewModel.Factory(context)
            )
            val tiltVector by deviceTiltFlow.collectAsStateWithLifecycle()
            Atmos3DApp(
                viewModel = viewModel,
                tiltVector = tiltVector
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val manager = sensorManager ?: return
        val activeSensor = rotationSensor ?: accelerometerSensor
        if (activeSensor != null) {
            manager.registerListener(
                this,
                activeSensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val sensorEvent = event ?: return
        when (sensorEvent.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, sensorEvent.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val targetAzimuth = (orientationAngles[0] / 1.57f).coerceIn(-1f, 1f)
                val targetPitch = ((orientationAngles[1] + 0.45f) / 1.1f).coerceIn(-1f, 1f)
                val targetRoll = (orientationAngles[2] / 1.1f).coerceIn(-1f, 1f)
                applyLowPassFilter(targetPitch, targetRoll, targetAzimuth)
            }
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_ACCELEROMETER -> {
                if (sensorEvent.values.size >= 2) {
                    val targetRoll = (-sensorEvent.values[0] / 9.81f).coerceIn(-1f, 1f)
                    val targetPitch = ((sensorEvent.values[1] - 4.5f) / 9.81f).coerceIn(-1f, 1f)
                    applyLowPassFilter(targetPitch, targetRoll, 0f)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun applyLowPassFilter(targetPitch: Float, targetRoll: Float, targetAzimuth: Float) {
        val alpha = 0.14f
        smoothedPitch += (targetPitch - smoothedPitch) * alpha
        smoothedRoll += (targetRoll - smoothedRoll) * alpha
        smoothedAzimuth += (targetAzimuth - smoothedAzimuth) * alpha
        _deviceTiltFlow.value = TiltVector(
            pitch = smoothedPitch,
            roll = smoothedRoll,
            azimuth = smoothedAzimuth
        )
    }
}

@Composable
fun Atmos3DApp(
    viewModel: AtmosViewModel,
    tiltVector: TiltVector = TiltVector()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-detect Android system Animator duration scale (Reduced Motion accessibility setting)
    val systemReducedMotion = remember(context) {
        runCatching {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        }.getOrDefault(false)
    }
    val effectiveReducedMotion = systemReducedMotion || uiState.settings.reducedMotion
    val effectiveTilt = if (effectiveReducedMotion) TiltVector() else tiltVector

    val effectiveScene = uiState.activeSceneOverride
        ?: uiState.weatherBundle?.current?.sceneType
        ?: uiState.activeLocation.cachedSceneType
        ?: WeatherSceneType.SUNNY
    val localHour = uiState.weatherBundle?.current?.localHourOfDay ?: 12

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
        viewModel.updateNotificationPermissionStatus(
            SmartFarmingAndAlertsEngine.hasNotificationPermission(context)
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateNotificationPermissionStatus(granted)
        if (granted) {
            viewModel.evaluateAndNotifyPersonalAlerts(context)
        }
    }

    val requestNotificationPermissionFlow = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (alreadyGranted) {
                viewModel.updateNotificationPermissionStatus(true)
                viewModel.evaluateAndNotifyPersonalAlerts(context)
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.updateNotificationPermissionStatus(true)
            viewModel.evaluateAndNotifyPersonalAlerts(context)
        }
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

    BackHandler(enabled = uiState.isImmersiveMode || uiState.currentDestination != AtmosDestination.DASHBOARD) {
        if (uiState.isImmersiveMode) {
            viewModel.toggleImmersiveMode()
        } else {
            viewModel.navigateTo(AtmosDestination.DASHBOARD)
        }
    }

    CompositionLocalProvider(LocalDeviceTilt provides effectiveTilt) {
        Atmos3DTheme(
            themeMode = uiState.settings.themeMode,
            sceneType = effectiveScene
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 640.dp

                // Full-bleed real-time 3D weather environment backdrop synchronized with local time & accessibility
                Weather3DSceneView(
                    sceneType = effectiveScene,
                    graphicsQuality = uiState.settings.graphicsQuality,
                    tiltVector = effectiveTilt,
                    localHourOfDay = localHour,
                    reducedMotion = effectiveReducedMotion,
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
                                            activeLocation = uiState.activeLocation,
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
                                            onNavigateToCities = { viewModel.navigateTo(AtmosDestination.LOCATIONS) },
                                            onNavigateToAiCopilot = { viewModel.navigateTo(AtmosDestination.AI_COPILOT) },
                                            onAskAiAboutForecast = { prompt -> viewModel.askAiAboutForecastPeriod(prompt) }
                                        )
                                    }
                                    AtmosDestination.FORECASTS -> {
                                        ForecastsScreen(
                                            activeLocation = uiState.activeLocation,
                                            weatherBundle = uiState.weatherBundle,
                                            settings = uiState.settings,
                                            onRefreshWeather = { viewModel.refreshCurrentWeather() },
                                            onPreviewSceneOnDashboard = { scene ->
                                                viewModel.setSceneOverride(scene)
                                                viewModel.navigateTo(AtmosDestination.DASHBOARD)
                                            },
                                            onAskAiAboutForecast = { prompt ->
                                                viewModel.askAiAboutForecastPeriod(prompt)
                                            }
                                        )
                                    }
                                    AtmosDestination.AI_COPILOT -> {
                                        AiCopilotScreen(
                                            activeLocation = uiState.activeLocation,
                                            weatherBundle = uiState.weatherBundle,
                                            savedLocations = uiState.savedLocations,
                                            settings = uiState.settings,
                                            activeSceneOverride = uiState.activeSceneOverride,
                                            copilotLanguage = uiState.copilotLanguage,
                                            chatMessages = uiState.chatMessages,
                                            isAiGenerating = uiState.isAiGenerating,
                                            lastFailedPrompt = uiState.lastFailedPrompt,
                                            aiAdvisorHeadline = uiState.aiAdvisorHeadline,
                                            aiAdvisorItems = uiState.aiAdvisorItems,
                                            isGeneratingAdvisor = uiState.isGeneratingAdvisor,
                                            advisorErrorMessage = uiState.advisorErrorMessage,
                                            lastSceneCommandResult = uiState.lastSceneCommandResult,
                                            sceneCommandErrorMessage = uiState.sceneCommandErrorMessage,
                                            isProcessingSceneCommand = uiState.isProcessingSceneCommand,
                                            travelDestination = uiState.travelDestination,
                                            travelDestinationBundle = uiState.travelDestinationBundle,
                                            travelDayOffset = uiState.travelDayOffset,
                                            isLoadingTravelDestination = uiState.isLoadingTravelDestination,
                                            onSelectLanguage = { viewModel.setCopilotLanguage(it) },
                                            onSendCopilotPrompt = { viewModel.sendCopilotPrompt(it) },
                                            onRetryLastPrompt = { viewModel.retryLastCopilotPrompt() },
                                            onExecuteSceneCommand = { viewModel.executeAiSceneCommand(it) },
                                            onResetSceneOverride = { viewModel.setSceneOverride(null) },
                                            onGenerateAiAdvisor = { viewModel.generateAiDailyAdvisor() },
                                            onSelectTravelDestination = { viewModel.selectTravelDestination(it) },
                                            onSelectTravelDayOffset = { viewModel.selectTravelDayOffset(it) },
                                            onAskAiTravelPlan = { viewModel.askAiForTravelPlan() },
                                            isRefreshing = uiState.isRefreshing,
                                            selectedFarmingCrop = uiState.selectedFarmingCrop,
                                            triggeredPersonalAlerts = uiState.triggeredPersonalAlerts,
                                            hasNotificationPermission = uiState.hasNotificationPermission,
                                            lastNotificationStatusMessage = uiState.lastNotificationStatusMessage,
                                            onSelectFarmingLocation = { loc ->
                                                viewModel.selectLocation(loc, navigateToDashboard = false)
                                            },
                                            onSelectFarmingCrop = { crop ->
                                                viewModel.selectFarmingCrop(crop)
                                            },
                                            onSetPersonalizedAlertsEnabled = { enabled ->
                                                viewModel.setPersonalizedAlertsEnabled(enabled)
                                            },
                                            onSetMonitorAllFavorites = { monitor ->
                                                viewModel.setMonitorAllFavoriteCities(monitor)
                                            },
                                            onSetRainThreshold = { pct ->
                                                viewModel.setRainAlertThreshold(pct)
                                            },
                                            onSetTempThreshold = { tempC ->
                                                viewModel.setTempAlertThreshold(tempC)
                                            },
                                            onSetWindThreshold = { windKmh ->
                                                viewModel.setWindAlertThreshold(windKmh)
                                            },
                                            onSetAqiThreshold = { aqi ->
                                                viewModel.setAqiAlertThreshold(aqi)
                                            },
                                            onRequestNotificationPermission = requestNotificationPermissionFlow,
                                            onEvaluateAndNotifyNow = {
                                                viewModel.evaluateAndNotifyPersonalAlerts(context)
                                            }
                                        )
                                    }
                                    AtmosDestination.GLOBE_3D -> {
                                        Interactive3DGlobeScreen(
                                            savedLocations = uiState.savedLocations,
                                            activeLocation = uiState.activeLocation,
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
                                            activeLocationId = uiState.activeLocation.id,
                                            searchQuery = uiState.searchQuery,
                                            searchResults = uiState.searchResults,
                                            isSearching = uiState.isSearching,
                                            searchErrorMessage = uiState.searchErrorMessage,
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
                                            systemReducedMotionDetected = systemReducedMotion,
                                            hasLocationPermission = uiState.hasLocationPermission,
                                            onTempUnitChange = { viewModel.setTempUnit(it) },
                                            onWindUnitChange = { viewModel.setWindUnit(it) },
                                            onThemeModeChange = { viewModel.setThemeMode(it) },
                                            onGraphicsQualityChange = { viewModel.setGraphicsQuality(it) },
                                            onReducedMotionChange = { viewModel.setReducedMotion(it) },
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
                    Text(
                        text = dest.label,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                alwaysShowLabel = true,
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
        AtmosDestination.AI_COPILOT -> if (selected) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome
        AtmosDestination.GLOBE_3D -> if (selected) Icons.Filled.Public else Icons.Outlined.Public
        AtmosDestination.ANALYTICS -> if (selected) Icons.Filled.Analytics else Icons.Outlined.Analytics
        AtmosDestination.LOCATIONS -> if (selected) Icons.Filled.LocationCity else Icons.Outlined.LocationCity
        AtmosDestination.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
    }
}
