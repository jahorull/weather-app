package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.DailyForecast
import com.example.model.HourlyForecast
import com.example.model.SavedLocation
import com.example.model.UserSettings
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.example.ui.components.AnimatedWeatherConditionIcon
import com.example.ui.components.FrostedGlassCard
import com.example.ui.components.WindCompassDial

/**
 * Screens 3 & 4: Upgraded Hourly (24-Hour) and Weekly (7-Day) Forecasts with Cubic-Bezier
 * Interactive Charts, drag/tap scrubbing, and null-safe metric inspection.
 */
@Composable
fun ForecastsScreen(
    activeLocation: SavedLocation,
    weatherBundle: WeatherBundle?,
    settings: UserSettings,
    onRefreshWeather: () -> Unit,
    onPreviewSceneOnDashboard: (WeatherSceneType) -> Unit,
    onAskAiAboutForecast: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = 24-Hour, 1 = 7-Day
    var selectedHourIndex by remember { mutableIntStateOf(0) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }

    val hourly = weatherBundle?.hourly.orEmpty()
    val daily = weatherBundle?.daily.orEmpty()
    val accentColor = Color(
        (weatherBundle?.current?.sceneType ?: WeatherSceneType.SUNNY).accentColorHex
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("forecasts_screen_root"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "${activeLocation.cityName.uppercase()} METEOROLOGY",
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

        if (weatherBundle == null) {
            item {
                FrostedGlassCard(accentTint = accentColor) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Forecast Data Unavailable Offline",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Connect to the internet and tap Retry to load live 24-hour and 7-day forecasts for ${activeLocation.cityName}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onRefreshWeather,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF041322)
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Forecast Sync")
                        }
                    }
                }
            }
        } else {
            // Interactive Cubic-Bezier Temperature & Precipitation Chart Card
            item {
                FrostedGlassCard(accentTint = accentColor) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (selectedTab == 0) {
                                "INTERACTIVE 24-HOUR CUBIC TEMPERATURE & RAIN PROBABILITY CHART"
                            } else {
                                "INTERACTIVE 7-DAY HIGH/LOW & PRECIPITATION CHART"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap or drag horizontally across the chart to inspect detailed forecast telemetry",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedTab == 0 && hourly.isNotEmpty()) {
                            val slice = hourly.take(12)
                            val safeIndex = selectedHourIndex.coerceIn(0, slice.size - 1)
                            val selectedHr = slice[safeIndex]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${selectedHr.displayHour}: ${
                                        WeatherFormatter.formatTemp(selectedHr.temperatureC, settings.tempUnit)
                                    } (${selectedHr.conditionText})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Rain: ${WeatherFormatter.formatNullablePercent(selectedHr.precipProbabilityPercent)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            InteractiveHourlyChart(
                                hourly = slice,
                                selectedIndex = safeIndex,
                                onSelectIndex = { selectedHourIndex = it },
                                accentColor = accentColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                                    .testTag("interactive_hourly_chart")
                            )
                        } else if (daily.isNotEmpty()) {
                            val safeDayIdx = selectedDayIndex.coerceIn(0, daily.size - 1)
                            val selectedDay = daily[safeDayIdx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${selectedDay.dayOfWeek}: High ${
                                        WeatherFormatter.formatTemp(selectedDay.maxTempC, settings.tempUnit)
                                    } / Low ${WeatherFormatter.formatTemp(selectedDay.minTempC, settings.tempUnit)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Rain: ${WeatherFormatter.formatNullablePercent(selectedDay.precipProbabilityPercent)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                            InteractiveDailyChart(
                                daily = daily,
                                selectedIndex = safeDayIdx,
                                onSelectIndex = { selectedDayIndex = it },
                                accentColor = accentColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
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
                        cityName = activeLocation.cityName,
                        hour = hr,
                        settings = settings,
                        onSimulateScene = { onPreviewSceneOnDashboard(hr.sceneType) },
                        onAskAiAboutHour = {
                            onAskAiAboutForecast(
                                "Explain the forecast for ${activeLocation.cityName} at ${hr.displayHour} (${hr.conditionText}, ${
                                    WeatherFormatter.formatTemp(hr.temperatureC, settings.tempUnit)
                                }, Feels Like ${WeatherFormatter.formatTemp(hr.feelsLikeC, settings.tempUnit)}, Rain probability ${
                                    WeatherFormatter.formatNullablePercent(hr.precipProbabilityPercent)
                                }, Wind ${WeatherFormatter.formatWind(hr.windSpeedKmh, settings.windUnit)}) and any practical recommendations."
                            )
                        }
                    )
                } else if (daily.isNotEmpty()) {
                    val safeIdx = selectedDayIndex.coerceIn(0, daily.size - 1)
                    val day = daily[safeIdx]
                    SelectedDayDetailCard(
                        cityName = activeLocation.cityName,
                        day = day,
                        settings = settings,
                        onSimulateScene = { onPreviewSceneOnDashboard(day.sceneType) },
                        onAskAiAboutDay = {
                            onAskAiAboutForecast(
                                "Explain the ${day.dayOfWeek} (${day.shortDate}) forecast for ${activeLocation.cityName} (${day.conditionText}, High ${
                                    WeatherFormatter.formatTemp(day.maxTempC, settings.tempUnit)
                                } / Low ${WeatherFormatter.formatTemp(day.minTempC, settings.tempUnit)}, Rain chance ${
                                    WeatherFormatter.formatNullablePercent(day.precipProbabilityPercent)
                                }, Max Wind ${WeatherFormatter.formatWind(day.maxWindSpeedKmh, settings.windUnit)}) including forecast uncertainty and daily recommendations."
                            )
                        }
                    )
                }
            }

            // Hour-by-Hour 24h List OR 7-Day Forecast Rows
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
                                        text = "Rain ${WeatherFormatter.formatNullablePercent(hr.precipProbabilityPercent)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Text(
                                        text = "Hum ${WeatherFormatter.formatNullablePercent(hr.humidityPercent)}",
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
                                        text = "Rain ${WeatherFormatter.formatNullablePercent(day.precipProbabilityPercent)} · Wind ${
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
        modifier = modifier
            .pointerInput(hourly.size) {
                detectTapGestures { offset ->
                    if (size.width > 0 && hourly.isNotEmpty()) {
                        val colWidth = size.width.toFloat() / hourly.size
                        val tappedIdx = (offset.x / colWidth).toInt().coerceIn(0, hourly.size - 1)
                        onSelectIndex(tappedIdx)
                    }
                }
            }
            .pointerInput(hourly.size) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0 && hourly.isNotEmpty()) {
                        val colWidth = size.width.toFloat() / hourly.size
                        val draggedIdx = (change.position.x / colWidth).toInt().coerceIn(0, hourly.size - 1)
                        onSelectIndex(draggedIdx)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height
        if (hourly.isEmpty() || w <= 0f || h <= 0f) return@Canvas

        val colW = w / hourly.size
        val points = hourly.mapIndexed { idx, item ->
            val cx = colW * idx + colW * 0.5f
            val normTemp = ((item.temperatureC - minTemp) / tempRange).toFloat().coerceIn(0f, 1f)
            val cy = h * 0.70f - (normTemp * h * 0.54f)
            Offset(cx, cy)
        }

        val curvePath = Path()
        val fillPath = Path()

        points.forEachIndexed { idx, pt ->
            if (idx == 0) {
                curvePath.moveTo(pt.x, pt.y)
                fillPath.moveTo(pt.x, h * 0.76f)
                fillPath.lineTo(pt.x, pt.y)
            } else {
                val prev = points[idx - 1]
                val ctrlX = (prev.x + pt.x) * 0.5f
                curvePath.cubicTo(ctrlX, prev.y, ctrlX, pt.y, pt.x, pt.y)
                fillPath.cubicTo(ctrlX, prev.y, ctrlX, pt.y, pt.x, pt.y)
            }
        }

        if (points.isNotEmpty()) {
            fillPath.lineTo(points.last().x, h * 0.76f)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.34f),
                        accentColor.copy(alpha = 0.02f)
                    ),
                    startY = 0f,
                    endY = h * 0.76f
                )
            )
        }

        hourly.forEachIndexed { idx, item ->
            val cx = points[idx].x
            val precip = item.precipProbabilityPercent ?: 0
            val barH = (precip / 100f) * (h * 0.22f)
            drawRoundRect(
                color = if (idx == selectedIndex) accentColor.copy(alpha = 0.65f) else Color(0xFF38BDF8).copy(alpha = 0.24f),
                topLeft = Offset(cx - colW * 0.26f, h - barH),
                size = Size(colW * 0.52f, barH.coerceAtLeast(4f)),
                cornerRadius = CornerRadius(6f, 6f)
            )

            if (idx == selectedIndex) {
                drawLine(
                    color = accentColor.copy(alpha = 0.55f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, h),
                    strokeWidth = 2.2f
                )
            }
        }

        drawPath(
            path = curvePath,
            color = accentColor,
            style = Stroke(width = 4.2f, cap = StrokeCap.Round)
        )

        points.forEachIndexed { idx, pt ->
            if (idx == selectedIndex) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.35f),
                    radius = 14f,
                    center = pt
                )
            }
            drawCircle(
                color = if (idx == selectedIndex) Color.White else accentColor,
                radius = if (idx == selectedIndex) 7.5f else 4.5f,
                center = pt
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
        modifier = modifier
            .pointerInput(daily.size) {
                detectTapGestures { offset ->
                    if (size.width > 0 && daily.isNotEmpty()) {
                        val colWidth = size.width.toFloat() / daily.size
                        val tappedIdx = (offset.x / colWidth).toInt().coerceIn(0, daily.size - 1)
                        onSelectIndex(tappedIdx)
                    }
                }
            }
            .pointerInput(daily.size) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (size.width > 0 && daily.isNotEmpty()) {
                        val colWidth = size.width.toFloat() / daily.size
                        val draggedIdx = (change.position.x / colWidth).toInt().coerceIn(0, daily.size - 1)
                        onSelectIndex(draggedIdx)
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
                    color = Color.White.copy(alpha = 0.09f),
                    topLeft = Offset(colW * idx + 4f, 0f),
                    size = Size(colW - 8f, h),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }

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

            val precip = day.precipProbabilityPercent ?: 0
            val rainH = (precip / 100f) * (h * 0.18f)
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
    cityName: String,
    hour: HourlyForecast,
    settings: UserSettings,
    onSimulateScene: () -> Unit,
    onAskAiAboutHour: () -> Unit
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
                TelemetryPill("Rain Prob", WeatherFormatter.formatNullablePercent(hour.precipProbabilityPercent))
                TelemetryPill("Wind", WeatherFormatter.formatWind(hour.windSpeedKmh, settings.windUnit))
                TelemetryPill("Humidity", WeatherFormatter.formatNullablePercent(hour.humidityPercent))
            }
            Spacer(modifier = Modifier.height(10.dp))
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
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ask_ai_forecast_period_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ask AI About ${hour.displayHour}")
                }
                Button(
                    onClick = onSimulateScene,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(hour.sceneType.accentColorHex).copy(alpha = 0.25f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Preview 3D Scene")
                }
            }
        }
    }
}

@Composable
private fun SelectedDayDetailCard(
    cityName: String,
    day: DailyForecast,
    settings: UserSettings,
    onSimulateScene: () -> Unit,
    onAskAiAboutDay: () -> Unit
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
                TelemetryPill("Rain Chance", WeatherFormatter.formatNullablePercent(day.precipProbabilityPercent))
                TelemetryPill("Max Wind", WeatherFormatter.formatWind(day.maxWindSpeedKmh, settings.windUnit))
                TelemetryPill("Sunrise", day.sunriseTime)
                TelemetryPill("Sunset", day.sunsetTime)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAskAiAboutDay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF041322)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ask_ai_forecast_period_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ask AI About ${day.dayOfWeek}")
                }
                Button(
                    onClick = onSimulateScene,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(day.sceneType.accentColorHex).copy(alpha = 0.25f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Preview 3D Scene")
                }
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
 * Screen 6: Detailed Weather Analytics, Air Quality Breakdown & 3D Atmospheric Studio.
 */
@Composable
fun WeatherAnalyticsScreen(
    weatherBundle: WeatherBundle?,
    settings: UserSettings,
    activeSceneOverride: WeatherSceneType?,
    onSelectSceneOverride: (WeatherSceneType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = weatherBundle?.current
    val effectiveScene = activeSceneOverride ?: current?.sceneType ?: WeatherSceneType.SUNNY
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
                                                onSelectSceneOverride(scene)
                                            }
                                            .testTag("analytics_scene_chip_${scene.name.lowercase()}")
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
                                                    text = if (scene == current?.sceneType && activeSceneOverride == null) "Live Current" else "3D Preset",
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

        // 2. Air Quality & Pollutant Breakdown Card
        item {
            val aq = weatherBundle?.airQuality
            val aqiColor = Color(WeatherFormatter.aqiColorHex(aq?.usAqi))
            FrostedGlassCard(accentTint = aqiColor) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = null,
                            tint = aqiColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AIR QUALITY & POLLUTANT SPECTRUM",
                            style = MaterialTheme.typography.labelSmall,
                            color = aqiColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryPill("US AQI", aq?.usAqi?.toString() ?: "Unavailable")
                        TelemetryPill("Category", WeatherFormatter.aqiCategoryLabel(aq?.usAqi))
                        TelemetryPill("PM2.5", WeatherFormatter.formatNullablePollutant(aq?.pm25))
                        TelemetryPill("PM10", WeatherFormatter.formatNullablePollutant(aq?.pm10))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        TelemetryPill("Ozone (O₃)", WeatherFormatter.formatNullablePollutant(aq?.ozone))
                        TelemetryPill("NO₂", WeatherFormatter.formatNullablePollutant(aq?.nitrogenDioxide))
                    }
                }
            }
        }

        if (current != null) {
            // 3. Thermal Comfort & Dew Point Matrix
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
                            TelemetryPill(
                                "Dew Point",
                                current.dewPointC?.let { WeatherFormatter.formatTemp(it, settings.tempUnit) } ?: "Unavailable"
                            )
                            TelemetryPill("Humidity", WeatherFormatter.formatNullablePercent(current.humidityPercent))
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

            // 4. Wind Vector & Barometric Pressure Deep Dive
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
                            val dirLabel = current.windDirectionDeg?.let {
                                "${WeatherFormatter.windDirectionCardinal(it)} ($it°)"
                            } ?: "Direction Unavailable"
                            Text(
                                text = "${WeatherFormatter.formatWind(current.windSpeedKmh, settings.windUnit)} · $dirLabel",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Surface Pressure: ${WeatherFormatter.formatNullablePressure(current.pressureHpa)} (${
                                    WeatherFormatter.pressureTrendLabel(current.pressureHpa)
                                })",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                        WindCompassDial(
                            windDirectionDeg = current.windDirectionDeg ?: 0,
                            modifier = Modifier.size(68.dp)
                        )
                    }
                }
            }

            // 5. 7-Day UV & Precipitation Risk Matrix
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
                            val precip = day.precipProbabilityPercent ?: 0
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
                                    progress = { (precip / 100f).coerceIn(0.04f, 1f) },
                                    color = Color(0xFF38BDF8),
                                    trackColor = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                val uvStr = day.uvIndexMax?.toInt()?.toString() ?: "N/A"
                                Text(
                                    text = "Rain ${WeatherFormatter.formatNullablePercent(day.precipProbabilityPercent)} · UV $uvStr",
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
}
