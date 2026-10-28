package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AtmosUniverseEngine
import com.example.model.DefaultGlobalCities
import com.example.model.SavedLocation
import com.example.model.TimeMachinePointType
import com.example.model.TimeMachineSlot
import com.example.model.TwinInterestCategory
import com.example.model.TwinUserPreference
import com.example.model.UserSettings
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import com.example.model.WhatIfSimulationState
import com.example.ui.components.AnimatedWeatherConditionIcon
import com.example.ui.components.FrostedGlassCard
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * SIGNATURE FEATURE 1: ATMOS WHAT-IF LAB
 * Interactive 3D weather experimentation mode with wind, cloud density, rain, fog, sunlight,
 * and time-of-day controls plus natural-language command mapping ("Create a stormy sunset scene.").
 * Strictly labeled as a hypothetical simulation that never alters or predicts real weather.
 */
@Composable
fun AtmosWhatIfLabCard(
    whatIfSimulation: WhatIfSimulationState,
    isProcessingCommand: Boolean,
    onUpdateParameters: (
        windStrength: Float,
        cloudDensity: Float,
        rainIntensity: Float,
        fogDensity: Float,
        sunlightIntensity: Float,
        timeOfDayHour: Float
    ) -> Unit,
    onExecuteWhatIfCommand: (String) -> Unit,
    onResetSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }
    val simAccent = Color(whatIfSimulation.derivedSceneType.accentColorHex)

    FrostedGlassCard(
        accentTint = if (whatIfSimulation.isActive) Color(0xFFF472B6) else Color(0xFFA78BFA),
        modifier = modifier
            .fillMaxWidth()
            .testTag("atmos_what_if_lab_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header + Prominent Hypothetical Simulation Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = Color(0xFFF472B6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "ATMOSYN WHAT-IF LAB · 3D EXPERIMENTATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF472B6)
                        )
                        Text(
                            text = "Interactive 3D Atmospheric Sandbox",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (whatIfSimulation.isActive) {
                    OutlinedButton(
                        onClick = onResetSimulation,
                        modifier = Modifier.testTag("what_if_reset_button")
                    ) {
                        Text("Exit Lab", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mandatory Non-Negotiable Simulation Honesty Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (whatIfSimulation.isActive) {
                    Color(0xFFF472B6).copy(alpha = 0.22f)
                } else {
                    Color(0xFFA78BFA).copy(alpha = 0.16f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (whatIfSimulation.isActive) Color(0xFFF472B6) else Color(0xFFA78BFA).copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("what_if_simulation_disclaimer_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFFDE68A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (whatIfSimulation.isActive) {
                            "HYPOTHETICAL SIMULATION ACTIVE — Visual experiment only. Changing these controls never alters or predicts real atmospheric conditions."
                        } else {
                            "HYPOTHETICAL SIMULATION SANDBOX — Experiment with 3D wind, clouds, rain, fog, sunlight, and time of day without affecting live weather data."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Natural-Language What-If Command Chips ("Create a stormy sunset scene.", etc.)
            Text(
                text = "Try a Natural-Language What-If Command:",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(AtmosUniverseEngine.sampleWhatIfCommands) { idx, cmd ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF472B6).copy(alpha = 0.16f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFF472B6).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .clickable {
                                commandInput = cmd
                                onExecuteWhatIfCommand(cmd)
                            }
                            .testTag("what_if_preset_chip_$idx")
                    ) {
                        Text(
                            text = cmd,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Natural-Language Command Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = {
                        Text(
                            text = "e.g., Create a stormy sunset scene.",
                            color = Color.White.copy(alpha = 0.50f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF472B6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.24f),
                        cursorColor = Color(0xFFF472B6)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("what_if_command_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val target = commandInput.trim().ifEmpty { "Create a stormy sunset scene." }
                        onExecuteWhatIfCommand(target)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF472B6))
                        .testTag("what_if_apply_command_button")
                ) {
                    if (isProcessingCommand) {
                        CircularProgressIndicator(
                            color = Color(0xFF041322),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Run What-If simulation command",
                            tint = Color(0xFF041322)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6 Interactive Parameter Sliders + Quick Step Controls
            WhatIfParameterRow(
                label = "Wind Strength",
                valueLabel = "${(whatIfSimulation.windStrength * 100).roundToInt()}% (${(whatIfSimulation.windStrength * 95).roundToInt()} km/h sim)",
                value = whatIfSimulation.windStrength,
                valueRange = 0f..1f,
                accentColor = simAccent,
                sliderTag = "what_if_wind_slider",
                boostButtonTag = "what_if_wind_boost_button",
                onValueChange = { newWind ->
                    onUpdateParameters(
                        newWind,
                        whatIfSimulation.cloudDensity,
                        whatIfSimulation.rainIntensity,
                        whatIfSimulation.fogDensity,
                        whatIfSimulation.sunlightIntensity,
                        whatIfSimulation.timeOfDayHour
                    )
                },
                onBoostClick = {
                    val next = if (whatIfSimulation.windStrength >= 0.8f) 0.25f else (whatIfSimulation.windStrength + 0.30f).coerceAtMost(1f)
                    onUpdateParameters(
                        next,
                        whatIfSimulation.cloudDensity,
                        whatIfSimulation.rainIntensity,
                        whatIfSimulation.fogDensity,
                        whatIfSimulation.sunlightIntensity,
                        whatIfSimulation.timeOfDayHour
                    )
                }
            )

            WhatIfParameterRow(
                label = "Cloud Density",
                valueLabel = "${(whatIfSimulation.cloudDensity * 100).roundToInt()}%",
                value = whatIfSimulation.cloudDensity,
                valueRange = 0f..1f,
                accentColor = simAccent,
                sliderTag = "what_if_cloud_slider",
                boostButtonTag = "what_if_cloud_boost_button",
                onValueChange = { newClouds ->
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        newClouds,
                        whatIfSimulation.rainIntensity,
                        whatIfSimulation.fogDensity,
                        whatIfSimulation.sunlightIntensity,
                        whatIfSimulation.timeOfDayHour
                    )
                },
                onBoostClick = {
                    val next = if (whatIfSimulation.cloudDensity >= 0.85f) 0.20f else (whatIfSimulation.cloudDensity + 0.30f).coerceAtMost(1f)
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        next,
                        whatIfSimulation.rainIntensity,
                        whatIfSimulation.fogDensity,
                        whatIfSimulation.sunlightIntensity,
                        whatIfSimulation.timeOfDayHour
                    )
                }
            )

            WhatIfParameterRow(
                label = "Rain Intensity",
                valueLabel = "${(whatIfSimulation.rainIntensity * 100).roundToInt()}%",
                value = whatIfSimulation.rainIntensity,
                valueRange = 0f..1f,
                accentColor = Color(0xFF38BDF8),
                sliderTag = "what_if_rain_slider",
                boostButtonTag = "what_if_rain_boost_button",
                onValueChange = { newRain ->
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        whatIfSimulation.cloudDensity,
                        newRain,
                        whatIfSimulation.fogDensity,
                        whatIfSimulation.sunlightIntensity,
                        whatIfSimulation.timeOfDayHour
                    )
                },
                onBoostClick = {
                    val next = if (whatIfSimulation.rainIntensity >= 0.75f) 0.0f else (whatIfSimulation.rainIntensity + 0.35f).coerceAtMost(1f)
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        whatIfSimulation.cloudDensity,
                        next,
                        whatIfSimulation.fogDensity,
                        whatIfSimulation.sunlightIntensity,
                        whatIfSimulation.timeOfDayHour
                    )
                }
            )

            WhatIfParameterRow(
                label = "Atmospheric Fog",
                valueLabel = "${(whatIfSimulation.fogDensity * 100).roundToInt()}%",
                value = whatIfSimulation.fogDensity,
                valueRange = 0f..1f,
                accentColor = Color(0xFFCBD5E1),
                sliderTag = "what_if_fog_slider",
                boostButtonTag = "what_if_fog_boost_button",
                onValueChange = { newFog ->
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        whatIfSimulation.cloudDensity,
                        whatIfSimulation.rainIntensity,
                        newFog,
                        whatIfSimulation.sunlightIntensity,
                        whatIfSimulation.timeOfDayHour
                    )
                },
                onBoostClick = {
                    val next = if (whatIfSimulation.fogDensity >= 0.75f) 0.10f else (whatIfSimulation.fogDensity + 0.35f).coerceAtMost(1f)
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        whatIfSimulation.cloudDensity,
                        whatIfSimulation.rainIntensity,
                        next,
                        whatIfSimulation.sunlightIntensity,
                        whatIfSimulation.timeOfDayHour
                    )
                }
            )

            WhatIfParameterRow(
                label = "Sunlight & Corona",
                valueLabel = "${(whatIfSimulation.sunlightIntensity * 100).roundToInt()}%",
                value = whatIfSimulation.sunlightIntensity,
                valueRange = 0f..1f,
                accentColor = Color(0xFFFBBF24),
                sliderTag = "what_if_sunlight_slider",
                boostButtonTag = "what_if_sunlight_boost_button",
                onValueChange = { newSun ->
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        whatIfSimulation.cloudDensity,
                        whatIfSimulation.rainIntensity,
                        whatIfSimulation.fogDensity,
                        newSun,
                        whatIfSimulation.timeOfDayHour
                    )
                },
                onBoostClick = {
                    val next = if (whatIfSimulation.sunlightIntensity >= 0.85f) 0.20f else (whatIfSimulation.sunlightIntensity + 0.30f).coerceAtMost(1f)
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        whatIfSimulation.cloudDensity,
                        whatIfSimulation.rainIntensity,
                        whatIfSimulation.fogDensity,
                        next,
                        whatIfSimulation.timeOfDayHour
                    )
                }
            )

            WhatIfParameterRow(
                label = "Time of Day Clock",
                valueLabel = "${AtmosUniverseEngine.formatFractionalHour(whatIfSimulation.timeOfDayHour)} (${whatIfSimulation.derivedSceneType.title})",
                value = whatIfSimulation.timeOfDayHour,
                valueRange = 0f..23.5f,
                accentColor = Color(0xFFFB923C),
                sliderTag = "what_if_time_slider",
                boostButtonTag = "what_if_time_boost_button",
                onValueChange = { newHour ->
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        whatIfSimulation.cloudDensity,
                        whatIfSimulation.rainIntensity,
                        whatIfSimulation.fogDensity,
                        whatIfSimulation.sunlightIntensity,
                        newHour
                    )
                },
                onBoostClick = {
                    val nextHour = when {
                        whatIfSimulation.timeOfDayHour < 11f -> 13.5f
                        whatIfSimulation.timeOfDayHour < 17f -> 18.0f
                        whatIfSimulation.timeOfDayHour < 21f -> 22.0f
                        else -> 6.5f
                    }
                    onUpdateParameters(
                        whatIfSimulation.windStrength,
                        whatIfSimulation.cloudDensity,
                        whatIfSimulation.rainIntensity,
                        whatIfSimulation.fogDensity,
                        whatIfSimulation.sunlightIntensity,
                        nextHour
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Simulation Status Summary Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("what_if_status_box")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedWeatherConditionIcon(
                        sceneType = whatIfSimulation.derivedSceneType,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (whatIfSimulation.isActive) {
                                "Active Simulation: ${whatIfSimulation.activeCommandLabel ?: whatIfSimulation.derivedSceneType.title}"
                            } else {
                                "Ready — Move any slider or tap a command above to animate the 3D scene"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = whatIfSimulation.simulationExplanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.80f),
                            modifier = Modifier.testTag("what_if_active_explanation_text")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatIfParameterRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    sliderTag: String,
    boostButtonTag: String,
    onValueChange: (Float) -> Unit,
    onBoostClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.22f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBoostClick() }
                        .testTag(boostButtonTag)
                ) {
                    Text(
                        text = "Step +",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.16f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .testTag(sliderTag)
        )
    }
}

/**
 * SIGNATURE FEATURE 2: ATMOS WEATHER TIME MACHINE
 * Interactive timeline for exploring available historical weather observations, current live
 * observations, future forecasts, and explicitly marked unavailable periods.
 * Animates the 3D weather scene as the selected time changes.
 */
@Composable
fun AtmosTimeMachineCard(
    activeLocation: SavedLocation,
    weatherBundle: WeatherBundle?,
    savedLocations: List<SavedLocation>,
    settings: UserSettings,
    selectedSlot: TimeMachineSlot?,
    onSelectLocation: (SavedLocation) -> Unit,
    onSelectTimeSlot: (TimeMachineSlot) -> Unit,
    onExitTimeMachine: () -> Unit,
    onAskAiAboutTimeSlot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val slots = remember(activeLocation, weatherBundle) {
        AtmosUniverseEngine.buildTimeMachineSlots(activeLocation, weatherBundle)
    }
    val defaultNowIndex = remember(slots) {
        slots.indexOfFirst { it.pointType == TimeMachinePointType.CURRENT_OBSERVATION }
            .let { if (it == -1) 0 else it }
    }
    var selectedIndex by remember(slots) {
        mutableIntStateOf(defaultNowIndex)
    }
    var isPlayingTimeline by remember { mutableStateOf(false) }

    val currentSlot = selectedSlot ?: slots.getOrElse(selectedIndex) { slots.first() }
    val pointBadgeColor = Color(currentSlot.pointType.colorHex)

    val selectableCities = remember(savedLocations, activeLocation) {
        (savedLocations + DefaultGlobalCities.hubs).distinctBy { it.id }.take(8)
    }

    LaunchedEffect(isPlayingTimeline, slots.size) {
        if (isPlayingTimeline && slots.size > 1) {
            while (isPlayingTimeline) {
                delay(1500L)
                val nextIdx = (selectedIndex + 1) % slots.size
                selectedIndex = nextIdx
                onSelectTimeSlot(slots[nextIdx])
            }
        }
    }

    FrostedGlassCard(
        accentTint = pointBadgeColor,
        modifier = modifier
            .fillMaxWidth()
            .testTag("atmos_time_machine_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = pointBadgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "ATMOSYN WEATHER TIME MACHINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = pointBadgeColor
                        )
                        Text(
                            text = "Historical Observations ↔ Live ↔ 7-Day Forecast",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (selectedSlot != null) {
                    OutlinedButton(
                        onClick = {
                            isPlayingTimeline = false
                            selectedIndex = defaultNowIndex
                            onExitTimeMachine()
                        },
                        modifier = Modifier.testTag("time_machine_exit_button")
                    ) {
                        Text("Return to Now", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Location Selector Strip
            Text(
                text = "Select Time Machine Location:",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectableCities, key = { "tm_city_${it.id}" }) { city ->
                    val isCurrent = city.id == activeLocation.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrent) pointBadgeColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                if (isCurrent) pointBadgeColor else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                isPlayingTimeline = false
                                onSelectLocation(city)
                            }
                            .testTag("time_machine_city_${city.id}")
                    ) {
                        Text(
                            text = city.cityName,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Category Legend Badges (Historical vs Current vs Forecast vs Unavailable)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TimeMachinePointType.entries) { pt ->
                    val c = Color(pt.colorHex)
                    val isCurrentCategory = currentSlot.pointType == pt
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrentCategory) c.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = if (isCurrentCategory) c else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(c)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pt.badgeLabelEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Interactive Timeline Slot Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(slots, key = { _, item -> item.id }) { idx, slot ->
                    val isSelected = slot.id == currentSlot.id
                    val chipColor = Color(slot.pointType.colorHex)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) chipColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) chipColor else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                isPlayingTimeline = false
                                selectedIndex = idx
                                onSelectTimeSlot(slot)
                            }
                            .testTag("time_machine_slot_$idx")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = slot.shortLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            val subText = slot.temperatureC?.let {
                                WeatherFormatter.formatTemp(it, settings.tempUnit)
                            } ?: "N/A"
                            Text(
                                text = subText,
                                style = MaterialTheme.typography.labelSmall,
                                color = chipColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Timeline Scrubber Slider + Step/Play Controls
            if (slots.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isPlayingTimeline = false
                            val prev = (selectedIndex - 1).coerceAtLeast(0)
                            selectedIndex = prev
                            onSelectTimeSlot(slots[prev])
                        },
                        modifier = Modifier.testTag("time_machine_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous time machine slot",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { isPlayingTimeline = !isPlayingTimeline },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(pointBadgeColor.copy(alpha = 0.25f))
                            .testTag("time_machine_play_button")
                    ) {
                        Icon(
                            imageVector = if (isPlayingTimeline) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingTimeline) "Pause Time Machine" else "Animate Time Machine",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            isPlayingTimeline = false
                            val next = (selectedIndex + 1).coerceAtMost(slots.lastIndex)
                            selectedIndex = next
                            onSelectTimeSlot(slots[next])
                        },
                        modifier = Modifier.testTag("time_machine_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next time machine slot",
                            tint = Color.White
                        )
                    }

                    Slider(
                        value = selectedIndex.toFloat(),
                        onValueChange = { raw ->
                            isPlayingTimeline = false
                            val idx = raw.roundToInt().coerceIn(0, slots.lastIndex)
                            selectedIndex = idx
                            onSelectTimeSlot(slots[idx])
                        },
                        valueRange = 0f..slots.lastIndex.coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = pointBadgeColor,
                            activeTrackColor = pointBadgeColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("time_machine_scrubber_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Inspected Time Slot Telemetry & Provenance Panel
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, pointBadgeColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .testTag("time_machine_detail_panel")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = pointBadgeColor.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "${currentSlot.pointType.badgeLabelEn} · ${currentSlot.pointType.badgeLabelBn}",
                                style = MaterialTheme.typography.labelSmall,
                                color = pointBadgeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("time_machine_point_type_badge")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentSlot.sceneType != null) {
                            AnimatedWeatherConditionIcon(
                                sceneType = currentSlot.sceneType,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            val tempHeadline = currentSlot.temperatureC?.let {
                                "${WeatherFormatter.formatTemp(it, settings.tempUnit)} · ${currentSlot.conditionText}"
                            } ?: currentSlot.conditionText
                            Text(
                                text = "${activeLocation.cityName}: $tempHeadline",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("time_machine_condition_headline")
                            )
                            Text(
                                text = "Timestamp: ${currentSlot.timestampLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFBAE6FD),
                                modifier = Modifier.testTag("time_machine_timestamp_text")
                            )
                            Text(
                                text = "Data Source: ${currentSlot.dataSourceLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.testTag("time_machine_datasource_text")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentSlot.pointType == TimeMachinePointType.UNAVAILABLE_PERIOD) {
                        Text(
                            text = currentSlot.honestyNoteEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFDE68A),
                            modifier = Modifier.testTag("time_machine_unavailable_message")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentSlot.honestyNoteBn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    } else {
                        val feelsLabel = currentSlot.feelsLikeC?.let {
                            WeatherFormatter.formatTemp(it, settings.tempUnit)
                        } ?: "Unavailable"
                        val rainProbLabel = WeatherFormatter.formatNullablePercent(currentSlot.precipProbabilityPercent)
                        val windLabel = currentSlot.windSpeedKmh?.let {
                            WeatherFormatter.formatWind(it, settings.windUnit)
                        } ?: "Unavailable"
                        val humLabel = WeatherFormatter.formatNullablePercent(currentSlot.humidityPercent)
                        val uvLabel = WeatherFormatter.formatNullableUv(currentSlot.uvIndex)

                        Text(
                            text = "Feels/Low: $feelsLabel   ·   Rain Prob: $rainProbLabel   ·   Wind: $windLabel\nHumidity: $humLabel   ·   UV Index: $uvLabel   ·   3D Scene: ${currentSlot.sceneType?.title ?: "Live"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentSlot.honestyNoteEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val prompt = "Explain the Time Machine weather state for ${activeLocation.cityName} at '${currentSlot.shortLabel}' (${currentSlot.timestampLabel}, Category: ${currentSlot.pointType.badgeLabelEn}, Condition: ${currentSlot.conditionText}, Source: ${currentSlot.dataSourceLabel}) in English and Bengali."
                            onAskAiAboutTimeSlot(prompt)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pointBadgeColor,
                            contentColor = Color(0xFF041322)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("time_machine_ask_ai_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask AI Copilot to Analyze This Time Slot")
                    }
                }
            }
        }
    }
}

/**
 * SIGNATURE FEATURE 3: PERSONAL WEATHER TWIN
 * Allows users to select interests (Farming, Outdoor Photography, Commuting, Exercise, Travel),
 * save/edit/delete custom preferences locally on device, and view personalized recommendations
 * with explicit Forecast Data Evidence.
 */
@Composable
fun PersonalWeatherTwinCard(
    activeLocation: SavedLocation,
    weatherBundle: WeatherBundle?,
    settings: UserSettings,
    twinPreferences: List<TwinUserPreference>,
    onSaveOrUpdatePreference: (
        existingId: String?,
        interest: TwinInterestCategory,
        customTitle: String,
        preferredWindow: String,
        userNote: String
    ) -> Unit,
    onDeletePreference: (String) -> Unit,
    onResetDefaultPreferences: () -> Unit,
    onAskAiTwinAdvice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedInterest by remember { mutableStateOf(TwinInterestCategory.OUTDOOR_PHOTOGRAPHY) }
    var editingPreferenceId by remember { mutableStateOf<String?>(null) }
    var customTitleInput by remember { mutableStateOf(TwinInterestCategory.OUTDOOR_PHOTOGRAPHY.labelEn) }
    var preferredWindowInput by remember { mutableStateOf(TwinInterestCategory.OUTDOOR_PHOTOGRAPHY.defaultWindow) }
    var userNoteInput by remember { mutableStateOf("") }

    val recommendations = remember(twinPreferences, activeLocation, weatherBundle, settings) {
        AtmosUniverseEngine.generateTwinRecommendations(
            preferences = twinPreferences,
            location = activeLocation,
            bundle = weatherBundle,
            settings = settings
        )
    }

    val twinAccent = Color(0xFF34D399)

    FrostedGlassCard(
        accentTint = twinAccent,
        modifier = modifier
            .fillMaxWidth()
            .testTag("personal_weather_twin_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.PersonPin,
                        contentDescription = null,
                        tint = twinAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "PERSONAL WEATHER TWIN · LOCAL PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = twinAccent
                        )
                        Text(
                            text = "Personalized Forecast-Grounded Routines",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onResetDefaultPreferences,
                    modifier = Modifier.testTag("twin_restore_defaults_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restore default Weather Twin profiles",
                        tint = Color.White.copy(alpha = 0.78f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your interests & notes are stored privately on-device via Jetpack DataStore. Every recommendation explains the exact forecast data behind it without making unsupported health or safety guarantees.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Interest Category Selector Chips (Farming, Outdoor Photography, Commuting, Exercise, Travel)
            Text(
                text = if (editingPreferenceId != null) {
                    "Editing Saved Interest Profile (Tap Category or Update Details):"
                } else {
                    "1. Select Interest Category to Add or Customize:"
                },
                style = MaterialTheme.typography.labelSmall,
                color = twinAccent
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TwinInterestCategory.entries, key = { it.id }) { category ->
                    val isSelected = selectedInterest == category
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) twinAccent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                if (isSelected) twinAccent else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedInterest = category
                                if (editingPreferenceId == null) {
                                    customTitleInput = category.labelEn
                                    preferredWindowInput = category.defaultWindow
                                }
                            }
                            .testTag("twin_interest_chip_${category.id}")
                    ) {
                        Text(
                            text = "${category.labelEn} · ${category.labelBn}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Save / Edit Preference Form
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customTitleInput,
                            onValueChange = { customTitleInput = it },
                            label = { Text("Activity / Goal Label", color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = twinAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.24f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("twin_title_input")
                        )
                        OutlinedTextField(
                            value = preferredWindowInput,
                            onValueChange = { preferredWindowInput = it },
                            label = { Text("Time Window", color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = twinAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.24f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("twin_window_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userNoteInput,
                        onValueChange = { userNoteInput = it },
                        label = { Text("Personal Preference / Equipment Note (Optional)", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = twinAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("twin_note_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveOrUpdatePreference(
                                    editingPreferenceId,
                                    selectedInterest,
                                    customTitleInput,
                                    preferredWindowInput,
                                    userNoteInput
                                )
                                editingPreferenceId = null
                                userNoteInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = twinAccent,
                                contentColor = Color(0xFF041322)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("twin_save_pref_button")
                        ) {
                            Icon(
                                imageVector = if (editingPreferenceId != null) Icons.Default.Edit else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (editingPreferenceId != null) {
                                    "Update Saved Twin Preference"
                                } else {
                                    "Save to Personal Weather Twin"
                                }
                            )
                        }

                        if (editingPreferenceId != null) {
                            OutlinedButton(
                                onClick = {
                                    editingPreferenceId = null
                                    customTitleInput = selectedInterest.labelEn
                                    preferredWindowInput = selectedInterest.defaultWindow
                                    userNoteInput = ""
                                },
                                modifier = Modifier.testTag("twin_cancel_edit_button")
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Saved Preferences & Grounded Recommendations List
            Text(
                text = "Your Saved Weather Twin Profiles (${recommendations.size}):",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f)
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (recommendations.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("twin_empty_state_box")
                ) {
                    Text(
                        text = "No saved Weather Twin preferences yet. Select an interest above and tap 'Save to Personal Weather Twin', or tap the refresh icon to restore defaults.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recommendations.forEach { rec ->
                        val pref = rec.preference
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                                .testTag("twin_item_card_${pref.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = twinAccent.copy(alpha = 0.22f)
                                            ) {
                                                Text(
                                                    text = "${pref.interest.labelEn} · ${rec.suitabilityBadge}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = twinAccent,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = pref.updatedAtLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.60f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${pref.customTitle} (${pref.preferredWindow})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingPreferenceId = pref.id
                                                selectedInterest = pref.interest
                                                customTitleInput = pref.customTitle
                                                preferredWindowInput = pref.preferredWindow
                                                userNoteInput = pref.userNote
                                            },
                                            modifier = Modifier.testTag("twin_edit_${pref.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit ${pref.customTitle}",
                                                tint = Color(0xFF38BDF8)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeletePreference(pref.id) },
                                            modifier = Modifier.testTag("twin_delete_${pref.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete ${pref.customTitle}",
                                                tint = Color(0xFFF87171)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = rec.recommendationEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = rec.recommendationBn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFBAE6FD)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF041322).copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("twin_evidence_${pref.id}")
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Forecast Data Evidence: ${rec.forecastEvidence}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFFDE68A)
                                        )
                                        Text(
                                            text = rec.safetyBoundaryNote,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    val summaryInterests = twinPreferences.joinToString { "${it.customTitle} (${it.interest.labelEn}, ${it.preferredWindow})" }
                    onAskAiTwinAdvice(
                        "Based on the actual live forecast for ${activeLocation.cityName}, provide personalized Weather Twin advice in English and Bengali for my saved interests: $summaryInterests. Cite exact temperature, rain probability, wind, UV, and AQI values."
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = twinAccent,
                    contentColor = Color(0xFF041322)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("twin_ask_ai_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask AI Copilot for Deep Weather Twin Briefing")
            }
        }
    }
}
