package com.example.model

import kotlin.math.roundToInt

/**
 * The 8 distinct 3D weather environments specified for Atmos 3D.
 */
enum class WeatherSceneType(
    val title: String,
    val subtitle: String,
    val skyTopHex: Long,
    val skyMidHex: Long,
    val skyBottomHex: Long,
    val accentColorHex: Long,
    val secondaryColorHex: Long
) {
    SUNNY(
        title = "Sunny & Clear",
        subtitle = "Radiant 3D solar sphere, volumetric light shafts & soft cumulus",
        skyTopHex = 0xFF073B6B,
        skyMidHex = 0xFF0284C7,
        skyBottomHex = 0xFF38BDF8,
        accentColorHex = 0xFFFBBF24,
        secondaryColorHex = 0xFFFEF08A
    ),
    CLOUDY(
        title = "Partly Cloudy",
        subtitle = "Multi-layered 3D volumetric cloud banks with atmospheric depth",
        skyTopHex = 0xFF17253B,
        skyMidHex = 0xFF334155,
        skyBottomHex = 0xFF64748B,
        accentColorHex = 0xFF7DD3FC,
        secondaryColorHex = 0xFFE2E8F0
    ),
    RAINY(
        title = "Rain Showers",
        subtitle = "3D rain particle vectors, wet-glass droplet refraction & ripples",
        skyTopHex = 0xFF091324,
        skyMidHex = 0xFF1E293B,
        skyBottomHex = 0xFF334155,
        accentColorHex = 0xFF38BDF8,
        secondaryColorHex = 0xFF93C5FD
    ),
    THUNDERSTORM(
        title = "Thunderstorm",
        subtitle = "Dark anvil clouds, branching 3D lightning arcs & heavy downpour",
        skyTopHex = 0xFF050814,
        skyMidHex = 0xFF161834,
        skyBottomHex = 0xFF2B234A,
        accentColorHex = 0xFFA78BFA,
        secondaryColorHex = 0xFF38BDF8
    ),
    SNOWY(
        title = "Snowfall",
        subtitle = "3D depth-sorted snowflakes, crystalline frost & alpine glow",
        skyTopHex = 0xFF0D1D35,
        skyMidHex = 0xFF1E3A5F,
        skyBottomHex = 0xFF476C9B,
        accentColorHex = 0xFF67E8F9,
        secondaryColorHex = 0xFFF8FAFC
    ),
    FOGGY(
        title = "Atmospheric Fog",
        subtitle = "Layered drifting mist bands & diffused depth visibility",
        skyTopHex = 0xFF1A2433,
        skyMidHex = 0xFF3B4B5E,
        skyBottomHex = 0xFF64748B,
        accentColorHex = 0xFFCBD5E1,
        secondaryColorHex = 0xFF94A3B8
    ),
    SUNSET(
        title = "Golden Sunset",
        subtitle = "Warm twilight horizon, purple-amber sky gradients & solar rim glow",
        skyTopHex = 0xFF1E1B4B,
        skyMidHex = 0xFF7C2D12,
        skyBottomHex = 0xFFEA580C,
        accentColorHex = 0xFFFB923C,
        secondaryColorHex = 0xFFF472B6
    ),
    NIGHT(
        title = "Starlit Night",
        subtitle = "3D cratered moon, parallax starfield, auroral mist & nocturnal clouds",
        skyTopHex = 0xFF030712,
        skyMidHex = 0xFF091326,
        skyBottomHex = 0xFF172554,
        accentColorHex = 0xFF22D3EE,
        secondaryColorHex = 0xFFA5B4FC
    );

    companion object {
        /**
         * Maps WMO weather interpretation codes and time-of-day into the 8 3D weather scenes.
         */
        fun fromWmoCode(
            wmoCode: Int,
            isDay: Boolean = true,
            hourOfDay: Int = 12
        ): WeatherSceneType {
            return when (wmoCode) {
                95, 96, 99 -> THUNDERSTORM
                71, 73, 75, 77, 85, 86 -> SNOWY
                51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> RAINY
                45, 48 -> FOGGY
                1, 2, 3 -> {
                    if (!isDay) NIGHT
                    else if (hourOfDay in 17..19) SUNSET
                    else CLOUDY
                }
                else -> {
                    if (!isDay) NIGHT
                    else if (hourOfDay in 17..19 || hourOfDay in 5..6) SUNSET
                    else SUNNY
                }
            }
        }

        fun conditionLabelFromWmo(wmoCode: Int, isDay: Boolean = true, hourOfDay: Int = 12): String {
            return when (wmoCode) {
                0 -> if (!isDay) "Clear Night" else if (hourOfDay in 17..19) "Sunset Sky" else "Sunny"
                1 -> if (!isDay) "Mostly Clear" else "Mostly Sunny"
                2 -> "Partly Cloudy"
                3 -> "Overcast"
                45, 48 -> "Dense Fog"
                51, 53, 55 -> "Light Drizzle"
                56, 57 -> "Freezing Drizzle"
                61, 63 -> "Moderate Rain"
                65 -> "Heavy Rain"
                66, 67 -> "Freezing Rain"
                71, 73 -> "Snowfall"
                75, 77 -> "Heavy Snow"
                80, 81, 82 -> "Rain Showers"
                85, 86 -> "Snow Showers"
                95 -> "Thunderstorm"
                96, 99 -> "Severe Storm & Hail"
                else -> if (isDay) "Partly Cloudy" else "Clear Night"
            }
        }
    }
}

enum class TempUnit(val symbol: String, val label: String) {
    CELSIUS("°C", "Celsius (°C)"),
    FAHRENHEIT("°F", "Fahrenheit (°F)")
}

enum class WindUnit(val symbol: String, val label: String) {
    KMH("km/h", "Kilometers/h (km/h)"),
    MPH("mph", "Miles/h (mph)"),
    MS("m/s", "Meters/sec (m/s)")
}

enum class ThemeMode(val label: String) {
    DYNAMIC("Dynamic Weather"),
    DARK("Deep Space Dark"),
    LIGHT("Daylight Glass")
}

enum class GraphicsQuality(val label: String, val particleMultiplier: Float) {
    HIGH_3D("Ultra 3D Volumetric", 1.0f),
    BALANCED_3D("Balanced 3D (60 FPS)", 0.6f),
    FALLBACK_2D("2D Battery Saver", 0.25f)
}

data class UserSettings(
    val tempUnit: TempUnit = TempUnit.CELSIUS,
    val windUnit: WindUnit = WindUnit.KMH,
    val themeMode: ThemeMode = ThemeMode.DYNAMIC,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH_3D,
    val autoRefreshMinutes: Int = 30,
    val notificationsEnabled: Boolean = true,
    val activeLocationId: String = "dhaka_bd"
)

data class SavedLocation(
    val id: String,
    val cityName: String,
    val region: String = "",
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String = "auto",
    val isCurrentLocation: Boolean = false,
    val isFavorite: Boolean = true,
    val cachedTempC: Double? = null,
    val cachedCondition: String? = null,
    val cachedSceneType: WeatherSceneType? = null
)

data class CurrentWeather(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val highC: Double,
    val lowC: Double,
    val conditionCode: Int,
    val conditionText: String,
    val sceneType: WeatherSceneType,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val uvIndex: Double,
    val visibilityKm: Double,
    val pressureHpa: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val isDay: Boolean,
    val dewPointC: Double = (temperatureC - ((100 - humidityPercent) / 5.0))
)

data class HourlyForecast(
    val timeIso: String,
    val displayHour: String,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val precipProbabilityPercent: Int,
    val windSpeedKmh: Double,
    val humidityPercent: Int,
    val uvIndex: Double,
    val conditionCode: Int,
    val conditionText: String,
    val sceneType: WeatherSceneType
)

data class DailyForecast(
    val dateIso: String,
    val dayOfWeek: String,
    val shortDate: String,
    val maxTempC: Double,
    val minTempC: Double,
    val precipProbabilityPercent: Int,
    val maxWindSpeedKmh: Double,
    val uvIndexMax: Double,
    val humidityAvgPercent: Int,
    val sunriseTime: String,
    val sunsetTime: String,
    val conditionCode: Int,
    val conditionText: String,
    val sceneType: WeatherSceneType
)

data class WeatherBundle(
    val location: SavedLocation,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val fetchedAtMillis: Long,
    val isFromCache: Boolean
)

/**
 * Pure conversion & meteorological formatting utilities (tested by unit tests).
 */
object WeatherFormatter {

    fun convertTemp(tempC: Double, unit: TempUnit): Int {
        return when (unit) {
            TempUnit.CELSIUS -> tempC.roundToInt()
            TempUnit.FAHRENHEIT -> ((tempC * 9.0 / 5.0) + 32.0).roundToInt()
        }
    }

    fun formatTemp(tempC: Double, unit: TempUnit, includeUnit: Boolean = true): String {
        val value = convertTemp(tempC, unit)
        return if (includeUnit) "$value${unit.symbol}" else "$value°"
    }

    fun convertWind(speedKmh: Double, unit: WindUnit): Double {
        return when (unit) {
            WindUnit.KMH -> speedKmh
            WindUnit.MPH -> speedKmh * 0.621371
            WindUnit.MS -> speedKmh / 3.6
        }
    }

    fun formatWind(speedKmh: Double, unit: WindUnit): String {
        val converted = convertWind(speedKmh, unit)
        return "${converted.roundToInt()} ${unit.symbol}"
    }

    fun windDirectionCardinal(degrees: Int): String {
        val normalized = ((degrees % 360) + 360) % 360
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((normalized + 22.5) / 45.0).toInt() % 8
        return directions[index]
    }

    fun uvCategoryLabel(uv: Double): String {
        return when {
            uv < 3.0 -> "Low"
            uv < 6.0 -> "Moderate"
            uv < 8.0 -> "High"
            uv < 11.0 -> "Very High"
            else -> "Extreme"
        }
    }

    fun humidityComfortLabel(humidity: Int): String {
        return when {
            humidity < 30 -> "Dry Air"
            humidity <= 60 -> "Optimal Comfort"
            humidity <= 75 -> "Humid"
            else -> "Very Muggy"
        }
    }

    fun pressureTrendLabel(pressureHpa: Double): String {
        return when {
            pressureHpa < 1005.0 -> "Low Pressure System"
            pressureHpa <= 1020.0 -> "Standard Atmospheric"
            else -> "High Pressure Ridge"
        }
    }

    fun formatRelativeUpdateTime(fetchedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffSeconds = ((nowMillis - fetchedAtMillis) / 1000L).coerceAtLeast(0L)
        return when {
            diffSeconds < 45 -> "Updated just now"
            diffSeconds < 3600 -> "Updated ${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "Updated ${diffSeconds / 3600}h ago"
            else -> "Updated ${diffSeconds / 86400}d ago"
        }
    }
}

object DefaultGlobalCities {
    val hubs = listOf(
        SavedLocation(
            id = "dhaka_bd",
            cityName = "Dhaka",
            region = "Dhaka Division",
            country = "Bangladesh",
            latitude = 23.8103,
            longitude = 90.4125,
            timezone = "Asia/Dhaka",
            cachedTempC = 28.0,
            cachedCondition = "Partly Cloudy",
            cachedSceneType = WeatherSceneType.CLOUDY
        ),
        SavedLocation(
            id = "tokyo_jp",
            cityName = "Tokyo",
            region = "Kanto",
            country = "Japan",
            latitude = 35.6762,
            longitude = 139.6503,
            timezone = "Asia/Tokyo",
            cachedTempC = 19.0,
            cachedCondition = "Clear Night",
            cachedSceneType = WeatherSceneType.NIGHT
        ),
        SavedLocation(
            id = "london_uk",
            cityName = "London",
            region = "England",
            country = "United Kingdom",
            latitude = 51.5074,
            longitude = -0.1278,
            timezone = "Europe/London",
            cachedTempC = 14.0,
            cachedCondition = "Rain Showers",
            cachedSceneType = WeatherSceneType.RAINY
        ),
        SavedLocation(
            id = "new_york_us",
            cityName = "New York",
            region = "New York",
            country = "United States",
            latitude = 40.7128,
            longitude = -74.0060,
            timezone = "America/New_York",
            cachedTempC = 22.0,
            cachedCondition = "Sunny",
            cachedSceneType = WeatherSceneType.SUNNY
        ),
        SavedLocation(
            id = "reykjavik_is",
            cityName = "Reykjavik",
            region = "Capital Region",
            country = "Iceland",
            latitude = 64.1466,
            longitude = -21.9426,
            timezone = "Atlantic/Reykjavik",
            cachedTempC = -1.0,
            cachedCondition = "Snowfall",
            cachedSceneType = WeatherSceneType.SNOWY
        ),
        SavedLocation(
            id = "singapore_sg",
            cityName = "Singapore",
            region = "Central",
            country = "Singapore",
            latitude = 1.3521,
            longitude = 103.8198,
            timezone = "Asia/Singapore",
            cachedTempC = 30.0,
            cachedCondition = "Thunderstorm",
            cachedSceneType = WeatherSceneType.THUNDERSTORM
        ),
        SavedLocation(
            id = "cairo_eg",
            cityName = "Cairo",
            region = "Cairo Governorate",
            country = "Egypt",
            latitude = 30.0444,
            longitude = 31.2357,
            timezone = "Africa/Cairo",
            cachedTempC = 29.0,
            cachedCondition = "Golden Sunset",
            cachedSceneType = WeatherSceneType.SUNSET
        ),
        SavedLocation(
            id = "san_francisco_us",
            cityName = "San Francisco",
            region = "California",
            country = "United States",
            latitude = 37.7749,
            longitude = -122.4194,
            timezone = "America/Los_Angeles",
            cachedTempC = 16.0,
            cachedCondition = "Dense Fog",
            cachedSceneType = WeatherSceneType.FOGGY
        ),
        SavedLocation(
            id = "sydney_au",
            cityName = "Sydney",
            region = "New South Wales",
            country = "Australia",
            latitude = -33.8688,
            longitude = 151.2093,
            timezone = "Australia/Sydney",
            cachedTempC = 24.0,
            cachedCondition = "Sunny",
            cachedSceneType = WeatherSceneType.SUNNY
        ),
        SavedLocation(
            id = "rio_br",
            cityName = "Rio de Janeiro",
            region = "Rio de Janeiro",
            country = "Brazil",
            latitude = -22.9068,
            longitude = -43.1729,
            timezone = "America/Sao_Paulo",
            cachedTempC = 27.0,
            cachedCondition = "Partly Cloudy",
            cachedSceneType = WeatherSceneType.CLOUDY
        )
    )
}
