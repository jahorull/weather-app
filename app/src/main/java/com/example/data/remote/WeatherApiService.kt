package com.example.data.remote

import com.example.BuildConfig
import com.example.model.CurrentWeather
import com.example.model.DailyForecast
import com.example.model.HourlyForecast
import com.example.model.SavedLocation
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

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,visibility,uv_index",
        @Query("hourly") hourly: String = "temperature_2m,apparent_temperature,precipitation_probability,weather_code,wind_speed_10m,relative_humidity_2m,uv_index,is_day",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max,wind_speed_10m_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoForecastResponse
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
    @Json(name = "temperature_2m") val temperature2m: Double = 24.0,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int = 60,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 25.0,
    @Json(name = "is_day") val isDay: Int = 1,
    @Json(name = "weather_code") val weatherCode: Int = 1,
    @Json(name = "surface_pressure") val surfacePressure: Double = 1013.0,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double = 12.0,
    @Json(name = "wind_direction_10m") val windDirection10m: Int = 180,
    @Json(name = "visibility") val visibilityMeters: Double? = 10000.0,
    @Json(name = "uv_index") val uvIndex: Double? = 5.0
)

data class HourlyDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double> = emptyList(),
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>? = null,
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double> = emptyList(),
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int>? = null,
    @Json(name = "uv_index") val uvIndex: List<Double?>? = null,
    @Json(name = "is_day") val isDay: List<Int>? = null
)

data class DailyDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @Json(name = "sunrise") val sunrise: List<String> = emptyList(),
    @Json(name = "sunset") val sunset: List<String> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?>? = null,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?>? = null,
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double> = emptyList()
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
                    .header("User-Agent", "Atmos3D-Android/1.0")
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
        fetchedAtMillis: Long = System.currentTimeMillis(),
        isFromCache: Boolean = false
    ): WeatherBundle {
        val currentDto = response.current ?: CurrentDto()
        val dailyDto = response.daily ?: DailyDto()
        val hourlyDto = response.hourly ?: HourlyDto()

        val currentHour = extractHourOfDay(currentDto.time)
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

        val todayHigh = dailyDto.temperature2mMax.firstOrNull() ?: (currentDto.temperature2m + 3.0)
        val todayLow = dailyDto.temperature2mMin.firstOrNull() ?: (currentDto.temperature2m - 4.0)
        val todaySunrise = formatIsoClock(dailyDto.sunrise.firstOrNull() ?: "06:05")
        val todaySunset = formatIsoClock(dailyDto.sunset.firstOrNull() ?: "18:15")
        val currentUv = currentDto.uvIndex
            ?: dailyDto.uvIndexMax?.firstOrNull()
            ?: if (isDay) 5.0 else 0.0
        val visibilityKm = ((currentDto.visibilityMeters ?: 10000.0) / 1000.0).coerceIn(0.5, 30.0)

        // Locate starting index in hourly array matching current time
        val startIndex = hourlyDto.time.indexOfFirst { it >= currentDto.time.take(13) }
            .let { if (it == -1) 0 else it }

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
                    precipProbabilityPercent = hourlyDto.precipitationProbability?.getOrNull(idx) ?: 0,
                    windSpeedKmh = hourlyDto.windSpeed10m.getOrElse(idx) { currentDto.windSpeed10m },
                    humidityPercent = hourlyDto.relativeHumidity2m?.getOrNull(idx) ?: currentDto.relativeHumidity2m,
                    uvIndex = hourlyDto.uvIndex?.getOrNull(idx) ?: 0.0,
                    conditionCode = code,
                    conditionText = WeatherSceneType.conditionLabelFromWmo(code, hrIsDay, hr),
                    sceneType = scene
                )
            )
        }

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
                    maxTempC = dailyDto.temperature2mMax.getOrElse(d) { todayHigh },
                    minTempC = dailyDto.temperature2mMin.getOrElse(d) { todayLow },
                    precipProbabilityPercent = dailyDto.precipitationProbabilityMax?.getOrNull(d) ?: 10,
                    maxWindSpeedKmh = dailyDto.windSpeed10mMax.getOrElse(d) { currentDto.windSpeed10m },
                    uvIndexMax = dailyDto.uvIndexMax?.getOrNull(d) ?: 6.0,
                    humidityAvgPercent = currentDto.relativeHumidity2m,
                    sunriseTime = formatIsoClock(dailyDto.sunrise.getOrElse(d) { "06:00" }),
                    sunsetTime = formatIsoClock(dailyDto.sunset.getOrElse(d) { "18:15" }),
                    conditionCode = code,
                    conditionText = WeatherSceneType.conditionLabelFromWmo(code, isDay = true, hourOfDay = 13),
                    sceneType = scene
                )
            )
        }

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
                uvIndex = currentUv,
                visibilityKm = visibilityKm,
                pressureHpa = currentDto.surfacePressure,
                sunriseTime = todaySunrise,
                sunsetTime = todaySunset,
                isDay = isDay
            ),
            hourly = hourlyList,
            daily = dailyList,
            fetchedAtMillis = fetchedAtMillis,
            isFromCache = isFromCache
        )
    }

    private fun extractHourOfDay(isoTime: String): Int {
        val tIndex = isoTime.indexOf('T')
        if (tIndex != -1 && isoTime.length >= tIndex + 3) {
            return isoTime.substring(tIndex + 1, tIndex + 3).toIntOrNull() ?: 12
        }
        return 12
    }

    private fun formatIsoClock(isoDateTime: String): String {
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
