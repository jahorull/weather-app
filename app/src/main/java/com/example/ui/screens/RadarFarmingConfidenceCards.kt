package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.SmartFarmingAndAlertsEngine
import com.example.model.AgreementLevel
import com.example.model.BangladeshCrop
import com.example.model.DefaultGlobalCities
import com.example.model.PersonalThresholdAlert
import com.example.model.RadarFrame
import com.example.model.RadarTimelineBundle
import com.example.model.SavedLocation
import com.example.model.UserSettings
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import com.example.ui.components.FrostedGlassCard
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * FEATURE 1 — AI RAIN RADAR CARD
 * Interactive weather map with animated precipitation layers and a time slider (-6h observed to +12h forecast).
 * Displays historical observations and forecast layers only when supplied by the provider, with exact
 * timestamps, data sources, loading states, and unavailable-data messages. Never simulates radar data as real.
 */
@Composable
fun AiRainRadarCard(
    activeLocation: SavedLocation,
    weatherBundle: WeatherBundle?,
    settings: UserSettings,
    isRefreshing: Boolean,
    onAskAiRadarSummary: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val radarTimeline: RadarTimelineBundle? = weatherBundle?.radarTimeline
    val frames: List<RadarFrame> = radarTimeline?.frames.orEmpty()

    // Default to the first current/forecast frame ("Now") when available
    val defaultIndex = remember(frames) {
        val firstForecast = frames.indexOfFirst { !it.isHistoricalObservation }
        if (firstForecast >= 0) firstForecast else 0
    }

    var selectedFrameIndex by remember(frames) {
        mutableIntStateOf(defaultIndex.coerceIn(0, (frames.size - 1).coerceAtLeast(0)))
    }
    var isPlaying by remember { mutableStateOf(false) }
    var animTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying, frames.size) {
        if (isPlaying && frames.size > 1) {
            while (isPlaying) {
                delay(900L)
                selectedFrameIndex = (selectedFrameIndex + 1) % frames.size
                animTick = (animTick + 1) % 360
            }
        }
    }

    val safeIndex = selectedFrameIndex.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
    val currentFrame: RadarFrame? = frames.getOrNull(safeIndex)

    FrostedGlassCard(
        accentTint = Color(0xFF38BDF8),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_rain_radar_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI RAIN RADAR & PRECIPITATION TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                }

                if (currentFrame != null) {
                    val isObs = currentFrame.isHistoricalObservation
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isObs) {
                            Color(0xFF10B981).copy(alpha = 0.25f)
                        } else {
                            Color(0xFF38BDF8).copy(alpha = 0.25f)
                        },
                        modifier = Modifier.testTag("ai_rain_radar_frame_type_badge")
                    ) {
                        Text(
                            text = if (isObs) "HISTORICAL OBSERVATION" else "FORECAST LAYER",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isObs) Color(0xFF6EE7B7) else Color(0xFF7DD3FC),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Region: ${activeLocation.cityName}, ${activeLocation.country} (${
                    String.format(Locale.US, "%.2f°N, %.2f°E", activeLocation.latitude, activeLocation.longitude)
                })",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (frames.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_rain_radar_unavailable_box")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isRefreshing) {
                                "Loading live RainViewer & Open-Meteo precipitation timeline for ${activeLocation.cityName}…"
                            } else {
                                "Live radar & precipitation layers are currently unavailable for ${activeLocation.cityName}. Atmos 3D never simulates fake radar sweeps when provider data is missing."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            } else if (currentFrame != null) {
                // Interactive Radar Map Viewport Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF04101F), Color(0xFF071C34), Color(0xFF051326))
                            )
                        )
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .testTag("ai_rain_radar_canvas")
                ) {
                    PrecipitationRadarCanvas(
                        frame = currentFrame,
                        animTick = animTick,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top-left Telemetry Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(Color(0xFF041322).copy(alpha = 0.78f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${currentFrame.relativeOffsetLabel} · ${currentFrame.displayTimeLabel} (${currentFrame.conditionLabel})",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("ai_rain_radar_timestamp_label")
                        )
                        val mmText = currentFrame.precipitationMm?.let {
                            String.format(Locale.US, "%.1f mm/h", it)
                        } ?: "Unavailable"
                        val probText = currentFrame.precipProbabilityPercent?.let { "$it%" } ?: "Unavailable"
                        val windText = WeatherFormatter.formatWind(currentFrame.windSpeedKmh, settings.windUnit)
                        Text(
                            text = "Precip Rate: $mmText · Rain Prob: $probText · Wind: $windText",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7DD3FC)
                        )
                    }

                    // Bottom-right Legend
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color(0xFF041322).copy(alpha = 0.78f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadarLegendDot(color = Color(0xFF34D399), label = "Light (<1mm)")
                        RadarLegendDot(color = Color(0xFF38BDF8), label = "Mod (1-4mm)")
                        RadarLegendDot(color = Color(0xFFF59E0B), label = "Heavy (>4mm)")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Jump Chips (-6h Observed, Now, +6h Forecast, +12h Forecast)
                val jumpTargets = remember(frames) {
                    buildList {
                        add(0 to (frames.firstOrNull()?.relativeOffsetLabel ?: "-6h Obs"))
                        val nowIdx = frames.indexOfFirst { !it.isHistoricalObservation }.coerceAtLeast(0)
                        add(nowIdx to "Now (Live)")
                        val plus6Idx = (nowIdx + 6).coerceAtMost(frames.size - 1)
                        if (plus6Idx > nowIdx) add(plus6Idx to "+6h Forecast")
                        val lastIdx = frames.size - 1
                        if (lastIdx > plus6Idx) add(lastIdx to frames[lastIdx].relativeOffsetLabel)
                    }.distinctBy { it.first }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(jumpTargets, key = { it.first }) { (targetIdx, chipLabel) ->
                        val selected = safeIndex == targetIdx
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF38BDF8).copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (selected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    isPlaying = false
                                    selectedFrameIndex = targetIdx
                                }
                                .testTag("radar_jump_chip_$targetIdx")
                        ) {
                            Text(
                                text = chipLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Time Slider & Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            isPlaying = false
                            selectedFrameIndex = (safeIndex - 1).coerceAtLeast(0)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ai_rain_radar_prev_button")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous radar frame", tint = Color.White)
                    }

                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                            .testTag("ai_rain_radar_play_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause radar animation" else "Play radar animation",
                            tint = Color(0xFF041322)
                        )
                    }

                    IconButton(
                        onClick = {
                            isPlaying = false
                            selectedFrameIndex = (safeIndex + 1).coerceAtMost(frames.size - 1)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ai_rain_radar_next_button")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next radar frame", tint = Color.White)
                    }

                    Slider(
                        value = safeIndex.toFloat(),
                        onValueChange = {
                            isPlaying = false
                            selectedFrameIndex = it.roundToInt().coerceIn(0, frames.size - 1)
                        },
                        valueRange = 0f..(frames.size - 1).coerceAtLeast(1).toFloat(),
                        steps = (frames.size - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_rain_radar_slider")
                    )
                }

                // Data Source & Coverage Provenance Footer
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Data Source: ${currentFrame.dataSourceLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF34D399),
                            modifier = Modifier.testTag("ai_rain_radar_source_text")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = radarTimeline?.coverageNote ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val obsCount = frames.count { it.isHistoricalObservation }
                        val fcstCount = frames.count { !it.isHistoricalObservation }
                        val peakFrame = frames.maxByOrNull { it.precipProbabilityPercent ?: 0 }
                        val prompt = "Analyze the AI Rain Radar timeline for ${activeLocation.cityName} ($obsCount historical observation frames and $fcstCount forecast frames). " +
                            "Selected frame ${currentFrame.relativeOffsetLabel} (${currentFrame.displayTimeLabel}) shows ${currentFrame.precipitationMm ?: 0.0} mm/h and ${currentFrame.precipProbabilityPercent ?: 0}% rain probability. " +
                            "Peak frame is ${peakFrame?.relativeOffsetLabel ?: "N/A"} (${peakFrame?.precipProbabilityPercent ?: 0}%). Explain when rain is most likely in English and simple Bengali (বাংলা)."
                        onAskAiRadarSummary(prompt)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_rain_radar_explain_button")
                ) {
                    Text("Ask AI to Explain Radar Timeline (EN / বাংলা)", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RadarLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun PrecipitationRadarCanvas(
    frame: RadarFrame,
    animTick: Int,
    modifier: Modifier = Modifier
) {
    val precipMm = frame.precipitationMm ?: 0.0
    val prob = frame.precipProbabilityPercent ?: 0
    val windDeg = frame.windDirectionDeg.toFloat()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val maxRadius = minOf(w, h) * 0.44f

        // Coordinate grid lines
        val gridColor = Color(0xFF38BDF8).copy(alpha = 0.14f)
        for (i in 1..3) {
            val y = h * (i / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            val x = w * (i / 4f)
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }

        // Range rings (25km, 50km, 100km)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        listOf(0.33f, 0.66f, 1.0f).forEach { frac ->
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.22f),
                radius = maxRadius * frac,
                center = center,
                style = Stroke(width = 1.2f, pathEffect = dashEffect)
            )
        }

        // Only draw precipitation cells when actual API data reports non-zero precipitation or probability
        val intensityFactor = ((precipMm / 6.0).coerceIn(0.0, 1.0) * 0.7 + (prob / 100.0) * 0.3).toFloat()
        if (precipMm > 0.0 || prob >= 15) {
            val cellColor = when {
                precipMm >= 4.0 || prob >= 75 -> Color(0xFFF59E0B)
                precipMm >= 1.0 || prob >= 45 -> Color(0xFF38BDF8)
                else -> Color(0xFF34D399)
            }

            val radAngle = (windDeg * PI / 180.0).toFloat()
            val driftOffset = (animTick % 20) / 20f * 14f
            val dx = cos(radAngle) * driftOffset
            val dy = sin(radAngle) * driftOffset

            val blobRadius = maxRadius * (0.28f + 0.55f * intensityFactor)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cellColor.copy(alpha = (0.52f * intensityFactor).coerceIn(0.18f, 0.65f)),
                        cellColor.copy(alpha = (0.24f * intensityFactor).coerceIn(0.08f, 0.35f)),
                        Color.Transparent
                    ),
                    center = Offset(center.x + dx, center.y + dy),
                    radius = blobRadius
                ),
                radius = blobRadius,
                center = Offset(center.x + dx, center.y + dy)
            )

            // Secondary rain band cell along wind vector
            val secondaryCenter = Offset(
                center.x - cos(radAngle) * maxRadius * 0.28f + dx,
                center.y - sin(radAngle) * maxRadius * 0.22f + dy
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cellColor.copy(alpha = (0.40f * intensityFactor).coerceIn(0.12f, 0.50f)),
                        Color.Transparent
                    ),
                    center = secondaryCenter,
                    radius = blobRadius * 0.72f
                ),
                radius = blobRadius * 0.72f,
                center = secondaryCenter
            )
        }

        // Center city marker pin
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = 3.5.dp.toPx(),
            center = center
        )
    }
}

/**
 * FEATURE 2 — FORECAST CONFIDENCE ANALYZER CARD
 * Compares forecasts from multiple NWP models (Open-Meteo Best Match, ECMWF IFS 0.25°, NOAA GFS, DWD ICON).
 * Explains model disagreement in simple Bengali and English.
 * Never invents confidence percentages or claims guaranteed accuracy.
 * Clearly separates observations, numerical forecasts, and AI-generated explanations.
 */
@Composable
fun ForecastConfidenceAnalyzerCard(
    activeLocation: SavedLocation,
    weatherBundle: WeatherBundle?,
    settings: UserSettings,
    onAskAiModelSpread: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val report = weatherBundle?.confidenceReport
    var selectedDayIndex by remember(report) { mutableIntStateOf(0) }
    var showBengali by remember { mutableStateOf(true) }

    FrostedGlassCard(
        accentTint = Color(0xFFA855F7),
        modifier = modifier
            .fillMaxWidth()
            .testTag("forecast_confidence_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FORECAST CONFIDENCE & MULTI-MODEL ANALYZER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC084FC)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showBengali = !showBengali }
                        .testTag("confidence_toggle_bengali_button")
                ) {
                    Text(
                        text = if (showBengali) "EN + বাংলা" else "English Only",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (report == null || report.dailyComparisons.isEmpty()) {
                Text(
                    text = "Multi-model forecast comparison is currently unavailable for ${activeLocation.cityName}. Refresh live weather telemetry to compare Open-Meteo, ECMWF IFS, NOAA GFS, and DWD ICON.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.80f)
                )
            } else {
                // SECTION 1: Clearly separated Current Observation
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .testTag("confidence_observation_box")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "1. VERIFIED OBSERVATION (LIVE TELEMETRY · NOT A FORECAST)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = report.observedSummaryEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        if (showBengali) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = report.observedSummaryBn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SECTION 2: Multi-Model Forecast Comparison
                Text(
                    text = "2. MULTI-MODEL NUMERICAL FORECAST COMPARISON (${report.dataSourcesLabel})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC084FC),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(report.dailyComparisons) { idx, dayComp ->
                        val selected = idx == selectedDayIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFFA855F7).copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (selected) Color(0xFFC084FC) else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedDayIndex = idx }
                                .testTag("confidence_day_chip_$idx")
                        ) {
                            Text(
                                text = dayComp.dayLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                val activeDay = report.dailyComparisons.getOrElse(selectedDayIndex) { report.dailyComparisons.first() }
                Spacer(modifier = Modifier.height(8.dp))

                // Agreement Qualitative Badge (No fabricated percentage!)
                val badgeColor = when (activeDay.agreementLevel) {
                    AgreementLevel.HIGH -> Color(0xFF34D399)
                    AgreementLevel.MODERATE -> Color(0xFFFBBF24)
                    AgreementLevel.LOW -> Color(0xFFF87171)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeColor.copy(alpha = 0.18f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confidence_agreement_badge")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${activeDay.dayLabel}: ${activeDay.agreementLevel.labelEn} (${activeDay.agreementLevel.labelBn})",
                            style = MaterialTheme.typography.labelLarge,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                        val tempSpreadStr = activeDay.maxTempSpreadC?.let { String.format(Locale.US, "Δ%.1f°C", it) } ?: "N/A"
                        val rainSpreadStr = activeDay.precipSpreadPercent?.let { "Δ$it%" } ?: "N/A"
                        val windSpreadStr = activeDay.windSpreadKmh?.let { String.format(Locale.US, "Δ%.0f km/h", it) } ?: "N/A"
                        Text(
                            text = "Physical Model Spread: High Temp $tempSpreadStr · Rain Prob $rainSpreadStr · Wind $windSpreadStr (No invented confidence %)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Per-Model Table Rows
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeDay.models.forEach { entry ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.07f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.modelName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1.2f)
                                )
                                val hi = entry.maxTempC?.let { WeatherFormatter.formatTemp(it, settings.tempUnit) } ?: "N/A"
                                val lo = entry.minTempC?.let { WeatherFormatter.formatTemp(it, settings.tempUnit) } ?: "N/A"
                                val rain = entry.precipProbPercent?.let { "$it%" } ?: "N/A"
                                val wind = entry.maxWindKmh?.let { WeatherFormatter.formatWind(it, settings.windUnit) } ?: "N/A"
                                Text(
                                    text = "H:$hi / L:$lo · Rain:$rain · $wind",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SECTION 3: Bilingual Explanation of Model Disagreement
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confidence_explanation_box")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "3. MODEL DISAGREEMENT EXPLANATION (ENGLISH & বাংলা)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC084FC),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeDay.explanationEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        if (showBengali) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = activeDay.explanationBn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val modelsList = activeDay.models.joinToString(", ") { it.modelName }
                        val prompt = "Explain the multi-model forecast comparison for ${activeLocation.cityName} on ${activeDay.dayLabel} in both English and simple Bengali (বাংলা). " +
                            "Models compared: $modelsList. " +
                            "Temperature spread is Δ${activeDay.maxTempSpreadC ?: 0.0}°C, rain probability spread is Δ${activeDay.precipSpreadPercent ?: 0}%, and wind spread is Δ${activeDay.windSpreadKmh ?: 0.0} km/h (${activeDay.agreementLevel.labelEn}). " +
                            "Clearly separate current observation from model forecasts and do not invent confidence percentages."
                        onAskAiModelSpread(prompt)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC084FC),
                        contentColor = Color(0xFF041322)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confidence_ask_ai_button")
                ) {
                    Text("Ask AI to Explain Model Disagreement (EN / বাংলা)")
                }
            }
        }
    }
}

/**
 * FEATURE 3 — BANGLADESH SMART FARMING MODE CARD
 * Allows users to select a location (including 8 Bangladesh agricultural divisions + saved cities) and a crop.
 * Provides verified weather-based guidance on rainfall, irrigation timing, drying conditions, and
 * suitable windows for outdoor farm activities in English and Bengali.
 * Never provides unsupported pesticide dosages or guarantees crop outcomes.
 */
@Composable
fun BangladeshSmartFarmingCard(
    activeLocation: SavedLocation,
    weatherBundle: WeatherBundle?,
    savedLocations: List<SavedLocation>,
    selectedCrop: BangladeshCrop,
    settings: UserSettings,
    onSelectLocation: (SavedLocation) -> Unit,
    onSelectCrop: (BangladeshCrop) -> Unit,
    onAskAiKrishiAdvisor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val farmingLocations = remember(savedLocations) {
        (DefaultGlobalCities.bangladeshFarmingHubs + savedLocations).distinctBy { it.id }
    }

    val advisory = remember(activeLocation, weatherBundle, selectedCrop, settings) {
        SmartFarmingAndAlertsEngine.buildSmartFarmingAdvisory(
            location = activeLocation,
            bundle = weatherBundle,
            crop = selectedCrop,
            settings = settings
        )
    }

    var showBothLanguages by remember { mutableStateOf(true) }

    FrostedGlassCard(
        accentTint = Color(0xFF10B981),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_farming_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Agriculture,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BANGLADESH SMART FARMING MODE (স্মার্ট কৃষি)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34D399)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.22f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showBothLanguages = !showBothLanguages }
                        .testTag("farming_lang_toggle")
                ) {
                    Text(
                        text = if (showBothLanguages) "EN + বাংলা" else "English Only",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6EE7B7),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Active Agricultural Hub: ${advisory.updateTimestampLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.testTag("farming_active_hub_label")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Select Agricultural Region / Hub
            Text(
                text = "1. Select Agricultural Region / Division (অঞ্চল নির্বাচন):",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(farmingLocations, key = { "farm_loc_${it.id}" }) { loc ->
                    val selected = loc.id == activeLocation.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) Color(0xFF10B981).copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                if (selected) Color(0xFF34D399) else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectLocation(loc) }
                            .testTag("farming_location_chip_${loc.id}")
                    ) {
                        Text(
                            text = "${loc.cityName} (${loc.country})",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Select Crop
            Text(
                text = "2. Select Crop (ফসল নির্বাচন করুন):",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BangladeshCrop.entries, key = { it.id }) { crop ->
                    val selected = crop == selectedCrop
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) Color(0xFF10B981).copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                if (selected) Color(0xFF34D399) else Color.White.copy(alpha = 0.14f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectCrop(crop) }
                            .testTag("farming_crop_chip_${crop.id}")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = "${crop.nameEn} · ${crop.nameBn}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Optimal: ${crop.idealTempRangeC.start.toInt()}–${crop.idealTempRangeC.endInclusive.toInt()}°C",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Grounded Agricultural Guidance Pillars
            FarmingPillarBox(
                title = "RAINFALL & DRAINAGE (বৃষ্টিপাত ও নিষ্কাশন)",
                textEn = advisory.rainfallGuidanceEn,
                textBn = advisory.rainfallGuidanceBn,
                showBengali = showBothLanguages,
                accentColor = Color(0xFF38BDF8),
                testTag = "farming_pillar_rainfall"
            )
            Spacer(modifier = Modifier.height(8.dp))

            FarmingPillarBox(
                title = "IRRIGATION TIMING (সেচ দেওয়ার উপযুক্ত সময়)",
                textEn = advisory.irrigationGuidanceEn,
                textBn = advisory.irrigationGuidanceBn,
                showBengali = showBothLanguages,
                accentColor = Color(0xFF34D399),
                testTag = "farming_pillar_irrigation"
            )
            Spacer(modifier = Modifier.height(8.dp))

            FarmingPillarBox(
                title = "DRYING & POST-HARVEST (ফসল ও বীজ শুকানোর পরিবেশ)",
                textEn = advisory.dryingGuidanceEn,
                textBn = advisory.dryingGuidanceBn,
                showBengali = showBothLanguages,
                accentColor = Color(0xFFFBBF24),
                testTag = "farming_pillar_drying"
            )
            Spacer(modifier = Modifier.height(8.dp))

            FarmingPillarBox(
                title = "OUTDOOR FARM ACTIVITY WINDOWS (মাঠের কাজের সময়সীমা)",
                textEn = advisory.outdoorActivityWindowsEn,
                textBn = advisory.outdoorActivityWindowsBn,
                showBengali = showBothLanguages,
                accentColor = Color(0xFFC084FC),
                testTag = "farming_pillar_activities"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Forecast Uncertainty & DAE 16123 Expert Notice
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .testTag("farming_safety_notice_box")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "FORECAST UNCERTAINTY & DAE (16123) AGRONOMY BOUNDARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = advisory.uncertaintyExplanationEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    if (showBothLanguages) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = advisory.uncertaintyExplanationBn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFDE68A)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = advisory.expertGuidanceNoticeEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    if (showBothLanguages) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = advisory.expertGuidanceNoticeBn,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A).copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val prompt = "Provide Bangladesh Smart Farming weather guidance for ${selectedCrop.nameEn} (${selectedCrop.nameBn}) in ${activeLocation.cityName} in both English and simple Bengali (বাংলা). " +
                        "Include rainfall/drainage, irrigation timing, crop drying conditions, and suitable outdoor farm windows based on current telemetry. " +
                        "Explain forecast uncertainty, never give pesticide dosages or guaranteed crop outcomes, and remind when to consult DAE (16123)."
                    onAskAiKrishiAdvisor(prompt)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34D399),
                    contentColor = Color(0xFF041322)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("farming_ask_ai_button")
            ) {
                Text("Ask AI Krishi Advisor for ${selectedCrop.nameEn} (EN / বাংলা)")
            }
        }
    }
}

@Composable
private fun FarmingPillarBox(
    title: String,
    textEn: String,
    textBn: String,
    showBengali: Boolean,
    accentColor: Color,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = textEn,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            if (showBengali) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = textBn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA7F3D0)
                )
            }
        }
    }
}

/**
 * FEATURE 4 — PERSONALIZED WEATHER ALERTS CARD
 * Lets users configure custom thresholds for rain probability, temperature, wind speed, and US AQI,
 * manage favorite-city monitoring and Android POST_NOTIFICATIONS permissions, and view triggered
 * user-threshold alerts clearly separated from official government alerts.
 */
@Composable
fun PersonalizedWeatherAlertsCard(
    activeLocation: SavedLocation,
    settings: UserSettings,
    triggeredPersonalAlerts: List<PersonalThresholdAlert>,
    hasNotificationPermission: Boolean,
    lastNotificationStatusMessage: String?,
    onSetPersonalizedAlertsEnabled: (Boolean) -> Unit,
    onSetMonitorAllFavorites: (Boolean) -> Unit,
    onSetRainThreshold: (Int) -> Unit,
    onSetTempThreshold: (Int) -> Unit,
    onSetWindThreshold: (Int) -> Unit,
    onSetAqiThreshold: (Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onEvaluateAndNotifyNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    FrostedGlassCard(
        accentTint = Color(0xFFF59E0B),
        modifier = modifier
            .fillMaxWidth()
            .testTag("personalized_alerts_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "PERSONALIZED WEATHER ALERTS (USER THRESHOLDS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFBBF24)
                        )
                        Text(
                            text = "Custom Rain, Temp, Wind & AQI Triggers",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Switch(
                    checked = settings.personalizedAlertsEnabled,
                    onCheckedChange = onSetPersonalizedAlertsEnabled,
                    modifier = Modifier.testTag("personal_alerts_master_switch")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Configure personal thresholds for ${activeLocation.cityName} and favorite cities. Triggered notifications are clearly labeled as custom user thresholds, never fabricated as official government warnings.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.80f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Favorite Locations Scope Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monitor All Saved Favorite Locations",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Check thresholds across active city + cached favorite cities during refresh cycles",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }
                Switch(
                    checked = settings.monitorAllFavoriteCities,
                    onCheckedChange = onSetMonitorAllFavorites,
                    modifier = Modifier.testTag("personal_alerts_favorites_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Rain Probability Threshold
            ThresholdSelectorRow(
                title = "Rain Probability Threshold (>= ${settings.rainAlertThresholdPercent}%)",
                options = listOf(40, 60, 75, 85),
                selectedValue = settings.rainAlertThresholdPercent,
                labelProvider = { "$it%" },
                tagPrefix = "threshold_rain",
                onSelect = onSetRainThreshold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. High Temperature Threshold
            ThresholdSelectorRow(
                title = "High Temperature Threshold (>= ${
                    WeatherFormatter.formatTemp(settings.tempAlertThresholdC.toDouble(), settings.tempUnit)
                })",
                options = listOf(30, 33, 35, 38),
                selectedValue = settings.tempAlertThresholdC,
                labelProvider = { WeatherFormatter.formatTemp(it.toDouble(), settings.tempUnit) },
                tagPrefix = "threshold_temp",
                onSelect = onSetTempThreshold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Wind Speed Threshold
            ThresholdSelectorRow(
                title = "Wind Speed Threshold (>= ${
                    WeatherFormatter.formatWind(settings.windAlertThresholdKmh.toDouble(), settings.windUnit)
                })",
                options = listOf(25, 40, 55, 70),
                selectedValue = settings.windAlertThresholdKmh,
                labelProvider = { WeatherFormatter.formatWind(it.toDouble(), settings.windUnit) },
                tagPrefix = "threshold_wind",
                onSelect = onSetWindThreshold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Air Quality (US AQI) Threshold
            ThresholdSelectorRow(
                title = "Air Quality Threshold (>= US AQI ${settings.aqiAlertThresholdUs})",
                options = listOf(50, 100, 150, 200),
                selectedValue = settings.aqiAlertThresholdUs,
                labelProvider = { "AQI $it" },
                tagPrefix = "threshold_aqi",
                onSelect = onSetAqiThreshold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Permission & Evaluation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hasNotificationPermission) {
                    OutlinedButton(
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("personal_alerts_permission_button")
                    ) {
                        Text("Grant Notification Permission", color = Color(0xFFFDE68A))
                    }
                }
                Button(
                    onClick = onEvaluateAndNotifyNow,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFBBF24),
                        contentColor = Color(0xFF041322)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("personal_alerts_evaluate_button")
                ) {
                    Text("Check Thresholds & Notify Now")
                }
            }

            if (!lastNotificationStatusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lastNotificationStatusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6EE7B7),
                    modifier = Modifier.testTag("personal_alerts_status_message")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Triggered Personal Threshold Alerts List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("personal_alerts_triggered_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!settings.personalizedAlertsEnabled) {
                    Text(
                        text = "Personalized threshold monitoring is currently paused. Enable the toggle above to evaluate live telemetry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                } else if (triggeredPersonalAlerts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All monitored parameters in ${activeLocation.cityName} are currently below your custom alert thresholds.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    triggeredPersonalAlerts.forEach { alert ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${alert.locationName}: ${alert.metricCategory}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${alert.triggeredValueLabel} · ${alert.thresholdValueLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFDE68A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = alert.messageEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = alert.messageBn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = alert.sourceBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF93C5FD)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Architectural Transparency Notice (Client Auto-Refresh vs Server Push)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Notification Architecture Note: Thresholds are evaluated automatically during active/background auto-refresh cycles (${settings.autoRefreshMinutes} min interval) and dispatched via Android NotificationManager when POST_NOTIFICATIONS is granted. Continuous 24/7 wake-up alerts when the app process is terminated by the OS require a server-side cron + Firebase Cloud Messaging (FCM) push worker.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.68f),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun ThresholdSelectorRow(
    title: String,
    options: List<Int>,
    selectedValue: Int,
    labelProvider: (Int) -> String,
    tagPrefix: String,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.80f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { value ->
                val selected = selectedValue == value
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) Color(0xFFFBBF24).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (selected) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.14f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelect(value) }
                        .testTag("${tagPrefix}_$value")
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelProvider(value),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
