package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DailyForecast
import com.example.model.HourlyForecast
import com.example.model.UserSettings
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.example.ui.components.AnimatedWeatherConditionIcon
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.WindCompassDial

/**
 * Screens 3 & 4: Hourly (24-Hour) and Weekly (7-Day) Forecasts with Interactive Charts
 * and tap-to-inspect details for any hour or day.
 */
@Composable
fun ForecastsScreen(
    weatherBundle: WeatherBundle,
    settings: UserSettings,
    onPreviewSceneOnDashboard: (WeatherSceneType) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = 24-Hour, 1 = 7-Day
    var selectedHourIndex by remember { mutableIntStateOf(0) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    val hourly = weatherBundle.hourly
    val daily = weatherBundle.daily
    val accentColor = Color(weatherBundle.current.sceneType.accentColorHex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("forecasts_screen_root"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Segmented View Switcher
        item {
            Column {
                Text(
                    text = "${weatherBundle.location.cityName.uppercase()} METEOROLOGY",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
                Text(
                    text = "Hourly & 7-Day Forecasts",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Next 24 Hours (Hourly)", "7-Day Outlook").forEachIndexed { idx, title ->
                        val isSelected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.32f) else Color.Transparent
                                )
                                .clickable { selectedTab = idx }
                                .padding(vertical = 10.dp)
                                .testTag("forecast_tab_$idx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Interactive Temperature & Precipitation Chart Card
        item {
            FrostedGlassCard(accentTint = accentColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (selectedTab == 0) {
                            "INTERACTIVE 24-HOUR TEMPERATURE & RAIN PROBABILITY CHART"
                        } else {
                            "INTERACTIVE 7-DAY HIGH/LOW & PRECIPITATION CHART"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap any column on the chart to inspect detailed forecast metrics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedTab == 0 && hourly.isNotEmpty()) {
                        InteractiveHourlyChart(
                            hourly = hourly.take(12),
                            selectedIndex = selectedHourIndex.coerceIn(0, minOf(11, hourly.size - 1)),
                            onSelectIndex = { selectedHourIndex = it },
                            accentColor = accentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .testTag("interactive_hourly_chart")
                        )
                    } else if (daily.isNotEmpty()) {
                        InteractiveDailyChart(
                            daily = daily,
                            selectedIndex = selectedDayIndex.coerceIn(0, daily.size - 1),
                            onSelectIndex = { selectedDayIndex = it },
                            accentColor = accentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                                .testTag("interactive_daily_chart")
                        )
                    }
                }
            }
        }

        // Selected Hour or Day Detailed Inspection Card
        item {
            if (selectedTab == 0 && hourly.isNotEmpty()) {
                val safeIdx = selectedHourIndex.coerceIn(0, hourly.size - 1)
                val hr = hourly[safeIdx]
                SelectedHourDetailCard(
                    hour = hr,
                    settings = settings,
                    onSimulateScene = { onPreviewSceneOnDashboard(hr.sceneType) }
                )
            } else if (daily.isNotEmpty()) {
                val safeIdx = selectedDayIndex.coerceIn(0, daily.size - 1)
                val day = daily[safeIdx]
                SelectedDayDetailCard(
                    day = day,
                    settings = settings,
                    onSimulateScene = { onPreviewSceneOnDashboard(day.sceneType) }
                )
            }
        }

        // Hour-by-Hour 24h Scroll Strip + List OR 7-Day Forecast Rows
        if (selectedTab == 0) {
            item {
                Text(
                    text = "ALL 24 HOURS — TAP ANY HOUR TO INSPECT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            itemsIndexed(hourly, key = { idx, item -> "${idx}_${item.timeIso}" }) { idx, hr ->
                val isSelected = idx == selectedHourIndex
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.09f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { selectedHourIndex = idx }
                        .testTag("hourly_row_$idx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            AnimatedWeatherConditionIcon(
                                sceneType = hr.sceneType,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = hr.displayHour,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = hr.conditionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Rain ${hr.precipProbabilityPercent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = "Hum ${hr.humidityPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                            Text(
                                text = WeatherFormatter.formatTemp(hr.temperatureC, settings.tempUnit),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "7-DAY WEEKLY OUTLOOK — TAP ANY DAY TO INSPECT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            itemsIndexed(daily, key = { idx, item -> "${idx}_${item.dateIso}" }) { idx, day ->
                val isSelected = idx == selectedDayIndex
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) accentColor.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.09f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedDayIndex = idx }
                        .testTag("daily_row_$idx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(76.dp)) {
                            Text(
                                text = day.dayOfWeek,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = day.shortDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            AnimatedWeatherConditionIcon(
                                sceneType = day.sceneType,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = day.conditionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Rain ${day.precipProbabilityPercent}% · Wind ${
                                        WeatherFormatter.formatWind(day.maxWindSpeedKmh, settings.windUnit)
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = WeatherFormatter.formatTemp(day.minTempC, settings.tempUnit, includeUnit = false),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.62f)
                            )
                            Text(
                                text = "/",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                            Text(
                                text = WeatherFormatter.formatTemp(day.maxTempC, settings.tempUnit),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveHourlyChart(
    hourly: List<HourlyForecast>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val minTemp = (hourly.minOfOrNull { it.temperatureC } ?: 0.0) - 2.0
    val maxTemp = (hourly.maxOfOrNull { it.temperatureC } ?: 35.0) + 2.0
    val tempRange = (maxTemp - minTemp).coerceAtLeast(4.0)

    Canvas(
        modifier = modifier.pointerInput(hourly.size) {
            detectTapGestures { offset ->
                if (size.width > 0 && hourly.isNotEmpty()) {
                    val colWidth = size.width.toFloat() / hourly.size
                    val tappedIdx = (offset.x / colWidth).toInt().coerceIn(0, hourly.size - 1)
                    onSelectIndex(tappedIdx)
                }
            }
        }
    ) {
        val w = size.width
        val h = size.height
        if (hourly.isEmpty() || w <= 0f || h <= 0f) return@Canvas

        val colW = w / hourly.size
        val curvePath = Path()

        hourly.forEachIndexed { idx, item ->
            val cx = colW * idx + colW * 0.5f
            val normTemp = ((item.temperatureC - minTemp) / tempRange).toFloat().coerceIn(0f, 1f)
            val cy = h * 0.72f - (normTemp * h * 0.55f)

            // Precipitation Probability Bar at bottom
            val barH = (item.precipProbabilityPercent / 100f) * (h * 0.25f)
            drawRoundRect(
                color = if (idx == selectedIndex) accentColor.copy(alpha = 0.55f) else Color(0xFF38BDF8).copy(alpha = 0.22f),
                topLeft = Offset(cx - colW * 0.26f, h - barH),
                size = Size(colW * 0.52f, barH.coerceAtLeast(4f)),
                cornerRadius = CornerRadius(6f, 6f)
            )

            if (idx == 0) {
                curvePath.moveTo(cx, cy)
            } else {
                curvePath.lineTo(cx, cy)
            }

            if (idx == selectedIndex) {
                drawLine(
                    color = accentColor.copy(alpha = 0.45f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, h),
                    strokeWidth = 2f
                )
            }
        }

        drawPath(
            path = curvePath,
            color = accentColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        hourly.forEachIndexed { idx, item ->
            val cx = colW * idx + colW * 0.5f
            val normTemp = ((item.temperatureC - minTemp) / tempRange).toFloat().coerceIn(0f, 1f)
            val cy = h * 0.72f - (normTemp * h * 0.55f)
            drawCircle(
                color = if (idx == selectedIndex) Color.White else accentColor,
                radius = if (idx == selectedIndex) 7f else 4.5f,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
private fun InteractiveDailyChart(
    daily: List<DailyForecast>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val minTemp = (daily.minOfOrNull { it.minTempC } ?: 0.0) - 2.0
    val maxTemp = (daily.maxOfOrNull { it.maxTempC } ?: 35.0) + 2.0
    val tempRange = (maxTemp - minTemp).coerceAtLeast(4.0)

    Canvas(
        modifier = modifier.pointerInput(daily.size) {
            detectTapGestures { offset ->
                if (size.width > 0 && daily.isNotEmpty()) {
                    val colWidth = size.width.toFloat() / daily.size
                    val tappedIdx = (offset.x / colWidth).toInt().coerceIn(0, daily.size - 1)
                    onSelectIndex(tappedIdx)
                }
            }
        }
    ) {
        val w = size.width
        val h = size.height
        if (daily.isEmpty() || w <= 0f || h <= 0f) return@Canvas

        val colW = w / daily.size
        daily.forEachIndexed { idx, day ->
            val cx = colW * idx + colW * 0.5f
            val normHigh = ((day.maxTempC - minTemp) / tempRange).toFloat().coerceIn(0f, 1f)
            val normLow = ((day.minTempC - minTemp) / tempRange).toFloat().coerceIn(0f, 1f)
            val topY = h * 0.75f - (normHigh * h * 0.60f)
            val bottomY = h * 0.75f - (normLow * h * 0.60f)

            if (idx == selectedIndex) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.08f),
                    topLeft = Offset(colW * idx + 4f, 0f),
                    size = Size(colW - 8f, h),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }

            // High-to-Low Range Capsule
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFBBF24), Color(0xFF38BDF8)),
                    startY = topY,
                    endY = bottomY
                ),
                start = Offset(cx, topY),
                end = Offset(cx, bottomY.coerceAtLeast(topY + 12f)),
                strokeWidth = 14f,
                cap = StrokeCap.Round
            )

            // Rain probability bar along bottom
            val rainH = (day.precipProbabilityPercent / 100f) * (h * 0.18f)
            drawRoundRect(
                color = Color(0xFF38BDF8).copy(alpha = 0.45f),
                topLeft = Offset(cx - 10f, h - rainH),
                size = Size(20f, rainH.coerceAtLeast(4f)),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

@Composable
private fun SelectedHourDetailCard(
    hour: HourlyForecast,
    settings: UserSettings,
    onSimulateScene: () -> Unit
) {
    FrostedGlassCard(accentTint = Color(hour.sceneType.accentColorHex)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SELECTED HOUR TELEMETRY · ${hour.displayHour.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(hour.sceneType.accentColorHex)
                    )
                    Text(
                        text = "${WeatherFormatter.formatTemp(hour.temperatureC, settings.tempUnit)} — ${hour.conditionText}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
                AnimatedWeatherConditionIcon(sceneType = hour.sceneType, modifier = Modifier.size(42.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryPill("Feels Like", WeatherFormatter.formatTemp(hour.feelsLikeC, settings.tempUnit))
                TelemetryPill("Rain Prob", "${hour.precipProbabilityPercent}%")
                TelemetryPill("Wind", WeatherFormatter.formatWind(hour.windSpeedKmh, settings.windUnit))
                TelemetryPill("Humidity", "${hour.humidityPercent}%")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onSimulateScene,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(hour.sceneType.accentColorHex),
                    contentColor = Color(0xFF041322)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview ${hour.sceneType.title} 3D Environment on Dashboard")
            }
        }
    }
}

@Composable
private fun SelectedDayDetailCard(
    day: DailyForecast,
    settings: UserSettings,
    onSimulateScene: () -> Unit
) {
    FrostedGlassCard(accentTint = Color(day.sceneType.accentColorHex)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${day.dayOfWeek.uppercase()} (${day.shortDate.uppercase()}) OUTLOOK",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(day.sceneType.accentColorHex)
                    )
                    Text(
                        text = "High ${WeatherFormatter.formatTemp(day.maxTempC, settings.tempUnit)} · Low ${
                            WeatherFormatter.formatTemp(day.minTempC, settings.tempUnit)
                        }",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
                AnimatedWeatherConditionIcon(sceneType = day.sceneType, modifier = Modifier.size(42.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryPill("Rain Chance", "${day.precipProbabilityPercent}%")
                TelemetryPill("Max Wind", WeatherFormatter.formatWind(day.maxWindSpeedKmh, settings.windUnit))
                TelemetryPill("Sunrise", day.sunriseTime)
                TelemetryPill("Sunset", day.sunsetTime)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onSimulateScene,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(day.sceneType.accentColorHex),
                    contentColor = Color(0xFF041322)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preview ${day.sceneType.title} 3D Environment on Dashboard")
            }
        }
    }
}

@Composable
private fun TelemetryPill(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.62f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/**
 * Screen 6: Detailed Weather Analytics & 3D Atmospheric Environment Studio.
 */
@Composable
fun WeatherAnalyticsScreen(
    weatherBundle: WeatherBundle,
    settings: UserSettings,
    activeSceneOverride: WeatherSceneType?,
    onSelectSceneOverride: (WeatherSceneType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = weatherBundle.current
    val effectiveScene = activeSceneOverride ?: current.sceneType
    val accentColor = Color(effectiveScene.accentColorHex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen_root"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "DEEP METEOROLOGICAL TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
                Text(
                    text = "Weather Analytics & 3D Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
        }

        // 1. 3D Weather Environment Studio Card (Inspect all 8 3D Weather Scenes)
        item {
            FrostedGlassCard(accentTint = accentColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INTERACTIVE 3D SCENE SIMULATOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }
                        if (activeSceneOverride != null) {
                            Text(
                                text = "Restore Live API Scene",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.clickable { onSelectSceneOverride(null) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active 3D Scene: ${effectiveScene.title} — ${effectiveScene.subtitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Grid of all 8 3D Weather Environments
                    val rows = WeatherSceneType.entries.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { scene ->
                                    val isSelected = effectiveScene == scene
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) {
                                            Color(scene.accentColorHex).copy(alpha = 0.28f)
                                        } else {
                                            Color.White.copy(alpha = 0.08f)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(scene.accentColorHex) else Color.White.copy(alpha = 0.12f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                onSelectSceneOverride(
                                                    if (scene == current.sceneType) null else scene
                                                )
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AnimatedWeatherConditionIcon(
                                                sceneType = scene,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = scene.title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = if (scene == current.sceneType) "Live Current" else "3D Preset",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.6f)
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
        }

        // 2. Thermal Comfort & Dew Point Matrix
        item {
            FrostedGlassCard(accentTint = accentColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "THERMAL COMFORT & PSYCHROMETRIC PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryPill("Ambient Temp", WeatherFormatter.formatTemp(current.temperatureC, settings.tempUnit))
                        TelemetryPill("Apparent Feel", WeatherFormatter.formatTemp(current.feelsLikeC, settings.tempUnit))
                        TelemetryPill("Dew Point", WeatherFormatter.formatTemp(current.dewPointC, settings.tempUnit))
                        TelemetryPill("Humidity", "${current.humidityPercent}%")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Comfort Assessment: ${WeatherFormatter.humidityComfortLabel(current.humidityPercent)}. Thermal variance between ambient and apparent temperature is ${
                            String.format("%.1f", kotlin.math.abs(current.feelsLikeC - current.temperatureC))
                        }°.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 3. Wind Vector & Barometric Pressure Deep Dive
        item {
            FrostedGlassCard(accentTint = accentColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AERODYNAMIC & BAROMETRIC TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${WeatherFormatter.formatWind(current.windSpeedKmh, settings.windUnit)} · ${
                                WeatherFormatter.windDirectionCardinal(current.windDirectionDeg)
                            } (${current.windDirectionDeg}°)",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Surface Pressure: ${current.pressureHpa.toInt()} hPa (${
                                WeatherFormatter.pressureTrendLabel(current.pressureHpa)
                            })",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    WindCompassDial(
                        windDirectionDeg = current.windDirectionDeg,
                        modifier = Modifier.size(68.dp)
                    )
                }
            }
        }

        // 4. 7-Day Humidity & UV Trend Progression
        item {
            FrostedGlassCard(accentTint = accentColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "WEEKLY UV RADIATION & PRECIPITATION RISK MATRIX",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    weatherBundle.daily.forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day.dayOfWeek,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.width(56.dp)
                            )
                            LinearProgressIndicator(
                                progress = { (day.precipProbabilityPercent / 100f).coerceIn(0.04f, 1f) },
                                color = Color(0xFF38BDF8),
                                trackColor = Color.White.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "Rain ${day.precipProbabilityPercent}% · UV ${day.uvIndexMax.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}
