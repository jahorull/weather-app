package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.GraphicsQuality
import com.example.model.TempUnit
import com.example.model.ThemeMode
import com.example.model.UserSettings
import com.example.model.WindUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.atmosDataStore by preferencesDataStore(name = "atmos_3d_settings")

class UserSettingsStore(private val context: Context) {

    private object Keys {
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val WIND_UNIT = stringPreferencesKey("wind_unit")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val AUTO_REFRESH_MINUTES = intPreferencesKey("auto_refresh_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ACTIVE_LOCATION_ID = stringPreferencesKey("active_location_id")
        val PERSONALIZED_ALERTS_ENABLED = booleanPreferencesKey("personalized_alerts_enabled")
        val RAIN_ALERT_THRESHOLD = intPreferencesKey("rain_alert_threshold_percent")
        val TEMP_ALERT_THRESHOLD = intPreferencesKey("temp_alert_threshold_c")
        val WIND_ALERT_THRESHOLD = intPreferencesKey("wind_alert_threshold_kmh")
        val AQI_ALERT_THRESHOLD = intPreferencesKey("aqi_alert_threshold_us")
        val MONITOR_ALL_FAVORITE_CITIES = booleanPreferencesKey("monitor_all_favorite_cities")
    }

    val settingsFlow: Flow<UserSettings> = context.atmosDataStore.data.map { prefs ->
        UserSettings(
            tempUnit = prefs[Keys.TEMP_UNIT]?.let {
                runCatching { TempUnit.valueOf(it) }.getOrNull()
            } ?: TempUnit.CELSIUS,
            windUnit = prefs[Keys.WIND_UNIT]?.let {
                runCatching { WindUnit.valueOf(it) }.getOrNull()
            } ?: WindUnit.KMH,
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.DYNAMIC,
            graphicsQuality = prefs[Keys.GRAPHICS_QUALITY]?.let {
                runCatching { GraphicsQuality.valueOf(it) }.getOrNull()
            } ?: GraphicsQuality.HIGH_3D,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false,
            autoRefreshMinutes = prefs[Keys.AUTO_REFRESH_MINUTES] ?: 30,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            activeLocationId = prefs[Keys.ACTIVE_LOCATION_ID] ?: "dhaka_bd",
            personalizedAlertsEnabled = prefs[Keys.PERSONALIZED_ALERTS_ENABLED] ?: true,
            rainAlertThresholdPercent = prefs[Keys.RAIN_ALERT_THRESHOLD] ?: 60,
            tempAlertThresholdC = prefs[Keys.TEMP_ALERT_THRESHOLD] ?: 35,
            windAlertThresholdKmh = prefs[Keys.WIND_ALERT_THRESHOLD] ?: 40,
            aqiAlertThresholdUs = prefs[Keys.AQI_ALERT_THRESHOLD] ?: 100,
            monitorAllFavoriteCities = prefs[Keys.MONITOR_ALL_FAVORITE_CITIES] ?: true
        )
    }

    suspend fun setTempUnit(unit: TempUnit) {
        context.atmosDataStore.edit { it[Keys.TEMP_UNIT] = unit.name }
    }

    suspend fun setWindUnit(unit: WindUnit) {
        context.atmosDataStore.edit { it[Keys.WIND_UNIT] = unit.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.atmosDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setGraphicsQuality(quality: GraphicsQuality) {
        context.atmosDataStore.edit { it[Keys.GRAPHICS_QUALITY] = quality.name }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.atmosDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun setAutoRefreshMinutes(minutes: Int) {
        context.atmosDataStore.edit { it[Keys.AUTO_REFRESH_MINUTES] = minutes }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.atmosDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setActiveLocationId(locationId: String) {
        context.atmosDataStore.edit { it[Keys.ACTIVE_LOCATION_ID] = locationId }
    }

    suspend fun setPersonalizedAlertsEnabled(enabled: Boolean) {
        context.atmosDataStore.edit { it[Keys.PERSONALIZED_ALERTS_ENABLED] = enabled }
    }

    suspend fun setRainAlertThreshold(percent: Int) {
        context.atmosDataStore.edit { it[Keys.RAIN_ALERT_THRESHOLD] = percent.coerceIn(20, 95) }
    }

    suspend fun setTempAlertThreshold(tempC: Int) {
        context.atmosDataStore.edit { it[Keys.TEMP_ALERT_THRESHOLD] = tempC.coerceIn(20, 48) }
    }

    suspend fun setWindAlertThreshold(windKmh: Int) {
        context.atmosDataStore.edit { it[Keys.WIND_ALERT_THRESHOLD] = windKmh.coerceIn(20, 100) }
    }

    suspend fun setAqiAlertThreshold(usAqi: Int) {
        context.atmosDataStore.edit { it[Keys.AQI_ALERT_THRESHOLD] = usAqi.coerceIn(50, 300) }
    }

    suspend fun setMonitorAllFavoriteCities(enabled: Boolean) {
        context.atmosDataStore.edit { it[Keys.MONITOR_ALL_FAVORITE_CITIES] = enabled }
    }
}
