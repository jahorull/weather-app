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
         * Maps WMO weather interpretation codes and local time-of-day into the 8 3D weather scenes.
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
    FALLBACK_2D("Low-End 2D Mode", 0.25f)
}

data class UserSettings(
    val tempUnit: TempUnit = TempUnit.CELSIUS,
    val windUnit: WindUnit = WindUnit.KMH,
    val themeMode: ThemeMode = ThemeMode.DYNAMIC,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH_3D,
    val reducedMotion: Boolean = false,
    val autoRefreshMinutes: Int = 30,
    val notificationsEnabled: Boolean = true,
    val activeLocationId: String = "dhaka_bd",
    // Personalized Weather Alert Thresholds
    val personalizedAlertsEnabled: Boolean = true,
    val rainAlertThresholdPercent: Int = 60,
    val tempAlertThresholdC: Int = 35,
    val windAlertThresholdKmh: Int = 40,
    val aqiAlertThresholdUs: Int = 100,
    val monitorAllFavoriteCities: Boolean = true
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

/**
 * Live Air Quality telemetry from Open-Meteo Air Quality API.
 * Nullable fields explicitly distinguish unavailable station readings from 0.
 */
data class AirQualityData(
    val usAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val ozone: Double?,
    val nitrogenDioxide: Double?
)

/**
 * Real Weather Alert from National Weather Service (NWS) or live WMO meteorological thresholds.
 */
data class WeatherAlert(
    val id: String,
    val headline: String,
    val severity: String,
    val affectedArea: String,
    val issuedAt: String,
    val expiresAt: String = "Until conditions improve",
    val source: String,
    val description: String
)

data class CurrentWeather(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val highC: Double,
    val lowC: Double,
    val conditionCode: Int,
    val conditionText: String,
    val sceneType: WeatherSceneType,
    val humidityPercent: Int?,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int?,
    val precipProbabilityPercent: Int?,
    val uvIndex: Double?,
    val visibilityKm: Double?,
    val pressureHpa: Double?,
    val sunriseTime: String,
    val sunsetTime: String,
    val isDay: Boolean,
    val localHourOfDay: Int = 12,
    val cityLocalTimeLabel: String = "",
    val dewPointC: Double? = humidityPercent?.let { temperatureC - ((100 - it) / 5.0) }
)

data class HourlyForecast(
    val timeIso: String,
    val displayHour: String,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val precipProbabilityPercent: Int?,
    val precipitationMm: Double = 0.0,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int? = null,
    val humidityPercent: Int?,
    val uvIndex: Double?,
    val conditionCode: Int,
    val conditionText: String,
    val sceneType: WeatherSceneType,
    val isHistoricalObservation: Boolean = false
)

data class DailyForecast(
    val dateIso: String,
    val dayOfWeek: String,
    val shortDate: String,
    val maxTempC: Double,
    val minTempC: Double,
    val precipProbabilityPercent: Int?,
    val maxWindSpeedKmh: Double,
    val uvIndexMax: Double?,
    val humidityAvgPercent: Int?,
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
    val airQuality: AirQualityData? = null,
    val alerts: List<WeatherAlert> = emptyList(),
    val radarTimeline: RadarTimelineBundle? = null,
    val confidenceReport: ForecastConfidenceReport? = null,
    val fetchedAtMillis: Long,
    val isFromCache: Boolean
)

/**
 * Feature 1: AI Rain Radar Frame & Timeline Bundle.
 * Strictly separates historical radar/precipitation observations (`isHistoricalObservation = true`)
 * from numerical/nowcast forecast layers (`isHistoricalObservation = false`).
 */
data class RadarFrame(
    val index: Int,
    val timeIso: String,
    val displayTimeLabel: String,
    val relativeOffsetLabel: String,
    val isHistoricalObservation: Boolean,
    val precipitationMm: Double?,
    val precipProbabilityPercent: Int?,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val conditionCode: Int,
    val conditionLabel: String,
    val rainViewerTilePath: String? = null,
    val dataSourceLabel: String
)

data class RadarTimelineBundle(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val frames: List<RadarFrame>,
    val rainViewerHost: String? = null,
    val rainViewerGeneratedAtUnix: Long? = null,
    val hasRainViewerComposite: Boolean = false,
    val coverageNote: String,
    val fetchedAtMillis: Long
)

/**
 * Feature 2: Multi-Model Forecast Confidence Analyzer.
 * Compares ECMWF IFS, NOAA GFS, and DWD ICON models without inventing confidence percentages.
 */
enum class AgreementLevel(val labelEn: String, val labelBn: String, val colorHex: Long) {
    HIGH("High Model Agreement (Low Spread)", "মডেলগুলোর মধ্যে উচ্চ মিল (কম পার্থক্য)", 0xFF34D399),
    MODERATE("Moderate Model Agreement", "মডেলগুলোর মধ্যে মাঝারি মিল", 0xFFFBBF24),
    LOW("Significant Model Disagreement (High Uncertainty)", "মডেলগুলোর মধ্যে উল্লেখযোগ্য অমিল (অনিশ্চয়তা বেশি)", 0xFFEF4444)
}

data class ModelDailyEntry(
    val modelName: String,
    val maxTempC: Double?,
    val minTempC: Double?,
    val precipProbPercent: Int?,
    val maxWindKmh: Double?
)

data class DayModelComparison(
    val dateIso: String,
    val dayLabel: String,
    val models: List<ModelDailyEntry>,
    val maxTempSpreadC: Double?,
    val precipSpreadPercent: Int?,
    val windSpreadKmh: Double?,
    val agreementLevel: AgreementLevel,
    val explanationEn: String,
    val explanationBn: String
)

data class ForecastConfidenceReport(
    val locationName: String,
    val observedSummaryEn: String,
    val observedSummaryBn: String,
    val dailyComparisons: List<DayModelComparison>,
    val overallAgreement: AgreementLevel,
    val overallSummaryEn: String,
    val overallSummaryBn: String,
    val dataSourcesLabel: String = "Open-Meteo Multi-Model API (ECMWF IFS 0.25° · NOAA GFS Seamless · DWD ICON Seamless)",
    val fetchedAtMillis: Long
)

/**
 * Feature 3: Bangladesh Smart Farming Mode (Krishi Weather Intelligence).
 */
enum class BangladeshCrop(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val idealTempRangeC: ClosedFloatingPointRange<Double>,
    val dryingHumidityCeilingPercent: Int,
    val sensitivityNoteEn: String,
    val sensitivityNoteBn: String
) {
    AMAN_BORO_RICE(
        id = "rice",
        nameEn = "Aman / Boro Rice",
        nameBn = "ধান (আমন / বোরো)",
        idealTempRangeC = 22.0..34.0,
        dryingHumidityCeilingPercent = 72,
        sensitivityNoteEn = "Sensitive to standing water during harvest and high wind (>35 km/h) lodging during grain filling.",
        sensitivityNoteBn = "পাকা ধান কাটার সময় অতিবৃষ্টি ও ঘণ্টায় ৩৫ কি.মি.-এর বেশি বাতাসে ধানের গাছ হেলে পড়ার ঝুঁকি থাকে।"
    ),
    JUTE(
        id = "jute",
        nameEn = "Jute",
        nameBn = "পাট",
        idealTempRangeC = 24.0..36.0,
        dryingHumidityCeilingPercent = 75,
        sensitivityNoteEn = "Requires warm humid growth conditions, clear sunny windows for fiber drying after retting, and low wind during tall stalk stage.",
        sensitivityNoteBn = "পাট জাগ দেওয়া ও আঁশ শুকানোর জন্য রোদেলা আবহাওয়া এবং কম বৃষ্টির সময়কাল প্রয়োজন।"
    ),
    POTATO(
        id = "potato",
        nameEn = "Potato",
        nameBn = "আলু",
        idealTempRangeC = 15.0..26.0,
        dryingHumidityCeilingPercent = 70,
        sensitivityNoteEn = "Cool dry weather favors tuber growth; persistent fog, high humidity (>82%), and unseasonal rain increase late blight risk.",
        sensitivityNoteBn = "ঘন কুয়াশা, অতিরিক্ত আর্দ্রতা (>৮২%) ও অসময়ের বৃষ্টিতে আলুর মড়ক বা লেট ব্লাইট রোগের ঝুঁকি বাড়ে।"
    ),
    MANGO_ORCHARD(
        id = "mango",
        nameEn = "Mango & Orchard",
        nameBn = "আম ও ফল বাগান",
        idealTempRangeC = 24.0..36.0,
        dryingHumidityCeilingPercent = 70,
        sensitivityNoteEn = "Rain or dense fog during flowering/fruiting and Kalbaishakhi squalls (>40 km/h) can cause fruit drop.",
        sensitivityNoteBn = "আমের মুকুল বা গুটি অবস্থায় বৃষ্টি/কুয়াশা এবং কালবৈশাখী ঝড়ো বাতাসে (>৪০ কি.মি./ঘণ্টা) ফল ঝরে পড়ার আশঙ্কা থাকে।"
    ),
    WHEAT_MAIZE(
        id = "wheat_maize",
        nameEn = "Wheat & Maize",
        nameBn = "গম ও ভুট্টা",
        idealTempRangeC = 18.0..32.0,
        dryingHumidityCeilingPercent = 68,
        sensitivityNoteEn = "Requires well-drained soil; avoid irrigation before forecasted rain to prevent root waterlogging.",
        sensitivityNoteBn = "জলাবদ্ধতা সহ্য করতে পারে না; বৃষ্টির পূর্বাভাস থাকলে সেচ দেওয়া থেকে বিরত থাকা প্রয়োজন।"
    ),
    VEGETABLES_MUSTARD(
        id = "vegetables_mustard",
        nameEn = "Vegetables & Mustard",
        nameBn = "শাকসবজি ও সরিষা",
        idealTempRangeC = 16.0..30.0,
        dryingHumidityCeilingPercent = 72,
        sensitivityNoteEn = "Heavy rain damages delicate blooms and leafy crops; morning low-wind windows are best for field care.",
        sensitivityNoteBn = "ভারী বৃষ্টিতে সরিষার ফুল ও সবজি খেত ক্ষতিগ্রস্ত হতে পারে; সকালবেলা কম বাতাসে পরিচর্যা করা উত্তম।"
    )
}

data class SmartFarmingAdvisory(
    val location: SavedLocation,
    val crop: BangladeshCrop,
    val isDataAvailable: Boolean,
    val rainfallGuidanceEn: String,
    val rainfallGuidanceBn: String,
    val irrigationGuidanceEn: String,
    val irrigationGuidanceBn: String,
    val dryingGuidanceEn: String,
    val dryingGuidanceBn: String,
    val outdoorActivityWindowsEn: String,
    val outdoorActivityWindowsBn: String,
    val uncertaintyExplanationEn: String,
    val uncertaintyExplanationBn: String,
    val expertGuidanceNoticeEn: String,
    val expertGuidanceNoticeBn: String,
    val updateTimestampLabel: String
)

/**
 * Feature 4: Personalized User Threshold Alert (Distinct from Official NWS/WMO Alerts).
 */
data class PersonalThresholdAlert(
    val id: String,
    val locationName: String,
    val metricCategory: String,
    val triggeredValueLabel: String,
    val thresholdValueLabel: String,
    val messageEn: String,
    val messageBn: String,
    val evaluatedAtLabel: String,
    val sourceBadge: String = "Personalized User Threshold · Not an Official Government Warning"
)

/**
 * Pure conversion & meteorological formatting utilities (tested by unit tests).
 * Strictly distinguishes null ("Unavailable") from 0 ("0").
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

    fun formatNullablePercent(value: Int?): String {
        return if (value == null) "Unavailable" else "$value%"
    }

    fun formatNullableUv(uv: Double?): String {
        return if (uv == null) "Unavailable" else "${String.format("%.1f", uv)} · ${uvCategoryLabel(uv)}"
    }

    fun formatNullableVisibility(visibilityKm: Double?): String {
        return if (visibilityKm == null) "Unavailable" else "${String.format("%.1f", visibilityKm)} km"
    }

    fun formatNullablePressure(pressureHpa: Double?): String {
        return if (pressureHpa == null) "Unavailable" else "${pressureHpa.roundToInt()} hPa"
    }

    fun formatNullablePollutant(value: Double?, unit: String = "µg/m³"): String {
        return if (value == null) "Unavailable" else "${String.format("%.1f", value)} $unit"
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

    fun aqiCategoryLabel(usAqi: Int?): String {
        if (usAqi == null) return "Unavailable"
        return when {
            usAqi <= 50 -> "Good"
            usAqi <= 100 -> "Moderate"
            usAqi <= 150 -> "Sensitive Groups"
            usAqi <= 200 -> "Unhealthy"
            usAqi <= 300 -> "Very Unhealthy"
            else -> "Hazardous"
        }
    }

    fun aqiColorHex(usAqi: Int?): Long {
        if (usAqi == null) return 0xFF94A3B8
        return when {
            usAqi <= 50 -> 0xFF34D399
            usAqi <= 100 -> 0xFFFBBF24
            usAqi <= 150 -> 0xFFFB923C
            usAqi <= 200 -> 0xFFF87171
            usAqi <= 300 -> 0xFFC084FC
            else -> 0xFFEF4444
        }
    }

    fun humidityComfortLabel(humidity: Int?): String {
        if (humidity == null) return "Sensor Unavailable"
        return when {
            humidity < 30 -> "Dry Air"
            humidity <= 60 -> "Optimal Comfort"
            humidity <= 75 -> "Humid"
            else -> "Very Muggy"
        }
    }

    fun pressureTrendLabel(pressureHpa: Double?): String {
        if (pressureHpa == null) return "Barometer Unavailable"
        return when {
            pressureHpa < 1005.0 -> "Low Pressure System"
            pressureHpa <= 1020.0 -> "Standard Atmospheric"
            else -> "High Pressure Ridge"
        }
    }

    fun formatRelativeUpdateTime(fetchedAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        if (fetchedAtMillis <= 0L) return "Awaiting live sync"
        val diffSeconds = ((nowMillis - fetchedAtMillis) / 1000L).coerceAtLeast(0L)
        return when {
            diffSeconds < 45 -> "Updated just now"
            diffSeconds < 3600 -> "Updated ${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "Updated ${diffSeconds / 3600}h ago"
            else -> "Updated ${diffSeconds / 86400}d ago"
        }
    }
}

/**
 * Geographic directory of global cities with real coordinates and timezones.
 * Note: cachedTempC and cachedCondition default to null until populated by live API responses.
 */
object DefaultGlobalCities {
    val hubs = listOf(
        SavedLocation(
            id = "dhaka_bd",
            cityName = "Dhaka",
            region = "Dhaka Division",
            country = "Bangladesh",
            latitude = 23.8103,
            longitude = 90.4125,
            timezone = "Asia/Dhaka"
        ),
        SavedLocation(
            id = "tokyo_jp",
            cityName = "Tokyo",
            region = "Kanto",
            country = "Japan",
            latitude = 35.6762,
            longitude = 139.6503,
            timezone = "Asia/Tokyo"
        ),
        SavedLocation(
            id = "london_uk",
            cityName = "London",
            region = "England",
            country = "United Kingdom",
            latitude = 51.5074,
            longitude = -0.1278,
            timezone = "Europe/London"
        ),
        SavedLocation(
            id = "new_york_us",
            cityName = "New York",
            region = "New York",
            country = "United States",
            latitude = 40.7128,
            longitude = -74.0060,
            timezone = "America/New_York"
        ),
        SavedLocation(
            id = "reykjavik_is",
            cityName = "Reykjavik",
            region = "Capital Region",
            country = "Iceland",
            latitude = 64.1466,
            longitude = -21.9426,
            timezone = "Atlantic/Reykjavik"
        ),
        SavedLocation(
            id = "singapore_sg",
            cityName = "Singapore",
            region = "Central",
            country = "Singapore",
            latitude = 1.3521,
            longitude = 103.8198,
            timezone = "Asia/Singapore"
        ),
        SavedLocation(
            id = "cairo_eg",
            cityName = "Cairo",
            region = "Cairo Governorate",
            country = "Egypt",
            latitude = 30.0444,
            longitude = 31.2357,
            timezone = "Africa/Cairo"
        ),
        SavedLocation(
            id = "san_francisco_us",
            cityName = "San Francisco",
            region = "California",
            country = "United States",
            latitude = 37.7749,
            longitude = -122.4194,
            timezone = "America/Los_Angeles"
        ),
        SavedLocation(
            id = "sydney_au",
            cityName = "Sydney",
            region = "New South Wales",
            country = "Australia",
            latitude = -33.8688,
            longitude = 151.2093,
            timezone = "Australia/Sydney"
        ),
        SavedLocation(
            id = "rio_br",
            cityName = "Rio de Janeiro",
            region = "Rio de Janeiro",
            country = "Brazil",
            latitude = -22.9068,
            longitude = -43.1729,
            timezone = "America/Sao_Paulo"
        )
    )

    /**
     * Major agricultural divisions of Bangladesh for Smart Farming Mode.
     */
    val bangladeshFarmingHubs = listOf(
        hubs.first(), // Dhaka
        SavedLocation(
            id = "rajshahi_bd",
            cityName = "Rajshahi",
            region = "Rajshahi Division",
            country = "Bangladesh",
            latitude = 24.3745,
            longitude = 88.6042,
            timezone = "Asia/Dhaka"
        ),
        SavedLocation(
            id = "sylhet_bd",
            cityName = "Sylhet",
            region = "Sylhet Division",
            country = "Bangladesh",
            latitude = 24.8949,
            longitude = 91.8687,
            timezone = "Asia/Dhaka"
        ),
        SavedLocation(
            id = "khulna_bd",
            cityName = "Khulna",
            region = "Khulna Division",
            country = "Bangladesh",
            latitude = 22.8456,
            longitude = 89.5403,
            timezone = "Asia/Dhaka"
        ),
        SavedLocation(
            id = "chattogram_bd",
            cityName = "Chattogram",
            region = "Chattogram Division",
            country = "Bangladesh",
            latitude = 22.3569,
            longitude = 91.7832,
            timezone = "Asia/Dhaka"
        ),
        SavedLocation(
            id = "rangpur_bd",
            cityName = "Rangpur",
            region = "Rangpur Division",
            country = "Bangladesh",
            latitude = 25.7439,
            longitude = 89.2752,
            timezone = "Asia/Dhaka"
        ),
        SavedLocation(
            id = "barishal_bd",
            cityName = "Barishal",
            region = "Barishal Division",
            country = "Bangladesh",
            latitude = 22.7010,
            longitude = 90.3535,
            timezone = "Asia/Dhaka"
        ),
        SavedLocation(
            id = "mymensingh_bd",
            cityName = "Mymensingh",
            region = "Mymensingh Division",
            country = "Bangladesh",
            latitude = 24.7471,
            longitude = 90.4203,
            timezone = "Asia/Dhaka"
        )
    )
}
