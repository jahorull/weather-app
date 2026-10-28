package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.model.GraphicsQuality
import com.example.model.SavedLocation
import com.example.model.TempUnit
import com.example.model.ThemeMode
import com.example.model.UserSettings
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.example.model.WindUnit
import com.example.ui.components.AnimatedWeatherConditionIcon
import com.example.ui.components.FrostedGlassCard

/**
 * Screen 7: Saved Locations & Live City Search Screen with explicit empty/not-found feedback.
 */
@Composable
fun SavedLocationsScreen(
    savedLocations: List<SavedLocation>,
    activeLocationId: String,
    searchQuery: String,
    searchResults: List<SavedLocation>,
    isSearching: Boolean,
    searchErrorMessage: String?,
    settings: UserSettings,
    onSearchQueryChange: (String) -> Unit,
    onSelectCity: (SavedLocation) -> Unit,
    onSaveCity: (SavedLocation) -> Unit,
    onRemoveCity: (String) -> Unit,
    onDetectCurrentLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("locations_screen_root"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GLOBAL CITY DIRECTORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Saved Locations & Search",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }

                Button(
                    onClick = onDetectCurrentLocation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("locations_gps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("My GPS")
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_cities_hint),
                        color = Color.White.copy(alpha = 0.55f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8)
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color.White
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.10f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.20f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city_search_input")
            )
        }

        // Live Geocoding Search Results & Clear Not-Found Feedback
        if (searchQuery.trim().length >= 2) {
            item {
                Text(
                    text = "LIVE GEOCODING RESULTS (${searchResults.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8)
                )
            }

            if (!isSearching && searchResults.isEmpty()) {
                item {
                    FrostedGlassCard(
                        cornerRadius = 20.dp,
                        accentTint = Color(0xFFFBBF24),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("city_not_found_card")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = searchErrorMessage
                                    ?: "No city found matching \"${searchQuery.trim()}\".",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Check the spelling or search by a major city or regional name.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.72f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(searchResults, key = { "search_${it.id}" }) { resultCity ->
                val alreadySaved = savedLocations.any {
                    it.cityName.equals(resultCity.cityName, ignoreCase = true) &&
                        it.country.equals(resultCity.country, ignoreCase = true)
                }
                FrostedGlassCard(cornerRadius = 20.dp, accentTint = Color(0xFF38BDF8)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCity(resultCity) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = resultCity.cityName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = listOf(resultCity.region, resultCity.country)
                                    .filter { it.isNotBlank() }
                                    .joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            Text(
                                text = "${String.format("%.2f", resultCity.latitude)}°, ${String.format("%.2f", resultCity.longitude)}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!alreadySaved) {
                                Button(
                                    onClick = { onSaveCity(resultCity) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF38BDF8),
                                        contentColor = Color(0xFF041322)
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add & View")
                                }
                            } else {
                                Text(
                                    text = "Saved",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Saved Favorite Cities List
        item {
            Text(
                text = "SAVED FAVORITE LOCATIONS (${savedLocations.size})",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }

        items(savedLocations, key = { it.id }) { city ->
            val isActive = city.id == activeLocationId
            val scene = city.cachedSceneType ?: WeatherSceneType.SUNNY
            val accent = Color(scene.accentColorHex)

            FrostedGlassCard(
                cornerRadius = 22.dp,
                accentTint = if (isActive) accent else Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectCity(city) }
                    .testTag("saved_city_card_${city.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AnimatedWeatherConditionIcon(
                            sceneType = scene,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = city.cityName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accent.copy(alpha = 0.28f)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = listOf(city.region, city.country)
                                    .filter { it.isNotBlank() }
                                    .joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            Text(
                                text = city.cachedCondition ?: "Tap to fetch live weather",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        city.cachedTempC?.let { temp ->
                            Text(
                                text = WeatherFormatter.formatTemp(temp, settings.tempUnit),
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (savedLocations.size > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onRemoveCity(city.id) },
                                modifier = Modifier.testTag("delete_city_${city.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove ${city.cityName}",
                                    tint = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 8: Settings Screen for Units, Themes, 3D Animation Quality, Reduced Motion Accessibility,
 * Location Permissions, Auto-Refresh Interval, and API Key / Build Status.
 */
@Composable
fun SettingsScreen(
    settings: UserSettings,
    systemReducedMotionDetected: Boolean,
    hasLocationPermission: Boolean,
    onTempUnitChange: (TempUnit) -> Unit,
    onWindUnitChange: (WindUnit) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGraphicsQualityChange: (GraphicsQuality) -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
    onAutoRefreshChange: (Int) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customKeyConfigured = BuildConfig.WEATHER_API_KEY.isNotBlank() &&
        BuildConfig.WEATHER_API_KEY != "DEFAULT_OPEN_METEO_LIVE"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_root"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "SYSTEM CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "Units, 3D Engine & Preferences",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
        }

        // 1. Temperature Units (°C vs °F)
        item {
            FrostedGlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = Color(0xFFFBBF24))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TEMPERATURE UNIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TempUnit.entries.forEach { unit ->
                            val selected = settings.tempUnit == unit
                            ChoiceChipButton(
                                label = unit.label,
                                selected = selected,
                                onClick = { onTempUnitChange(unit) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("temp_unit_${unit.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 2. Wind Speed Units (km/h, mph, m/s)
        item {
            FrostedGlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WIND SPEED UNIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WindUnit.entries.forEach { unit ->
                            val selected = settings.windUnit == unit
                            ChoiceChipButton(
                                label = unit.symbol,
                                selected = selected,
                                onClick = { onWindUnitChange(unit) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wind_unit_${unit.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 3. 3D Rendering, Low-Performance Mode & Reduced Motion Accessibility
        item {
            FrostedGlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "3D RENDERING, ACCESSIBILITY & LOW-END DEVICE MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select Low-End 2D Mode on older devices to reduce particles and disable 3D ray shafts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GraphicsQuality.entries.forEach { quality ->
                            val selected = settings.graphicsQuality == quality
                            ChoiceChipButton(
                                label = quality.label,
                                selected = selected,
                                onClick = { onGraphicsQualityChange(quality) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("graphics_quality_${quality.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessibilityNew,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Reduced Motion Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = if (systemReducedMotionDetected) {
                                        "System Animator Scale is OFF (Auto-activated)"
                                    } else {
                                        "Freeze 3D tilt sway, lightning flashes & continuous motion"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.68f)
                                )
                            }
                        }
                        Switch(
                            checked = settings.reducedMotion || systemReducedMotionDetected,
                            onCheckedChange = onReducedMotionChange,
                            modifier = Modifier.testTag("reduced_motion_switch")
                        )
                    }
                }
            }
        }

        // 4. Visual Theme Mode (Dynamic Weather, Dark, Light)
        item {
            FrostedGlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "INTERFACE THEME & ATMOSPHERIC PALETTE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            ChoiceChipButton(
                                label = mode.label,
                                selected = settings.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 5. Auto-Refresh Interval & Severe Weather Alerts
        item {
            FrostedGlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "AUTOMATIC BACKGROUND REFRESH INTERVAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15 to "15 min", 30 to "30 min", 60 to "60 min").forEach { (mins, label) ->
                            ChoiceChipButton(
                                label = label,
                                selected = settings.autoRefreshMinutes == mins,
                                onClick = { onAutoRefreshChange(mins) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF34D399))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Severe Weather Alerts",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Highlight active NWS & WMO threshold warnings",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.68f)
                                )
                            }
                        }
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = onNotificationsChange
                        )
                    }
                }
            }
        }

        // 6. Location Permission & API Configuration Status
        item {
            FrostedGlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "LOCATION SERVICES & LIVE API CREDENTIALS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasLocationPermission) {
                                    "GPS Location Permission: Granted"
                                } else {
                                    "GPS Location Permission: Not Granted (Using Saved City)"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Used solely to detect local weather for your coordinates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        if (!hasLocationPermission) {
                            Button(
                                onClick = onRequestLocationPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38BDF8),
                                    contentColor = Color(0xFF041322)
                                )
                            ) {
                                Text("Grant")
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Location Granted",
                                tint = Color(0xFF34D399)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (customKeyConfigured) {
                                "Weather API: Custom Secret Key Active (BuildConfig.WEATHER_API_KEY)"
                            } else {
                                "Weather API: Live Open-Meteo Forecast, Air Quality & NWS Alerts Active (Optional WEATHER_API_KEY supported via AI Studio Secrets panel)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Color(0xFF38BDF8).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
