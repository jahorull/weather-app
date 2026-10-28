package com.example.data.remote

import com.example.BuildConfig
import com.example.model.AgreementLevel
import com.example.model.AirQualityData
import com.example.model.CurrentWeather
import com.example.model.DailyForecast
import com.example.model.DayModelComparison
import com.example.model.ForecastConfidenceReport
import com.example.model.HourlyForecast
import com.example.model.ModelDailyEntry
import com.example.model.RadarFrame
import com.example.model.RadarTimelineBundle
import com.example.model.SavedLocation
import com.example.model.WeatherAlert
import com.example.model.WeatherBundle
import com.example.model.WeatherSceneType
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,visibility,uv_index",
        @Query("hourly") hourly: String = "temperature_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_direction_10m,relative_humidity_2m,uv_index,is_day",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max,wind_speed_10m_max",
        @Query("timezone") timezone: String = "auto",
        @Query("past_hours") pastHours: Int = 6,
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoForecastResponse

    @GET("v1/forecast")
    suspend fun getSingleModelDailyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max",
        @Query("models") model: String,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 5
    ): OpenMeteoForecastResponse
}

interface RainViewerMapsApi {
    @GET("public/weather-maps.json")
    suspend fun getWeatherMaps(): RainViewerMapsResponse
}

interface OpenMeteoAirQualityApi {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,pm10,pm2_5,ozone,nitrogen_dioxide",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoAirQualityResponse
}

interface NationalWeatherServiceAlertsApi {
    @GET("alerts/active")
    suspend fun getActiveAlertsForPoint(
        @Query("point") latLonPoint: String
    ): NwsAlertsResponse
}

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchCities(
        @Query("name") query: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): GeocodingSearchResponse
}

interface ReverseGeocodingApi {
    @GET("data/reverse-geocode-client")
    suspend fun reverseGeocode(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("localityLanguage") language: String = "en"
    ): ReverseGeocodeResponse
}

data class RainViewerMapsResponse(
    @Json(name = "version") val version: String? = null,
    @Json(name = "generated") val generated: Long? = null,
    @Json(name = "host") val host: String? = null,
    @Json(name = "radar") val radar: RainViewerRadarDto? = null
)

data class RainViewerRadarDto(
    @Json(name = "past") val past: List<RainViewerFrameDto>? = null,
    @Json(name = "nowcast") val nowcast: List<RainViewerFrameDto>? = null
)

data class RainViewerFrameDto(
    @Json(name = "time") val time: Long = 0L,
    @Json(name = "path") val path: String? = null
)

data class OpenMeteoForecastResponse(
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "timezone") val timezone: String = "UTC",
    @Json(name = "current") val current: CurrentDto? = null,
    @Json(name = "hourly") val hourly: HourlyDto? = null,
    @Json(name = "daily") val daily: DailyDto? = null
)

data class CurrentDto(
    @Json(name = "time") val time: String = "",
    @Json(name = "temperature_2m") val temperature2m: Double = 0.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 0.0,
    @Json(name = "wind_direction_10m") val windDirection10m: Int? = null,
    @Json(name = "visibility") val visibilityMeters: Double? = null,
    @Json(name = "uv_index") val uvIndex: Double? = null
)

data class HourlyDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double> = emptyList(),
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>? = null,
    @Json(name = "precipitation") val precipitation: List<Double?>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double> = emptyList(),
    @Json(name = "wind_direction_10m") val windDirection10m: List<Int?>? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int?>? = null,
    @Json(name = "uv_index") val uvIndex: List<Double?>? = null,
    @Json(name = "is_day") val isDay: List<Int>? = null
)

data class DailyDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double?> = emptyList(),
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double?> = emptyList(),
    @Json(name = "sunrise") val sunrise: List<String> = emptyList(),
    @Json(name = "sunset") val sunset: List<String> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?>? = null,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?>? = null,
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double?> = emptyList()
)

data class OpenMeteoAirQualityResponse(
    @Json(name = "current") val current: AirQualityCurrentDto? = null
)

data class AirQualityCurrentDto(
    @Json(name = "time") val time: String? = null,
    @Json(name = "us_aqi") val usAqi: Int? = null,
    @Json(name = "pm10") val pm10: Double? = null,
    @Json(name = "pm2_5") val pm25: Double? = null,
    @Json(name = "ozone") val ozone: Double? = null,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double? = null
)

data class NwsAlertsResponse(
    @Json(name = "features") val features: List<NwsAlertFeatureDto>? = null
)

data class NwsAlertFeatureDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "properties") val properties: NwsAlertPropertiesDto? = null
)

data class NwsAlertPropertiesDto(
    @Json(name = "event") val event: String? = null,
    @Json(name = "headline") val headline: String? = null,
    @Json(name = "severity") val severity: String? = null,
    @Json(name = "areaDesc") val areaDesc: String? = null,
    @Json(name = "sent") val sent: String? = null,
    @Json(name = "expires") val expires: String? = null,
    @Json(name = "senderName") val senderName: String? = null,
    @Json(name = "description") val description: String? = null
)

data class GeocodingSearchResponse(
    @Json(name = "results") val results: List<GeocodingResultDto>? = null
)

data class GeocodingResultDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "name") val name: String = "",
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "country") val country: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "timezone") val timezone: String? = null
)

data class ReverseGeocodeResponse(
    @Json(name = "city") val city: String? = null,
    @Json(name = "locality") val locality: String? = null,
    @Json(name = "principalSubdivision") val principalSubdivision: String? = null,
    @Json(name = "countryName") val countryName: String? = null
)

object NetworkModule {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val configuredKey = BuildConfig.WEATHER_API_KEY
                val reqBuilder = chain.request().newBuilder()
                    .header("User-Agent", "Atmos3D-Android/2.0 (weather-telemetry)")
                    .header("Accept", "application/geo+json, application/json")
                if (configuredKey.isNotBlank() && configuredKey != "DEFAULT_OPEN_METEO_LIVE") {
                    reqBuilder.header("X-Weather-Key-Configured", "true")
                }
                chain.proceed(reqBuilder.build())
            }
            .addInterceptor(logging)
            .build()
    }

    val forecastApi: OpenMeteoForecastApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoForecastApi::class.java)
    }

    val rainViewerApi: RainViewerMapsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.rainviewer.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RainViewerMapsApi::class.java)
    }

    val airQualityApi: OpenMeteoAirQualityApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://air-quality-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoAirQualityApi::class.java)
    }

    val nwsAlertsApi: NationalWeatherServiceAlertsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.weather.gov/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NationalWeatherServiceAlertsApi::class.java)
    }

    val geocodingApi: OpenMeteoGeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingApi::class.java)
    }

    val reverseGeocodingApi: ReverseGeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.bigdatacloud.net/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ReverseGeocodingApi::class.java)
    }

    fun moshiInstance(): Moshi = moshi
}

object WeatherApiMapper {

    fun mapToWeatherBundle(
        location: SavedLocation,
        response: OpenMeteoForecastResponse,
        airQualityResponse: OpenMeteoAirQualityResponse? = null,
        nwsAlertsResponse: NwsAlertsResponse? = null,
        rainViewerResponse: RainViewerMapsResponse? = null,
        multiModelResponses: Map<String, OpenMeteoForecastResponse> = emptyMap(),
        fetchedAtMillis: Long = System.currentTimeMillis(),
        isFromCache: Boolean = false
    ): WeatherBundle {
        val currentDto = response.current ?: CurrentDto()
        val dailyDto = response.daily ?: DailyDto()
        val hourlyDto = response.hourly ?: HourlyDto()

        val currentHour = extractHourOfDay(currentDto.time)
        val cityLocalTimeLabel = formatIsoClock(currentDto.time)
        val isDay = currentDto.isDay == 1
        val currentScene = WeatherSceneType.fromWmoCode(
            wmoCode = currentDto.weatherCode,
            isDay = isDay,
            hourOfDay = currentHour
        )
        val currentConditionText = WeatherSceneType.conditionLabelFromWmo(
            wmoCode = currentDto.weatherCode,
            isDay = isDay,
            hourOfDay = currentHour
        )

        val todayHigh = dailyDto.temperature2mMax.firstOrNull { it != null } ?: currentDto.temperature2m
        val todayLow = dailyDto.temperature2mMin.firstOrNull { it != null } ?: currentDto.temperature2m
        val todaySunrise = dailyDto.sunrise.firstOrNull()?.let { formatIsoClock(it) } ?: "Unavailable"
        val todaySunset = dailyDto.sunset.firstOrNull()?.let { formatIsoClock(it) } ?: "Unavailable"

        // Preserve null if UV or visibility is genuinely missing from the API response
        val currentUv: Double? = currentDto.uvIndex ?: dailyDto.uvIndexMax?.firstOrNull()
        val visibilityKm: Double? = currentDto.visibilityMeters?.let { (it / 1000.0).coerceIn(0.0, 50.0) }

        val currentHourPrefix = currentDto.time.take(13)
        val startIndex = if (currentHourPrefix.isNotBlank()) {
            hourlyDto.time.indexOfFirst { it >= currentHourPrefix }.let { if (it == -1) 0 else it }
        } else {
            0
        }

        val pastHourlyList = mutableListOf<HourlyForecast>()
        for (idx in 0 until startIndex) {
            val timeStr = hourlyDto.time.getOrElse(idx) { "" }
            val hr = extractHourOfDay(timeStr)
            val hrIsDay = (hourlyDto.isDay?.getOrNull(idx) ?: if (hr in 6..18) 1 else 0) == 1
            val code = hourlyDto.weatherCode.getOrElse(idx) { currentDto.weatherCode }
            val scene = WeatherSceneType.fromWmoCode(code, hrIsDay, hr)
            val hoursAgo = startIndex - idx
            val label = "-${hoursAgo}h (${formatHourLabel(hr)})"
            pastHourlyList.add(
                HourlyForecast(
                    timeIso = timeStr,
                    displayHour = label,
                    temperatureC = hourlyDto.temperature2m.getOrElse(idx) { currentDto.temperature2m },
                    feelsLikeC = hourlyDto.apparentTemperature.getOrElse(idx) { currentDto.apparentTemperature },
                    precipProbabilityPercent = hourlyDto.precipitationProbability?.getOrNull(idx),
                    precipitationMm = hourlyDto.precipitation?.getOrNull(idx) ?: 0.0,
                    windSpeedKmh = hourlyDto.windSpeed10m.getOrElse(idx) { currentDto.windSpeed10m },
                    windDirectionDeg = hourlyDto.windDirection10m?.getOrNull(idx) ?: currentDto.windDirection10m,
                    humidityPercent = hourlyDto.relativeHumidity2m?.getOrNull(idx) ?: currentDto.relativeHumidity2m,
                    uvIndex = hourlyDto.uvIndex?.getOrNull(idx),
                    conditionCode = code,
                    conditionText = WeatherSceneType.conditionLabelFromWmo(code, hrIsDay, hr),
                    sceneType = scene,
                    isHistoricalObservation = true
                )
            )
        }

        val hourlyList = mutableListOf<HourlyForecast>()
        val maxHours = minOf(24, hourlyDto.time.size - startIndex)
        for (i in 0 until maxHours) {
            val idx = startIndex + i
            val timeStr = hourlyDto.time.getOrElse(idx) { "" }
            val hr = extractHourOfDay(timeStr)
            val hrIsDay = (hourlyDto.isDay?.getOrNull(idx) ?: if (hr in 6..18) 1 else 0) == 1
            val code = hourlyDto.weatherCode.getOrElse(idx) { currentDto.weatherCode }
            val scene = WeatherSceneType.fromWmoCode(code, hrIsDay, hr)
            val label = if (i == 0) "Now" else formatHourLabel(hr)
            hourlyList.add(
                HourlyForecast(
                    timeIso = timeStr,
                    displayHour = label,
                    temperatureC = hourlyDto.temperature2m.getOrElse(idx) { currentDto.temperature2m },
                    feelsLikeC = hourlyDto.apparentTemperature.getOrElse(idx) { currentDto.apparentTemperature },
                    precipProbabilityPercent = hourlyDto.precipitationProbability?.getOrNull(idx),
                    precipitationMm = hourlyDto.precipitation?.getOrNull(idx) ?: 0.0,
                    windSpeedKmh = hourlyDto.windSpeed10m.getOrElse(idx) { currentDto.windSpeed10m },
                    windDirectionDeg = hourlyDto.windDirection10m?.getOrNull(idx) ?: currentDto.windDirection10m,
                    humidityPercent = hourlyDto.relativeHumidity2m?.getOrNull(idx) ?: currentDto.relativeHumidity2m,
                    uvIndex = hourlyDto.uvIndex?.getOrNull(idx),
                    conditionCode = code,
                    conditionText = WeatherSceneType.conditionLabelFromWmo(code, hrIsDay, hr),
                    sceneType = scene,
                    isHistoricalObservation = false
                )
            )
        }

        val currentPrecipProb: Int? = hourlyList.firstOrNull()?.precipProbabilityPercent
            ?: dailyDto.precipitationProbabilityMax?.firstOrNull()

        val dailyList = mutableListOf<DailyForecast>()
        val dayCount = minOf(7, dailyDto.time.size)
        for (d in 0 until dayCount) {
            val dateStr = dailyDto.time[d]
            val code = dailyDto.weatherCode.getOrElse(d) { 1 }
            val scene = WeatherSceneType.fromWmoCode(code, isDay = true, hourOfDay = 13)
            val dayName = if (d == 0) "Today" else formatDayOfWeek(dateStr)
            val shortDate = formatShortDate(dateStr)
            dailyList.add(
                DailyForecast(
                    dateIso = dateStr,
                    dayOfWeek = dayName,
                    shortDate = shortDate,
                    maxTempC = dailyDto.temperature2mMax.getOrNull(d) ?: todayHigh,
                    minTempC = dailyDto.temperature2mMin.getOrNull(d) ?: todayLow,
                    precipProbabilityPercent = dailyDto.precipitationProbabilityMax?.getOrNull(d),
                    maxWindSpeedKmh = dailyDto.windSpeed10mMax.getOrNull(d) ?: currentDto.windSpeed10m,
                    uvIndexMax = dailyDto.uvIndexMax?.getOrNull(d),
                    humidityAvgPercent = currentDto.relativeHumidity2m,
                    sunriseTime = dailyDto.sunrise.getOrNull(d)?.let { formatIsoClock(it) } ?: "Unavailable",
                    sunsetTime = dailyDto.sunset.getOrNull(d)?.let { formatIsoClock(it) } ?: "Unavailable",
                    conditionCode = code,
                    conditionText = WeatherSceneType.conditionLabelFromWmo(code, isDay = true, hourOfDay = 13),
                    sceneType = scene
                )
            )
        }

        val airQualityData = airQualityResponse?.current?.let { aq ->
            if (aq.usAqi == null && aq.pm25 == null && aq.pm10 == null && aq.ozone == null && aq.nitrogenDioxide == null) {
                null
            } else {
                AirQualityData(
                    usAqi = aq.usAqi,
                    pm25 = aq.pm25,
                    pm10 = aq.pm10,
                    ozone = aq.ozone,
                    nitrogenDioxide = aq.nitrogenDioxide
                )
            }
        }

        val alertsList = buildAuthenticWeatherAlerts(
            location = location,
            currentDto = currentDto,
            visibilityKm = visibilityKm,
            currentUv = currentUv,
            airQuality = airQualityData,
            nwsAlertsResponse = nwsAlertsResponse
        )

        val radarTimeline = buildRadarTimeline(
            location = location,
            currentDto = currentDto,
            hourlyDto = hourlyDto,
            startIndex = startIndex,
            rainViewerResponse = rainViewerResponse,
            fetchedAtMillis = fetchedAtMillis
        )

        val confidenceReport = buildForecastConfidenceReport(
            location = location,
            currentDto = currentDto,
            primaryDaily = dailyList,
            multiModelResponses = multiModelResponses,
            fetchedAtMillis = fetchedAtMillis
        )

        val updatedLocation = location.copy(
            timezone = response.timezone.ifBlank { location.timezone },
            cachedTempC = currentDto.temperature2m,
            cachedCondition = currentConditionText,
            cachedSceneType = currentScene
        )

        return WeatherBundle(
            location = updatedLocation,
            current = CurrentWeather(
                temperatureC = currentDto.temperature2m,
                feelsLikeC = currentDto.apparentTemperature,
                highC = todayHigh,
                lowC = todayLow,
                conditionCode = currentDto.weatherCode,
                conditionText = currentConditionText,
                sceneType = currentScene,
                humidityPercent = currentDto.relativeHumidity2m,
                windSpeedKmh = currentDto.windSpeed10m,
                windDirectionDeg = currentDto.windDirection10m,
                precipProbabilityPercent = currentPrecipProb,
                uvIndex = currentUv,
                visibilityKm = visibilityKm,
                pressureHpa = currentDto.surfacePressure,
                sunriseTime = todaySunrise,
                sunsetTime = todaySunset,
                isDay = isDay,
                localHourOfDay = currentHour,
                cityLocalTimeLabel = cityLocalTimeLabel
            ),
            hourly = hourlyList,
            daily = dailyList,
            pastHourly = pastHourlyList,
            airQuality = airQualityData,
            alerts = alertsList,
            radarTimeline = radarTimeline,
            confidenceReport = confidenceReport,
            fetchedAtMillis = fetchedAtMillis,
            isFromCache = isFromCache
        )
    }

    /**
     * Builds the AI Rain Radar timeline strictly from real past hourly observations (`past_hours`)
     * + RainViewer radar composite frames (when available) + upcoming forecast precipitation layers.
     * Never labels forecast layers as historical observations or fabricates radar echoes.
     */
    fun buildRadarTimeline(
        location: SavedLocation,
        currentDto: CurrentDto,
        hourlyDto: HourlyDto,
        startIndex: Int,
        rainViewerResponse: RainViewerMapsResponse?,
        fetchedAtMillis: Long
    ): RadarTimelineBundle {
        val frames = mutableListOf<RadarFrame>()
        val pastCount = startIndex.coerceIn(0, 6)
        val firstIdx = (startIndex - pastCount).coerceAtLeast(0)
        val lastIdx = minOf(hourlyDto.time.size - 1, startIndex + 12)

        val rvPastPaths = rainViewerResponse?.radar?.past.orEmpty()
        val rvNowcastPaths = rainViewerResponse?.radar?.nowcast.orEmpty()
        val hasRainViewer = rvPastPaths.isNotEmpty() || rvNowcastPaths.isNotEmpty()

        var frameCounter = 0
        for (idx in firstIdx..lastIdx) {
            if (idx < 0 || idx >= hourlyDto.time.size) continue
            val timeStr = hourlyDto.time[idx]
            val hr = extractHourOfDay(timeStr)
            val isPastOrNow = idx <= startIndex
            val offsetHours = idx - startIndex
            val relativeLabel = when {
                offsetHours < 0 -> "${offsetHours}h (Observed)"
                offsetHours == 0 -> "Now (Live Obs)"
                else -> "+${offsetHours}h (Forecast)"
            }
            val code = hourlyDto.weatherCode.getOrElse(idx) { currentDto.weatherCode }
            val precipMm = hourlyDto.precipitation?.getOrNull(idx)
            val precipProb = hourlyDto.precipitationProbability?.getOrNull(idx)
            val windSpeed = hourlyDto.windSpeed10m.getOrElse(idx) { currentDto.windSpeed10m }
            val windDir = hourlyDto.windDirection10m?.getOrNull(idx) ?: currentDto.windDirection10m ?: 180

            val rvTilePath = if (isPastOrNow && rvPastPaths.isNotEmpty()) {
                rvPastPaths.getOrNull((frameCounter).coerceAtMost(rvPastPaths.size - 1))?.path
            } else if (!isPastOrNow && rvNowcastPaths.isNotEmpty()) {
                rvNowcastPaths.getOrNull((offsetHours - 1).coerceIn(0, rvNowcastPaths.size - 1))?.path
            } else {
                null
            }

            val sourceLabel = when {
                isPastOrNow && rvTilePath != null -> "Historical Observation · RainViewer Composite + Open-Meteo Past Telemetry"
                isPastOrNow -> "Historical Observation · Open-Meteo Hourly Surface Analysis"
                rvTilePath != null -> "Forecast Layer · RainViewer Nowcast + Open-Meteo NWP"
                else -> "Forecast Layer · Open-Meteo Numerical Precipitation Model"
            }

            frames.add(
                RadarFrame(
                    index = frameCounter++,
                    timeIso = timeStr,
                    displayTimeLabel = formatHourLabel(hr),
                    relativeOffsetLabel = relativeLabel,
                    isHistoricalObservation = isPastOrNow,
                    precipitationMm = precipMm,
                    precipProbabilityPercent = precipProb,
                    windSpeedKmh = windSpeed,
                    windDirectionDeg = windDir,
                    conditionCode = code,
                    conditionLabel = WeatherSceneType.conditionLabelFromWmo(code, isDay = hr in 6..18, hourOfDay = hr),
                    rainViewerTilePath = rvTilePath,
                    dataSourceLabel = sourceLabel
                )
            )
        }

        val coverageNote = if (hasRainViewer) {
            "RainViewer global radar composite timestamps (${rvPastPaths.size} past observations, ${rvNowcastPaths.size} nowcast frames) + Open-Meteo hourly precipitation telemetry active for ${location.cityName}."
        } else {
            "Ground Doppler radar composite tiles are not reported by RainViewer for this request; displaying verified Open-Meteo past hourly observations and numerical precipitation forecast layers for ${location.cityName}."
        }

        return RadarTimelineBundle(
            locationName = location.cityName,
            latitude = location.latitude,
            longitude = location.longitude,
            frames = frames,
            rainViewerHost = rainViewerResponse?.host,
            rainViewerGeneratedAtUnix = rainViewerResponse?.generated,
            hasRainViewerComposite = hasRainViewer,
            coverageNote = coverageNote,
            fetchedAtMillis = fetchedAtMillis
        )
    }

    /**
     * Feature 2: Compares forecasts across multiple NWP models (Primary Best-Match, ECMWF IFS,
     * NOAA GFS, DWD ICON) when provided by the API, or compares primary daily telemetry with
     * model spread without ever inventing confidence percentages.
     */
    fun buildForecastConfidenceReport(
        location: SavedLocation,
        currentDto: CurrentDto,
        primaryDaily: List<DailyForecast>,
        multiModelResponses: Map<String, OpenMeteoForecastResponse>,
        fetchedAtMillis: Long
    ): ForecastConfidenceReport {
        val observedSummaryEn = "Current Observation (${currentDto.time.ifBlank { "Live" }}): " +
            "${currentDto.temperature2m.roundToInt()}°C, Wind ${currentDto.windSpeed10m.roundToInt()} km/h, " +
            "Humidity ${currentDto.relativeHumidity2m?.let { "$it%" } ?: "Unavailable"}."
        val observedSummaryBn = "বর্তমান পর্যবেক্ষণ (${currentDto.time.ifBlank { "Live" }}): " +
            "তাপমাত্রা ${currentDto.temperature2m.roundToInt()}°C, বাতাস ${currentDto.windSpeed10m.roundToInt()} km/h, " +
            "আর্দ্রতা ${currentDto.relativeHumidity2m?.let { "$it%" } ?: "পাওয়া যায়নি"}।"

        val comparisons = mutableListOf<DayModelComparison>()
        val daysToCompare = minOf(5, primaryDaily.size)

        for (d in 0 until daysToCompare) {
            val baseDay = primaryDaily[d]
            val entries = mutableListOf<ModelDailyEntry>()

            // Always include the Primary Open-Meteo Best-Match Integration
            entries.add(
                ModelDailyEntry(
                    modelName = "Open-Meteo Best Match",
                    maxTempC = baseDay.maxTempC,
                    minTempC = baseDay.minTempC,
                    precipProbPercent = baseDay.precipProbabilityPercent,
                    maxWindKmh = baseDay.maxWindSpeedKmh
                )
            )

            // Add each distinct NWP model returned by the multi-model endpoint
            multiModelResponses.forEach { (modelLabel, modelResp) ->
                val mDaily = modelResp.daily
                if (mDaily != null && d < mDaily.time.size) {
                    val mMax = mDaily.temperature2mMax.getOrNull(d)
                    val mMin = mDaily.temperature2mMin.getOrNull(d)
                    val mRain = mDaily.precipitationProbabilityMax?.getOrNull(d)
                    val mWind = mDaily.windSpeed10mMax.getOrNull(d)
                    if (mMax != null || mRain != null || mWind != null) {
                        entries.add(
                            ModelDailyEntry(
                                modelName = modelLabel,
                                maxTempC = mMax,
                                minTempC = mMin,
                                precipProbPercent = mRain,
                                maxWindKmh = mWind
                            )
                        )
                    }
                }
            }

            val validTemps = entries.mapNotNull { it.maxTempC }
            val tempSpread = if (validTemps.size >= 2) {
                (validTemps.maxOrNull()!! - validTemps.minOrNull()!!)
            } else {
                null
            }

            val validRains = entries.mapNotNull { it.precipProbPercent }
            val rainSpread = if (validRains.size >= 2) {
                (validRains.maxOrNull()!! - validRains.minOrNull()!!)
            } else {
                null
            }

            val validWinds = entries.mapNotNull { it.maxWindKmh }
            val windSpread = if (validWinds.size >= 2) {
                (validWinds.maxOrNull()!! - validWinds.minOrNull()!!)
            } else {
                null
            }

            val agreement = when {
                tempSpread == null && rainSpread == null -> AgreementLevel.MODERATE
                (tempSpread ?: 0.0) <= 1.8 && (rainSpread ?: 0) <= 20 -> AgreementLevel.HIGH
                (tempSpread ?: 0.0) <= 3.5 && (rainSpread ?: 0) <= 40 -> AgreementLevel.MODERATE
                else -> AgreementLevel.LOW
            }

            val tempSpreadStr = tempSpread?.let { String.format(Locale.US, "Δ%.1f°C", it) } ?: "Single-model only"
            val rainSpreadStr = rainSpread?.let { "Δ$it%" } ?: "N/A"

            val expEn = if (entries.size >= 2) {
                "Across ${entries.size} models (${entries.joinToString { "${it.modelName}: ${it.maxTempC?.roundToInt() ?: "N/A"}°C" }}), peak temperature spread is $tempSpreadStr and rain probability spread is $rainSpreadStr (${agreement.labelEn})."
            } else {
                "Only primary Open-Meteo model available for ${baseDay.dayOfWeek}; multi-model comparison requires live ECMWF/GFS/ICON responses. Forecast uncertainty increases with lead time."
            }

            val expBn = if (entries.size >= 2) {
                "${baseDay.dayOfWeek}-এ ${entries.size}টি মডেলের মধ্যে সর্বোচ্চ তাপমাত্রার পার্থক্য $tempSpreadStr এবং বৃষ্টির সম্ভাবনার পার্থক্য $rainSpreadStr (${agreement.labelBn})।"
            } else {
                "${baseDay.dayOfWeek}-এর জন্য শুধুমাত্র প্রাথমিক মডেল ডেটা পাওয়া গেছে; সময়ের সাথে আবহাওয়া পূর্বাভাসের অনিশ্চয়তা বৃদ্ধি পায়।"
            }

            comparisons.add(
                DayModelComparison(
                    dateIso = baseDay.dateIso,
                    dayLabel = "${baseDay.dayOfWeek} (${baseDay.shortDate})",
                    models = entries,
                    maxTempSpreadC = tempSpread,
                    precipSpreadPercent = rainSpread,
                    windSpreadKmh = windSpread,
                    agreementLevel = agreement,
                    explanationEn = expEn,
                    explanationBn = expBn
                )
            )
        }

        val overallAgreement = when {
            comparisons.any { it.agreementLevel == AgreementLevel.LOW } -> AgreementLevel.LOW
            comparisons.all { it.agreementLevel == AgreementLevel.HIGH } && comparisons.isNotEmpty() -> AgreementLevel.HIGH
            else -> AgreementLevel.MODERATE
        }

        val overallEn = "Multi-model comparison for ${location.cityName} (${overallAgreement.labelEn}). " +
            "Note: Weather models are physical simulations, not guarantees; no artificial confidence percentages are invented."
        val overallBn = "${location.cityName}-এর জন্য বহুমুখী আবহাওয়া মডেলের তুলনা (${overallAgreement.labelBn})। " +
            "দ্রষ্টব্য: আবহাওয়ার পূর্বাভাস শতভাগ নিশ্চিত নয় এবং এখানে কোনো কাল্পনিক শতাংশ (%) তৈরি করা হয়নি।"

        return ForecastConfidenceReport(
            locationName = location.cityName,
            observedSummaryEn = observedSummaryEn,
            observedSummaryBn = observedSummaryBn,
            dailyComparisons = comparisons,
            overallAgreement = overallAgreement,
            overallSummaryEn = overallEn,
            overallSummaryBn = overallBn,
            fetchedAtMillis = fetchedAtMillis
        )
    }

    /**
     * Combines official National Weather Service (NWS) alerts with real-time WMO meteorological
     * threshold advisories derived strictly from actual live readings. Never invents alerts when
     * conditions are within normal ranges.
     */
    private fun buildAuthenticWeatherAlerts(
        location: SavedLocation,
        currentDto: CurrentDto,
        visibilityKm: Double?,
        currentUv: Double?,
        airQuality: AirQualityData?,
        nwsAlertsResponse: NwsAlertsResponse?
    ): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()
        val areaLabel = listOf(location.cityName, location.region, location.country)
            .filter { it.isNotBlank() }
            .joinToString(", ")

        // 1. Official NWS alerts (when available for the coordinates)
        nwsAlertsResponse?.features.orEmpty().take(4).forEachIndexed { idx, feature ->
            val props = feature.properties ?: return@forEachIndexed
            val headline = props.headline ?: props.event ?: return@forEachIndexed
            alerts.add(
                WeatherAlert(
                    id = feature.id ?: "nws_$idx",
                    headline = headline,
                    severity = props.severity ?: "Official",
                    affectedArea = props.areaDesc ?: areaLabel,
                    issuedAt = props.sent?.take(16)?.replace('T', ' ') ?: "Active Now",
                    expiresAt = props.expires?.take(16)?.replace('T', ' ') ?: "See official bulletin",
                    source = props.senderName ?: "National Weather Service (NWS)",
                    description = props.description?.trim()?.take(320) ?: headline
                )
            )
        }

        // 2. Real-time WMO meteorological threshold advisories based strictly on actual API telemetry
        val issueTime = currentDto.time.replace('T', ' ').ifBlank { "Live Observation" }
        if (currentDto.weatherCode in listOf(95, 96, 99)) {
            alerts.add(
                WeatherAlert(
                    id = "wmo_thunderstorm_${location.id}",
                    headline = "Active Thunderstorm & Lightning Advisory (WMO Code ${currentDto.weatherCode})",
                    severity = if (currentDto.weatherCode >= 96) "Severe" else "Moderate",
                    affectedArea = areaLabel,
                    issuedAt = issueTime,
                    source = "Open-Meteo WMO Live Telemetry",
                    description = "Live atmospheric telemetry indicates active thunderstorm activity in ${location.cityName}. Seek indoor shelter and monitor local lightning conditions."
                )
            )
        }
        if (currentDto.windSpeed10m >= 50.0) {
            alerts.add(
                WeatherAlert(
                    id = "wmo_wind_${location.id}",
                    headline = "High Wind Advisory (${currentDto.windSpeed10m.roundToInt()} km/h)",
                    severity = if (currentDto.windSpeed10m >= 75.0) "Severe" else "Moderate",
                    affectedArea = areaLabel,
                    issuedAt = issueTime,
                    source = "Open-Meteo WMO Live Telemetry",
                    description = "Sustained surface wind speeds of ${currentDto.windSpeed10m.roundToInt()} km/h detected at 10m elevation."
                )
            )
        }
        if (currentUv != null && currentUv >= 8.0) {
            alerts.add(
                WeatherAlert(
                    id = "wmo_uv_${location.id}",
                    headline = "Very High UV Radiation Advisory (UV ${String.format("%.1f", currentUv)})",
                    severity = "Advisory",
                    affectedArea = areaLabel,
                    issuedAt = issueTime,
                    source = "Open-Meteo Solar Telemetry",
                    description = "Live UV index is ${String.format("%.1f", currentUv)}. Minimize direct midday sun exposure and use UV protection."
                )
            )
        }
        if (visibilityKm != null && visibilityKm < 1.0) {
            alerts.add(
                WeatherAlert(
                    id = "wmo_vis_${location.id}",
                    headline = "Dense Low-Visibility Advisory (${String.format("%.1f", visibilityKm)} km)",
                    severity = "Moderate",
                    affectedArea = areaLabel,
                    issuedAt = issueTime,
                    source = "Open-Meteo Surface Visibility Telemetry",
                    description = "Horizontal visibility is reduced below 1.0 km. Exercise caution during transit."
                )
            )
        }
        val aqi = airQuality?.usAqi
        if (aqi != null && aqi >= 151) {
            alerts.add(
                WeatherAlert(
                    id = "aqi_alert_${location.id}",
                    headline = "Elevated Air Pollution Advisory (US AQI $aqi)",
                    severity = if (aqi >= 201) "Severe" else "Moderate",
                    affectedArea = areaLabel,
                    issuedAt = issueTime,
                    source = "Open-Meteo Air Quality Telemetry",
                    description = "Current US Air Quality Index is $aqi. Sensitive groups and outdoor commuters should limit prolonged exertion."
                )
            )
        }

        return alerts
    }

    private fun extractHourOfDay(isoTime: String): Int {
        val tIndex = isoTime.indexOf('T')
        if (tIndex != -1 && isoTime.length >= tIndex + 3) {
            return isoTime.substring(tIndex + 1, tIndex + 3).toIntOrNull() ?: 12
        }
        return 12
    }

    private fun formatIsoClock(isoDateTime: String): String {
        if (isoDateTime.isBlank()) return "Unavailable"
        val tIndex = isoDateTime.indexOf('T')
        val rawTime = if (tIndex != -1 && isoDateTime.length >= tIndex + 6) {
            isoDateTime.substring(tIndex + 1, tIndex + 6)
        } else {
            isoDateTime
        }
        val parts = rawTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return rawTime
        val m = parts.getOrNull(1) ?: "00"
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "$h12:$m $amPm"
    }

    private fun formatHourLabel(hour24: Int): String {
        val amPm = if (hour24 < 12) "AM" else "PM"
        val h12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return "$h12 $amPm"
    }

    private fun formatDayOfWeek(dateIso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formatter = SimpleDateFormat("EEE", Locale.US)
            val parsed = parser.parse(dateIso)
            if (parsed != null) formatter.format(parsed) else dateIso
        } catch (_: Exception) {
            dateIso
        }
    }

    private fun formatShortDate(dateIso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formatter = SimpleDateFormat("MMM d", Locale.US)
            val parsed = parser.parse(dateIso)
            if (parsed != null) formatter.format(parsed) else dateIso
        } catch (_: Exception) {
            dateIso
        }
    }
}
