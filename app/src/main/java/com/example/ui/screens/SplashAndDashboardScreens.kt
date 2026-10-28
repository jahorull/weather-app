package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AirQualityData
import com.example.model.HourlyForecast
import com.example.model.SavedLocation
import com.example.model.UserSettings
import com.example.model.WeatherAlert
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.example.ui.components.AnimatedWeatherConditionIcon
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.SolarHorizonArc
import com.example.ui.components.WindCompassDial
import kotlinx.coroutines.delay

/**
 * Screen 1: Splash screen with a 3D atmospheric animation.
 */
@Composable
fun Atmos3DSplashOverlay(
    onSplashFinished: () -> Unit
) {
    var started by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(1200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF0A2540),
                        Color(0xFF0284C7)
                    )
                )
            )
            .clickable { onSplashFinished() }
            .testTag("splash_screen_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .alpha(alphaAnim)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(2.dp, Color(0xFF38BDF8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AnimatedWeatherConditionIcon(
                    sceneType = WeatherSceneType.SUNNY,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "ATMOS 3D",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Living 3D Atmospheric Telemetry",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFBAE6FD),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Screen 2: Upgraded Full-Screen Dashboard with Live Weather, Air Quality (US AQI/PM2.5/PM10),
 * Weather Alerts, 24-Hour Strip, 6 Telemetry Cards, and Honest Loading/Error/Retry States.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    activeLocation: SavedLocation,
    weatherBundle: WeatherBundle?,
    savedLocations: List<SavedLocation>,
    settings: UserSettings,
    activeSceneOverride: WeatherSceneType?,
    isRefreshing: Boolean,
    isImmersiveMode: Boolean,
    errorMessage: String?,
    onToggleImmersiveMode: () -> Unit,
    onRefreshWeather: () -> Unit,
    onRequestLocationWithRationale: () -> Unit,
    onSelectSavedLocation: (SavedLocation) -> Unit,
    onOverrideScene: (WeatherSceneType?) -> Unit,
    onNavigateToForecasts: () -> Unit,
    onNavigateToCities: () -> Unit,
    onNavigateToAiCopilot: () -> Unit = {},
    onAskAiAboutForecast: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLocationRationaleDialog by remember { mutableStateOf(false) }
    var inspectedHour by remember { mutableStateOf<HourlyForecast?>(null) }

    val effectiveScene = activeSceneOverride
        ?: weatherBundle?.current?.sceneType
        ?: activeLocation.cachedSceneType
        ?: WeatherSceneType.SUNNY
    val accentColor = Color(effectiveScene.accentColorHex)

    if (showLocationRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showLocationRationaleDialog = false },
            containerColor = Color(0xFF0F172A),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = {
                Text(stringResource(R.string.location_rationale_title))
            },
            text = {
                Text(stringResource(R.string.location_rationale_body))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationRationaleDialog = false
                        onRequestLocationWithRationale()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF041322)
                    ),
                    modifier = Modifier.testTag("confirm_location_rationale_button")
                ) {
                    Text(stringResource(R.string.action_grant_location))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLocationRationaleDialog = false },
                    modifier = Modifier.testTag("dismiss_location_rationale_button")
                ) {
                    Text(stringResource(R.string.action_not_now), color = Color.White)
                }
            }
        )
    }

    inspectedHour?.let { hourItem ->
        ModalBottomSheet(
            onDismissRequest = { inspectedHour = null },
            containerColor = Color(0xFF0D182B),
            contentColor = Color.White
        ) {
            HourlyDetailSheetContent(
                hour = hourItem,
                settings = settings,
                onPreview3DScene = {
                    onOverrideScene(hourItem.sceneType)
                    inspectedHour = null
                },
                onAskAiAboutHour = {
                    val prompt = "Explain the forecast for ${activeLocation.cityName} at ${hourItem.displayHour} (${hourItem.conditionText}, ${
                        WeatherFormatter.formatTemp(hourItem.temperatureC, settings.tempUnit)
                    }, Rain ${WeatherFormatter.formatNullablePercent(hourItem.precipProbabilityPercent)}) and what I should prepare for."
                    inspectedHour = null
                    onAskAiAboutForecast(prompt)
                }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Immersive Full-Screen 3D Mode Floating Controls
        AnimatedVisibility(
            visible = isImmersiveMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
        ) {
            FrostedGlassCard(cornerRadius = 28.dp, accentTint = accentColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeLocation.cityName.uppercase()}, ${activeLocation.country.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                        val tempLabel = weatherBundle?.current?.temperatureC?.let {
                            WeatherFormatter.formatTemp(it, settings.tempUnit)
                        } ?: "3D Viewport"
                        Text(
                            text = "$tempLabel · ${effectiveScene.title}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Tilt device or drag screen to orbit 3D atmosphere",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                    IconButton(
                        onClick = onToggleImmersiveMode,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .testTag("exit_immersive_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit immersive 3D mode",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Standard Dashboard Content
        AnimatedVisibility(
            visible = !isImmersiveMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dashboard_lazy_column"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Top Bar: City Selector, Live/Cached Status Badge, Local Time, GPS & Refresh & Immersive Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onNavigateToCities() }
                                .padding(vertical = 4.dp)
                                .testTag("dashboard_city_header")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationCity,
                                    contentDescription = "Select city",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeLocation.cityName.uppercase()}, ${activeLocation.country.uppercase()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isCached = weatherBundle?.isFromCache == true
                                val hasLiveBundle = weatherBundle != null
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        !hasLiveBundle -> Color(0xFF64748B).copy(alpha = 0.35f)
                                        isCached -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                                        else -> Color(0xFF10B981).copy(alpha = 0.25f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isCached || !hasLiveBundle) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = if (isCached || !hasLiveBundle) Color(0xFFFBBF24) else Color(0xFF34D399),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = when {
                                                !hasLiveBundle -> if (isRefreshing) "SYNCING..." else "OFFLINE"
                                                isCached -> stringResource(R.string.status_cached_data)
                                                else -> stringResource(R.string.status_live_data)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isCached || !hasLiveBundle) Color(0xFFFDE68A) else Color(0xFFA7F3D0)
                                        )
                                    }
                                }

                                val localTimePrefix = weatherBundle?.current?.cityLocalTimeLabel
                                    ?.takeIf { it.isNotBlank() && it != "Unavailable" }
                                    ?.let { "Local $it · " } ?: ""

                                Text(
                                    text = localTimePrefix + WeatherFormatter.formatRelativeUpdateTime(
                                        weatherBundle?.fetchedAtMillis ?: 0L
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { showLocationRationaleDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .testTag("detect_gps_location_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Detect current GPS location",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = onRefreshWeather,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .testTag("refresh_weather_button")
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        color = accentColor,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.action_refresh),
                                        tint = Color.White
                                    )
                                }
                            }

                            IconButton(
                                onClick = onToggleImmersiveMode,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .testTag("enter_immersive_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = stringResource(R.string.action_immersive_mode),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Quick Saved City Switcher Pills
                if (savedLocations.size > 1) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(savedLocations, key = { it.id }) { loc ->
                                val isCurrent = loc.id == activeLocation.id
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isCurrent) accentColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.10f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = 1.dp,
                                            color = if (isCurrent) accentColor else Color.White.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onSelectSavedLocation(loc) }
                                        .testTag("city_chip_${loc.id}")
                                ) {
                                    Text(
                                        text = loc.cityName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Error / Cached Notice Banner with Retry Action
                if (!errorMessage.isNullOrBlank()) {
                    item {
                        FrostedGlassCard(cornerRadius = 18.dp, accentTint = Color(0xFFFBBF24)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFDE68A),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = onRefreshWeather,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFBBF24),
                                        contentColor = Color(0xFF041322)
                                    ),
                                    modifier = Modifier.testTag("banner_retry_button")
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                // Handle Loading or No-Cache Offline State honestly without fake weather numbers
                if (weatherBundle == null) {
                    item {
                        FrostedGlassCard(
                            cornerRadius = 24.dp,
                            accentTint = accentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hero_current_temp")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedWeatherConditionIcon(
                                    sceneType = effectiveScene,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (activeSceneOverride != null) {
                                        "3D Preset Active: ${effectiveScene.title}"
                                    } else {
                                        "3D Atmosphere: ${effectiveScene.title}"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                if (isRefreshing) {
                                    CircularProgressIndicator(color = accentColor)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Fetching live meteorological telemetry for ${activeLocation.cityName}…",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = "No live weather connection",
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Live Weather Data Unavailable",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No cached forecast exists yet for ${activeLocation.cityName}. Check your internet connection and tap Retry, or explore the 3D weather presets below.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.78f),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = onRefreshWeather,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor,
                                            contentColor = Color(0xFF041322)
                                        ),
                                        modifier = Modifier.testTag("empty_state_retry_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry Live Weather Fetch")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val current = weatherBundle.current

                    // 2. Hero 3D Weather Readout
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AnimatedWeatherConditionIcon(
                                    sceneType = effectiveScene,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = WeatherFormatter.formatTemp(current.temperatureC, settings.tempUnit),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = Color.White,
                                    modifier = Modifier.testTag("hero_current_temp")
                                )
                            }

                            Text(
                                text = "${WeatherFormatter.formatTemp(current.temperatureC, settings.tempUnit)} — ${
                                    if (activeSceneOverride != null) "${activeSceneOverride.title} (3D Preset)" else current.conditionText
                                }",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("hero_condition_label")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Feels Like ${WeatherFormatter.formatTemp(current.feelsLikeC, settings.tempUnit)}   ·   High ${
                                    WeatherFormatter.formatTemp(current.highC, settings.tempUnit, includeUnit = false)
                                } · Low ${
                                    WeatherFormatter.formatTemp(current.lowC, settings.tempUnit, includeUnit = false)
                                }   ·   Rain ${WeatherFormatter.formatNullablePercent(current.precipProbabilityPercent)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.88f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 3. Interactive 3D Atmosphere Presets Grid (Always active & visible for all 8 weather scenes)
                item {
                    FrostedGlassCard(
                        cornerRadius = 22.dp,
                        accentTint = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weather_presets_section")
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "3D ATMOSPHERE ENVIRONMENT PRESETS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor
                                    )
                                    Text(
                                        text = "${effectiveScene.title} — ${effectiveScene.subtitle}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.84f),
                                        modifier = Modifier.testTag("active_preset_status_text")
                                    )
                                }
                                if (activeSceneOverride != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = accentColor.copy(alpha = 0.24f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, accentColor, RoundedCornerShape(12.dp))
                                            .clickable { onOverrideScene(null) }
                                            .testTag("reset_live_scene_button")
                                    ) {
                                        Text(
                                            text = "Reset to Live Weather",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            val presetRows = WeatherSceneType.entries.chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                presetRows.forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { scene ->
                                            val isSelected = effectiveScene == scene
                                            val sceneAccent = Color(scene.accentColorHex)
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isSelected) sceneAccent.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.09f),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSelected) sceneAccent else Color.White.copy(alpha = 0.14f),
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable { onOverrideScene(scene) }
                                                    .testTag("scene_chip_${scene.name.lowercase()}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AnimatedWeatherConditionIcon(
                                                        sceneType = scene,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = scene.title,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = Color.White,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (weatherBundle != null) {
                    val current = weatherBundle.current

                    // 4. Weather Alerts Section (Real NWS Alerts + Live WMO Threshold Advisories)
                    item {
                        WeatherAlertsSectionCard(
                            alerts = weatherBundle.alerts,
                            accentColor = accentColor
                        )
                    }

                    // 5. Horizontally Scrollable 24-Hour Forecast Card
                    item {
                        FrostedGlassCard(accentTint = accentColor) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "HOURLY 24-HOUR FORECAST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor
                                    )
                                    Text(
                                        text = "Interactive Charts & 7-Day →",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        modifier = Modifier
                                            .clickable { onNavigateToForecasts() }
                                            .testTag("dashboard_open_forecasts_link")
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(weatherBundle.hourly, key = { it.timeIso + it.displayHour }) { hr ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .clickable { inspectedHour = hr }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = hr.displayHour,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AnimatedWeatherConditionIcon(
                                                sceneType = hr.sceneType,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = WeatherFormatter.formatTemp(hr.temperatureC, settings.tempUnit, includeUnit = false),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = hr.precipProbabilityPercent?.let { "$it%" } ?: "N/A",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Live Air Quality Card (US AQI, PM2.5, PM10, O3, NO2)
                    item {
                        AirQualityDashboardCard(
                            airQuality = weatherBundle.airQuality,
                            accentColor = accentColor
                        )
                    }

                    // 7. Compact Telemetry Grid: Row 1 (Humidity & Wind Speed + Direction)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FrostedGlassCard(
                                modifier = Modifier.weight(1f),
                                accentTint = accentColor
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "HUMIDITY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = WeatherFormatter.formatNullablePercent(current.humidityPercent),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val dewText = current.dewPointC?.let {
                                        " · Dew point ${WeatherFormatter.formatTemp(it, settings.tempUnit)}"
                                    } ?: ""
                                    Text(
                                        text = "${WeatherFormatter.humidityComfortLabel(current.humidityPercent)}$dewText",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            FrostedGlassCard(
                                modifier = Modifier.weight(1f),
                                accentTint = accentColor
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "WIND VECTOR",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = WeatherFormatter.formatWind(current.windSpeedKmh, settings.windUnit),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val dirText = current.windDirectionDeg?.let {
                                            "Dir: ${WeatherFormatter.windDirectionCardinal(it)} ($it°)"
                                        } ?: "Dir: Unavailable"
                                        Text(
                                            text = dirText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                    }
                                    WindCompassDial(windDirectionDeg = current.windDirectionDeg ?: 0)
                                }
                            }
                        }
                    }

                    // 8. Compact Telemetry Grid: Row 2 (UV Index & Visibility)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FrostedGlassCard(
                                modifier = Modifier.weight(1f),
                                accentTint = accentColor
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "UV INDEX",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = WeatherFormatter.formatNullableUv(current.uvIndex),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { ((current.uvIndex?.toFloat() ?: 0f) / 11f).coerceIn(0.04f, 1f) },
                                        color = Color(0xFFFBBF24),
                                        trackColor = Color.White.copy(alpha = 0.16f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }

                            FrostedGlassCard(
                                modifier = Modifier.weight(1f),
                                accentTint = accentColor
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color(0xFF67E8F9),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "VISIBILITY",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = WeatherFormatter.formatNullableVisibility(current.visibilityKm),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val visDesc = when {
                                        current.visibilityKm == null -> "Station reading unavailable"
                                        current.visibilityKm >= 9.0 -> "Clear horizon clarity"
                                        else -> "Reduced by mist/haze"
                                    }
                                    Text(
                                        text = visDesc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }

                    // 9. Compact Telemetry Grid: Row 3 (Atmospheric Pressure & Sunrise/Sunset)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FrostedGlassCard(
                                modifier = Modifier.weight(1f),
                                accentTint = accentColor
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Compress,
                                            contentDescription = null,
                                            tint = Color(0xFFA78BFA),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PRESSURE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = WeatherFormatter.formatNullablePressure(current.pressureHpa),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = WeatherFormatter.pressureTrendLabel(current.pressureHpa),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            FrostedGlassCard(
                                modifier = Modifier.weight(1f),
                                accentTint = accentColor
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WbTwilight,
                                            contentDescription = null,
                                            tint = Color(0xFFFB923C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SUNRISE & SUNSET",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.72f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SolarHorizonArc(
                                        isDay = current.isDay,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Rise", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                            Text(current.sunriseTime, style = MaterialTheme.typography.labelLarge, color = Color.White)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Set", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                            Text(current.sunsetTime, style = MaterialTheme.typography.labelLarge, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherAlertsSectionCard(
    alerts: List<WeatherAlert>,
    accentColor: Color
) {
    if (alerts.isEmpty()) {
        FrostedGlassCard(
            cornerRadius = 18.dp,
            accentTint = Color(0xFF34D399),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weather_alerts_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "No active weather alerts",
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "WEATHER ALERTS · ALL CLEAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34D399)
                    )
                    Text(
                        text = "No active severe weather warnings or advisories for this location.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("weather_alerts_card")
        ) {
            alerts.forEach { alert ->
                val alertColor = when (alert.severity.lowercase()) {
                    "extreme", "severe" -> Color(0xFFEF4444)
                    "moderate" -> Color(0xFFF59E0B)
                    else -> accentColor
                }
                FrostedGlassCard(cornerRadius = 20.dp, accentTint = alertColor) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = "Weather Alert",
                                    tint = alertColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WEATHER ALERT · ${alert.severity.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = alertColor
                                )
                            }
                            Text(
                                text = alert.issuedAt,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = alert.headline,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alert.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Area: ${alert.affectedArea} · Issued: ${alert.issuedAt} · Expires: ${alert.expiresAt} · Source: ${alert.source}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AirQualityDashboardCard(
    airQuality: AirQualityData?,
    accentColor: Color
) {
    val aqiValue = airQuality?.usAqi
    val badgeColor = Color(WeatherFormatter.aqiColorHex(aqiValue))

    FrostedGlassCard(
        accentTint = badgeColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("air_quality_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AIR QUALITY INDEX (US AQI)",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.22f)
                ) {
                    Text(
                        text = WeatherFormatter.aqiCategoryLabel(aqiValue),
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricMiniColumn(
                    label = "US AQI",
                    value = aqiValue?.toString() ?: "Unavailable"
                )
                MetricMiniColumn(
                    label = "PM2.5",
                    value = WeatherFormatter.formatNullablePollutant(airQuality?.pm25)
                )
                MetricMiniColumn(
                    label = "PM10",
                    value = WeatherFormatter.formatNullablePollutant(airQuality?.pm10)
                )
                MetricMiniColumn(
                    label = "Ozone (O₃)",
                    value = WeatherFormatter.formatNullablePollutant(airQuality?.ozone)
                )
            }
        }
    }
}

@Composable
private fun HourlyDetailSheetContent(
    hour: HourlyForecast,
    settings: UserSettings,
    onPreview3DScene: () -> Unit,
    onAskAiAboutHour: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HOURLY TELEMETRY BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "${hour.displayHour} — ${hour.conditionText}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
            AnimatedWeatherConditionIcon(
                sceneType = hour.sceneType,
                modifier = Modifier.size(48.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricMiniColumn("Temperature", WeatherFormatter.formatTemp(hour.temperatureC, settings.tempUnit))
            MetricMiniColumn("Feels Like", WeatherFormatter.formatTemp(hour.feelsLikeC, settings.tempUnit))
            MetricMiniColumn("Rain Prob", WeatherFormatter.formatNullablePercent(hour.precipProbabilityPercent))
            MetricMiniColumn("Wind", WeatherFormatter.formatWind(hour.windSpeedKmh, settings.windUnit))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAskAiAboutHour,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38BDF8),
                    contentColor = Color(0xFF041322)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask AI Copilot")
            }
            Button(
                onClick = onPreview3DScene,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(hour.sceneType.accentColorHex).copy(alpha = 0.25f),
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Simulate 3D")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun MetricMiniColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
