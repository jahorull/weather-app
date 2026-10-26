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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.HourlyForecast
import com.example.model.SavedLocation
import com.example.model.UserSettings
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
 * Screen 2: Main Full-Screen Dashboard with 3D Weather Viewport, Hourly Strip, and 6 Telemetry Cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    weatherBundle: WeatherBundle,
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
    modifier: Modifier = Modifier
) {
    var showLocationRationaleDialog by remember { mutableStateOf(false) }
    var inspectedHour by remember { mutableStateOf<HourlyForecast?>(null) }

    val current = weatherBundle.current
    val location = weatherBundle.location
    val effectiveScene = activeSceneOverride ?: current.sceneType
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
                            text = "${location.cityName.uppercase()}, ${location.country.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                        Text(
                            text = "${WeatherFormatter.formatTemp(current.temperatureC, settings.tempUnit)} · ${effectiveScene.title}",
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

        // Standard Dashboard Overlay Content
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
                // 1. Top Bar: City Selector, Live/Cached Status Badge, GPS & Refresh & Immersive Actions
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
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${location.cityName.uppercase()}, ${location.country.uppercase()}",
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
                                // Live vs Cached Badge
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (weatherBundle.isFromCache) {
                                        Color(0xFFF59E0B).copy(alpha = 0.25f)
                                    } else {
                                        Color(0xFF10B981).copy(alpha = 0.25f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (weatherBundle.isFromCache) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = if (weatherBundle.isFromCache) Color(0xFFFBBF24) else Color(0xFF34D399),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (weatherBundle.isFromCache) {
                                                stringResource(R.string.status_cached_data)
                                            } else {
                                                stringResource(R.string.status_live_data)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (weatherBundle.isFromCache) Color(0xFFFDE68A) else Color(0xFFA7F3D0)
                                        )
                                    }
                                }

                                Text(
                                    text = WeatherFormatter.formatRelativeUpdateTime(weatherBundle.fetchedAtMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.72f)
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
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(savedLocations, key = { it.id }) { loc ->
                                val isCurrent = loc.id == location.id
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

                // Error Banner if network failed
                if (!errorMessage.isNullOrBlank()) {
                    item {
                        FrostedGlassCard(cornerRadius = 16.dp, accentTint = Color(0xFFFBBF24)) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }

                // 2. Hero 3D Weather Readout (Large Temperature, Condition, Feels Like, High / Low)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
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
                                if (activeSceneOverride != null) "${activeSceneOverride.title} (3D Preview)" else current.conditionText
                            }",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Feels Like ${WeatherFormatter.formatTemp(current.feelsLikeC, settings.tempUnit)}   ·   High ${
                                WeatherFormatter.formatTemp(current.highC, settings.tempUnit, includeUnit = false)
                            } · Low ${
                                WeatherFormatter.formatTemp(current.lowC, settings.tempUnit, includeUnit = false)
                            }",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }

                // Interactive 3D Atmosphere Quick Switcher Bar (lets user explore all 8 3D environments immediately)
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "3D ATMOSPHERE ENVIRONMENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            if (activeSceneOverride != null) {
                                Text(
                                    text = "Reset to Live Weather",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accentColor,
                                    modifier = Modifier
                                        .clickable { onOverrideScene(null) }
                                        .padding(4.dp)
                                        .testTag("reset_live_scene_button")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(WeatherSceneType.entries, key = { it.name }) { scene ->
                                val isSelected = effectiveScene == scene
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) Color(scene.accentColorHex).copy(alpha = 0.32f) else Color.White.copy(alpha = 0.09f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(scene.accentColorHex) else Color.White.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            onOverrideScene(if (scene == current.sceneType) null else scene)
                                        }
                                        .testTag("scene_chip_${scene.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AnimatedWeatherConditionIcon(
                                            sceneType = scene,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = scene.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Horizontally Scrollable 24-Hour Forecast Card
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
                                    text = "Full Charts →",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clickable { onNavigateToForecasts() }
                                        .testTag("dashboard_open_forecasts_link")
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
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
                                            text = "${hr.precipProbabilityPercent}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Compact Telemetry Grid: Row 1 (Humidity & Wind Speed + Direction)
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
                                    text = "${current.humidityPercent}%",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${WeatherFormatter.humidityComfortLabel(current.humidityPercent)} · Dew point ${
                                        WeatherFormatter.formatTemp(current.dewPointC, settings.tempUnit)
                                    }",
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
                                    Text(
                                        text = "Dir: ${WeatherFormatter.windDirectionCardinal(current.windDirectionDeg)} (${current.windDirectionDeg}°)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                                WindCompassDial(windDirectionDeg = current.windDirectionDeg)
                            }
                        }
                    }
                }

                // 5. Compact Telemetry Grid: Row 2 (UV Index & Visibility)
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
                                    text = "${String.format("%.1f", current.uvIndex)} · ${WeatherFormatter.uvCategoryLabel(current.uvIndex)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (current.uvIndex.toFloat() / 11f).coerceIn(0.05f, 1f) },
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
                                    text = "${String.format("%.1f", current.visibilityKm)} km",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (current.visibilityKm >= 9.0) "Clear horizon clarity" else "Reduced by mist/haze",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                // 6. Compact Telemetry Grid: Row 3 (Atmospheric Pressure & Sunrise/Sunset)
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
                                    text = "${current.pressureHpa.toInt()} hPa",
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

@Composable
private fun HourlyDetailSheetContent(
    hour: HourlyForecast,
    settings: UserSettings,
    onPreview3DScene: () -> Unit
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
            MetricMiniColumn("Rain Prob", "${hour.precipProbabilityPercent}%")
            MetricMiniColumn("Wind", WeatherFormatter.formatWind(hour.windSpeedKmh, settings.windUnit))
        }

        Button(
            onClick = onPreview3DScene,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(hour.sceneType.accentColorHex),
                contentColor = Color(0xFF041322)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate ${hour.sceneType.title} 3D Environment")
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun MetricMiniColumn(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
