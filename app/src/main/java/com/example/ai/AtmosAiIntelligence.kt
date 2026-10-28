package com.example.ai

import android.util.Log
import com.example.model.DailyForecast
import com.example.model.HourlyForecast
import com.example.model.SavedLocation
import com.example.model.UserSettings
import com.example.model.WeatherAlert
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

private const val TAG = "AtmosAiIntelligence"

enum class CopilotLanguage(val label: String, val instruction: String) {
    AUTO("Auto (EN / বাংলা)", "Respond in the same language the user used (English or Bengali / বাংলা). If the user asks for Bengali, respond in clear, natural Bengali."),
    ENGLISH("English", "Respond in clear, concise English."),
    BENGALI("বাংলা (Bengali)", "Respond in natural, easy-to-understand Bengali (বাংলা), keeping technical unit symbols (°C/°F, km/h, US AQI) clear.")
}

data class CopilotChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampLabel: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val groundingBadge: String? = null
)

data class LifeAdvisorItem(
    val category: String,
    val recommendation: String,
    val weatherReason: String,
    val badgeLabel: String = "Recommendation · Not a Guarantee"
)

data class AiSceneCommandResult(
    val commandText: String,
    val matchedScene: WeatherSceneType,
    val explanation: String,
    val visualHighlights: String
)

data class TravelComparisonSummary(
    val originCity: SavedLocation,
    val destinationCity: SavedLocation,
    val dayOffset: Int,
    val dateLabel: String,
    val isBeyondReliableHorizon: Boolean,
    val originDayForecast: DailyForecast?,
    val destinationDayForecast: DailyForecast?,
    val deterministicSummary: String
)

/**
 * Grounded AI Weather Intelligence Service powered by Firebase AI Logic (gemini-3.8-flash).
 * Strictly grounds every response in actual Open-Meteo Forecast, US AQI, and NWS/WMO Alert data.
 * Never fabricates measurements, forecasts, air quality readings, or official warnings.
 */
object AtmosAiIntelligence {

    private val chatModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val sceneControllerSchema = Schema.obj(
        properties = mapOf(
            "scenePreset" to Schema.enumeration(
                values = WeatherSceneType.entries.map { it.name },
                description = "Exact WeatherSceneType enum name from the validated preset list"
            ),
            "explanation" to Schema.string(
                description = "Short explanation confirming the 3D visual simulation and noting real weather is unchanged"
            ),
            "visualHighlights" to Schema.string(
                description = "Visual 3D atmosphere effects activated by this preset"
            )
        )
    )

    private val sceneControllerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = sceneControllerSchema
            }
        )
    }

    private val dailyAdvisorSchema = Schema.obj(
        properties = mapOf(
            "summaryHeadline" to Schema.string(description = "Overall daily weather guidance headline"),
            "recommendations" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "category" to Schema.string(description = "Category such as Umbrella, Outdoor Activity, Heat/UV Precaution, Clothing, or Air Quality"),
                        "recommendation" to Schema.string(description = "Practical recommendation labeled as advice, not a guarantee"),
                        "weatherReason" to Schema.string(description = "Exact API weather values behind this recommendation")
                    )
                ),
                description = "List of 4 to 5 grounded daily life recommendations"
            )
        )
    )

    private val dailyAdvisorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dailyAdvisorSchema
            }
        )
    }

    /**
     * Builds the authoritative, ground-truth weather telemetry context string from the app's
     * actual WeatherBundle. Explicitly distinguishes null ("Unavailable") from 0.
     */
    fun buildGroundedWeatherContext(
        activeLocation: SavedLocation,
        weatherBundle: WeatherBundle?,
        settings: UserSettings
    ): String {
        if (weatherBundle == null) {
            return """
                GROUNDED WEATHER TELEMETRY STATUS: UNAVAILABLE / OFFLINE
                Selected Location: ${activeLocation.cityName}, ${activeLocation.country} (${activeLocation.latitude}, ${activeLocation.longitude})
                Live API Data: UNAVAILABLE (No live or cached WeatherBundle is currently loaded).
                CRITICAL RULE: Do NOT invent or guess any temperatures, rain probabilities, wind speeds, AQI values, or alerts. State clearly that live weather data for ${activeLocation.cityName} is currently unavailable and advise the user to tap Refresh when connected to the internet.
            """.trimIndent()
        }

        val cur = weatherBundle.current
        val ageMinutes = ((System.currentTimeMillis() - weatherBundle.fetchedAtMillis) / 60_000L).coerceAtLeast(0L)
        val freshnessStatus = when {
            weatherBundle.isFromCache -> "CACHED DATA (Stored offline $ageMinutes minutes ago)"
            ageMinutes > 45 -> "STALE DATA (Last updated $ageMinutes minutes ago)"
            else -> "LIVE API DATA (${WeatherFormatter.formatRelativeUpdateTime(weatherBundle.fetchedAtMillis)})"
        }

        val hourlySummary = weatherBundle.hourly.take(12).joinToString(separator = "\n") { hr ->
            "  - ${hr.displayHour}: Temp ${WeatherFormatter.formatTemp(hr.temperatureC, settings.tempUnit)} (Feels ${
                WeatherFormatter.formatTemp(hr.feelsLikeC, settings.tempUnit)
            }), Condition=${hr.conditionText}, RainProb=${WeatherFormatter.formatNullablePercent(hr.precipProbabilityPercent)}, Wind=${
                WeatherFormatter.formatWind(hr.windSpeedKmh, settings.windUnit)
            }, Humidity=${WeatherFormatter.formatNullablePercent(hr.humidityPercent)}, UV=${
                hr.uvIndex?.let { String.format(Locale.US, "%.1f", it) } ?: "Unavailable"
            }"
        }

        val dailySummary = weatherBundle.daily.joinToString(separator = "\n") { day ->
            "  - ${day.dayOfWeek} (${day.shortDate}, ${day.dateIso}): High ${
                WeatherFormatter.formatTemp(day.maxTempC, settings.tempUnit)
            } / Low ${
                WeatherFormatter.formatTemp(day.minTempC, settings.tempUnit)
            }, Condition=${day.conditionText}, RainProb=${
                WeatherFormatter.formatNullablePercent(day.precipProbabilityPercent)
            }, MaxWind=${
                WeatherFormatter.formatWind(day.maxWindSpeedKmh, settings.windUnit)
            }, MaxUV=${
                day.uvIndexMax?.let { String.format(Locale.US, "%.1f", it) } ?: "Unavailable"
            }, Sunrise=${day.sunriseTime}, Sunset=${day.sunsetTime}"
        }

        val aq = weatherBundle.airQuality
        val airQualitySummary = if (aq == null) {
            "Unavailable (No station reading returned)"
        } else {
            "US AQI=${aq.usAqi ?: "Unavailable"} (${WeatherFormatter.aqiCategoryLabel(aq.usAqi)}), " +
                "PM2.5=${WeatherFormatter.formatNullablePollutant(aq.pm25)}, " +
                "PM10=${WeatherFormatter.formatNullablePollutant(aq.pm10)}, " +
                "Ozone(O3)=${WeatherFormatter.formatNullablePollutant(aq.ozone)}, " +
                "NO2=${WeatherFormatter.formatNullablePollutant(aq.nitrogenDioxide)}"
        }

        val alertsSummary = if (weatherBundle.alerts.isEmpty()) {
            "NONE (No active official NWS alerts or WMO severe threshold advisories)."
        } else {
            weatherBundle.alerts.joinToString(separator = "\n") { alert ->
                "  - [${alert.severity}] ${alert.headline} | Source: ${alert.source} | Issued: ${alert.issuedAt} | Expires: ${alert.expiresAt} | Area: ${alert.affectedArea} | Details: ${alert.description}"
            }
        }

        return """
            === GROUNDED ATMOSYN WEATHER API TELEMETRY ===
            Location: ${weatherBundle.location.cityName}, ${weatherBundle.location.region}, ${weatherBundle.location.country}
            Coordinates: ${weatherBundle.location.latitude}, ${weatherBundle.location.longitude}
            City Local Time: ${cur.cityLocalTimeLabel.ifBlank { "Unavailable" }} (Hour: ${cur.localHourOfDay}:00)
            Data Status & Freshness: $freshnessStatus
            
            CURRENT OBSERVED WEATHER:
            - Temperature: ${WeatherFormatter.formatTemp(cur.temperatureC, settings.tempUnit)}
            - Feels Like: ${WeatherFormatter.formatTemp(cur.feelsLikeC, settings.tempUnit)}
            - Today High / Low: ${WeatherFormatter.formatTemp(cur.highC, settings.tempUnit)} / ${WeatherFormatter.formatTemp(cur.lowC, settings.tempUnit)}
            - Condition: ${cur.conditionText} (WMO Code ${cur.conditionCode})
            - Precipitation Probability: ${WeatherFormatter.formatNullablePercent(cur.precipProbabilityPercent)}
            - Humidity: ${WeatherFormatter.formatNullablePercent(cur.humidityPercent)} (${WeatherFormatter.humidityComfortLabel(cur.humidityPercent)})
            - Wind Speed & Direction: ${WeatherFormatter.formatWind(cur.windSpeedKmh, settings.windUnit)}, Direction: ${
                cur.windDirectionDeg?.let { "${WeatherFormatter.windDirectionCardinal(it)} ($it°)" } ?: "Unavailable"
            }
            - UV Index: ${WeatherFormatter.formatNullableUv(cur.uvIndex)}
            - Visibility: ${WeatherFormatter.formatNullableVisibility(cur.visibilityKm)}
            - Surface Pressure: ${WeatherFormatter.formatNullablePressure(cur.pressureHpa)}
            - Sunrise / Sunset: ${cur.sunriseTime} / ${cur.sunsetTime}
            
            AIR QUALITY TELEMETRY (Open-Meteo Air Quality API):
            - $airQualitySummary
            
            ACTIVE WEATHER ALERTS (Official NWS + Live WMO Thresholds):
            $alertsSummary
            
            NEXT 12 HOURS (HOURLY FORECAST):
            $hourlySummary
            
            7-DAY FORECAST OUTLOOK:
            $dailySummary
            ===============================================
        """.trimIndent()
    }

    /**
     * Streams a grounded response from Gemini 3.8 Flash for the AI Weather Copilot,
     * Forecast Explainer, or Smart Alert Explainer.
     */
    fun streamGroundedCopilotAnswer(
        userPrompt: String,
        activeLocation: SavedLocation,
        weatherBundle: WeatherBundle?,
        settings: UserSettings,
        language: CopilotLanguage = CopilotLanguage.AUTO
    ): Flow<String> = flow {
        val telemetryContext = buildGroundedWeatherContext(activeLocation, weatherBundle, settings)
        val fullPrompt = """
            You are the Atmosyn AI Weather Intelligence Copilot.
            ${language.instruction}
            
            STRICT GROUNDING & SAFETY RULES:
            1. Use ONLY the actual weather API telemetry provided below. Never invent, guess, or fabricate temperatures, rain probabilities, wind speeds, UV indices, air quality readings, or weather warnings.
            2. Always mention the selected city (${activeLocation.cityName}) and data update status (${
                if (weatherBundle == null) "Offline/Unavailable"
                else if (weatherBundle.isFromCache) "Cached (${WeatherFormatter.formatRelativeUpdateTime(weatherBundle.fetchedAtMillis)})"
                else WeatherFormatter.formatRelativeUpdateTime(weatherBundle.fetchedAtMillis)
            }) when relevant.
            3. If any metric is marked "Unavailable", explicitly state that the station/API reading is unavailable rather than assuming 0.
            4. When giving daily life, umbrella, clothing, or travel advice, explicitly frame it as a practical recommendation based on the forecast, not a guarantee.
            5. Never invent severe weather warnings or imply that an AI recommendation is an official government alert.
            
            $telemetryContext
            
            USER QUESTION / REQUEST:
            $userPrompt
        """.trimIndent()

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chatModel.generateContentStream(fullPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini returned an empty response. Please tap Retry.")
        }
    }

    /**
     * Builds immediate deterministic Daily Life Advisor cards strictly from the actual
     * WeatherBundle so recommendations are always available and 100% grounded in live API numbers.
     */
    fun buildDeterministicDailyAdvisor(
        weatherBundle: WeatherBundle?,
        settings: UserSettings
    ): List<LifeAdvisorItem> {
        if (weatherBundle == null) return emptyList()
        val cur = weatherBundle.current
        val maxNext6hRain = weatherBundle.hourly.take(6).mapNotNull { it.precipProbabilityPercent }.maxOrNull()
            ?: cur.precipProbabilityPercent
        val items = mutableListOf<LifeAdvisorItem>()

        // 1. Umbrella & Rain Readiness
        val umbrellaRec = when {
            maxNext6hRain == null -> "Precipitation probability is currently unavailable; consider keeping a compact umbrella handy as a precaution."
            maxNext6hRain >= 50 -> "Recommended to carry an umbrella or waterproof jacket today."
            maxNext6hRain >= 25 -> "Pack a light foldable umbrella in case of passing showers."
            else -> "No umbrella needed for the immediate hours based on current low rain probability."
        }
        val umbrellaReason = "Current rain probability is ${
            WeatherFormatter.formatNullablePercent(cur.precipProbabilityPercent)
        } and peak 6-hour rain probability is ${WeatherFormatter.formatNullablePercent(maxNext6hRain)} (${cur.conditionText})."
        items.add(
            LifeAdvisorItem(
                category = "Umbrella & Rain Readiness",
                recommendation = umbrellaRec,
                weatherReason = umbrellaReason
            )
        )

        // 2. Outdoor Activity & Exercise
        val windFormatted = WeatherFormatter.formatWind(cur.windSpeedKmh, settings.windUnit)
        val outdoorRec = when {
            cur.conditionCode in listOf(95, 96, 99) -> "Postpone outdoor sports and seek indoor activities due to active thunderstorm conditions."
            (maxNext6hRain ?: 0) >= 60 || cur.windSpeedKmh >= 40.0 -> "Prefer indoor workouts or sheltered transit over open-air exercise right now."
            cur.feelsLikeC >= 35.0 -> "Schedule outdoor walks or runs for early morning or after sunset (${cur.sunsetTime}) to avoid peak heat."
            else -> "Conditions are favorable for outdoor walks, commuting, and recreation."
        }
        items.add(
            LifeAdvisorItem(
                category = "Outdoor Activities",
                recommendation = outdoorRec,
                weatherReason = "Condition is ${cur.conditionText}, wind speed is $windFormatted, and feels-like temperature is ${
                    WeatherFormatter.formatTemp(cur.feelsLikeC, settings.tempUnit)
                }."
            )
        )

        // 3. Heat, Thermal & UV Precaution
        val uv = cur.uvIndex
        val thermalRec = when {
            cur.feelsLikeC >= 34.0 && (uv ?: 0.0) >= 6.0 -> "Stay hydrated, wear sunglasses/SPF 30+, and limit direct midday sun exposure."
            cur.feelsLikeC >= 32.0 -> "Drink extra water and take shade breaks when outdoors due to warm apparent temperature."
            (uv ?: 0.0) >= 6.0 -> "Apply broad-spectrum sunscreen and wear UV eye protection during daylight hours."
            cur.feelsLikeC <= 10.0 -> "Protect extremities from wind chill when spending extended time outside."
            else -> "Thermal and UV levels are currently moderate; standard hydration is recommended."
        }
        items.add(
            LifeAdvisorItem(
                category = "Heat & UV Precautions",
                recommendation = thermalRec,
                weatherReason = "Feels-like temperature is ${
                    WeatherFormatter.formatTemp(cur.feelsLikeC, settings.tempUnit)
                } (Humidity ${WeatherFormatter.formatNullablePercent(cur.humidityPercent)}) and UV Index is ${
                    WeatherFormatter.formatNullableUv(uv)
                }."
            )
        )

        // 4. Clothing & Attire Suggestion
        val clothingRec = when {
            cur.feelsLikeC >= 30.0 -> "Lightweight, breathable cotton or moisture-wicking fabrics in light colors are recommended."
            cur.feelsLikeC >= 20.0 -> "Comfortable single-layer attire (t-shirt or light shirt) with a light outer layer for evening breeze."
            cur.feelsLikeC >= 10.0 -> "Layered clothing with a sweater or windproof jacket is recommended."
            else -> "Insulated thermal layers, warm coat, and wind-resistant outerwear are recommended."
        }
        items.add(
            LifeAdvisorItem(
                category = "Clothing & Attire",
                recommendation = clothingRec,
                weatherReason = "Ambient temperature is ${
                    WeatherFormatter.formatTemp(cur.temperatureC, settings.tempUnit)
                } (High ${WeatherFormatter.formatTemp(cur.highC, settings.tempUnit)} / Low ${
                    WeatherFormatter.formatTemp(cur.lowC, settings.tempUnit)
                })."
            )
        )

        // 5. Air Quality Guidance (when available)
        val aqi = weatherBundle.airQuality?.usAqi
        if (aqi != null) {
            val aqiRec = when {
                aqi <= 50 -> "Air quality is Good; ideal for outdoor ventilation and exercise."
                aqi <= 100 -> "Air quality is Moderate; unusually sensitive individuals should monitor prolonged exertion."
                else -> "Consider reducing intense outdoor exertion near heavy traffic and wearing a mask if sensitive to particulates."
            }
            items.add(
                LifeAdvisorItem(
                    category = "Air Quality & Breathing",
                    recommendation = aqiRec,
                    weatherReason = "Live US AQI is $aqi (${WeatherFormatter.aqiCategoryLabel(aqi)}), PM2.5 is ${
                        WeatherFormatter.formatNullablePollutant(weatherBundle.airQuality.pm25)
                    }."
                )
            )
        }

        return items
    }

    /**
     * Generates AI-enhanced Daily Life Advisor recommendations using Gemini structured JSON output.
     */
    suspend fun generateAiDailyLifeAdvisor(
        activeLocation: SavedLocation,
        weatherBundle: WeatherBundle,
        settings: UserSettings,
        language: CopilotLanguage
    ): Pair<String, List<LifeAdvisorItem>> {
        val context = buildGroundedWeatherContext(activeLocation, weatherBundle, settings)
        val prompt = """
            Generate practical Daily Life Advisor recommendations for ${activeLocation.cityName} based strictly on the actual weather telemetry below.
            ${language.instruction}
            Include recommendations for:
            1. Umbrella & Rain Readiness
            2. Outdoor Activity Suggestions
            3. Heat & UV Precautions
            4. Clothing & Attire Suggestions
            5. Air Quality Precautions (using actual US AQI / PM2.5 if available)
            
            Every item MUST explain the exact weather numbers behind the recommendation and treat advice as a recommendation, not a guarantee.
            
            $context
        """.trimIndent()

        val response = withTimeout(60_000L) {
            dailyAdvisorModel.generateContent(prompt)
        }
        val rawJson = response.text ?: throw IllegalStateException("Empty JSON response from model.")
        val root = JSONObject(rawJson)
        val headline = root.optString("summaryHeadline", "AI Daily Life Recommendations for ${activeLocation.cityName}")
        val arr = root.optJSONArray("recommendations")
        val list = mutableListOf<LifeAdvisorItem>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    LifeAdvisorItem(
                        category = obj.optString("category", "Advisory"),
                        recommendation = obj.optString("recommendation", ""),
                        weatherReason = obj.optString("weatherReason", "")
                    )
                )
            }
        }
        return headline to list
    }

    /**
     * Fast, deterministic natural-language matcher for common 3D scene commands in English and Bengali.
     * Maps strictly to the validated WeatherSceneType enum.
     */
    fun matchLocalSceneCommand(command: String): WeatherSceneType? {
        val norm = command.lowercase(Locale.ROOT).trim()
        if (norm.isBlank()) return null

        return when {
            norm.contains("thunder") || norm.contains("lightning") || norm.contains("storm") ||
                norm.contains("বজ্র") || norm.contains("ঝড়") || norm.contains("ঝড়") -> WeatherSceneType.THUNDERSTORM
            norm.contains("snow") || norm.contains("blizzard") || norm.contains("frost") ||
                norm.contains(" তুষার") || norm.contains("বরফ") -> WeatherSceneType.SNOWY
            norm.contains("rain") || norm.contains("shower") || norm.contains("drizzle") ||
                norm.contains("বৃষ্টি") -> WeatherSceneType.RAINY
            norm.contains("fog") || norm.contains("mist") || norm.contains("haze") ||
                norm.contains("কুয়াশা") || norm.contains("কুয়াশা") -> WeatherSceneType.FOGGY
            norm.contains("sunset") || norm.contains("dusk") || norm.contains("twilight") || norm.contains("golden") ||
                norm.contains("সূর্যাস্ত") || norm.contains("গোধূলি") -> WeatherSceneType.SUNSET
            norm.contains("night") || norm.contains("star") || norm.contains("moon") || norm.contains("aurora") ||
                norm.contains("রাত") || norm.contains("তারা") || norm.contains("চাঁদ") -> WeatherSceneType.NIGHT
            norm.contains("cloud") || norm.contains("overcast") ||
                norm.contains("মেঘ") -> WeatherSceneType.CLOUDY
            norm.contains("sun") || norm.contains("clear") || norm.contains("bright") ||
                norm.contains("রোদ") || norm.contains("রৌদ্র") || norm.contains("পরিষ্কার") -> WeatherSceneType.SUNNY
            else -> null
        }
    }

    /**
     * Resolves a natural-language 3D scene command into one of the 8 validated WeatherSceneType presets.
     * First checks fast deterministic rules (with composite nuance like "rainy night"), and can also
     * invoke Gemini structured JSON output for complex natural-language phrasing.
     */
    suspend fun resolve3DSceneCommand(
        command: String,
        observedScene: WeatherSceneType?
    ): AiSceneCommandResult {
        val trimmed = command.trim()
        require(trimmed.isNotEmpty()) { "Please enter a 3D scene command (e.g., 'Show a rainy night' or 'Create a golden sunset')." }

        val lower = trimmed.lowercase(Locale.ROOT)
        // Check composite natural-language commands first (e.g. "Show a rainy night")
        val directMatch = when {
            lower.contains("rain") && lower.contains("night") -> WeatherSceneType.RAINY
            lower.contains("fog") && lower.contains("morning") -> WeatherSceneType.FOGGY
            lower.contains("golden") && lower.contains("sunset") -> WeatherSceneType.SUNSET
            else -> matchLocalSceneCommand(trimmed)
        }

        // Try Gemini structured JSON schema classification for rich explanation, falling back to validated local match
        try {
            val prompt = """
                Map the user's natural-language 3D weather scene command to the single best matching preset from the validated enum list:
                - SUNNY (Sunny & Clear: 3D sun sphere, volumetric rays, blue sky)
                - CLOUDY (Partly Cloudy: multi-layered 3D drifting volumetric clouds)
                - RAINY (Rain Showers: 3D rain particles, wet-glass droplets, surface ripples; also use for 'rainy night')
                - THUNDERSTORM (Thunderstorm: dark storm clouds, branching 3D lightning bolts, heavy downpour)
                - SNOWY (Snowfall: 3D tumbling snowflakes, alpine snowdrifts)
                - FOGGY (Atmospheric Fog: 5 drifting mist layers, reduced horizon visibility; also use for 'foggy morning')
                - SUNSET (Golden Sunset: warm orange-purple twilight sky, glowing low-horizon 3D sun)
                - NIGHT (Starlit Night: 3D cratered moon, parallax starfield, aurora borealis)
                
                Observed live weather scene right now: ${observedScene?.title ?: "Live API"}
                User command: "$trimmed"
            """.trimIndent()

            val response = withTimeout(30_000L) {
                sceneControllerModel.generateContent(prompt)
            }
            val rawJson = response.text
            if (!rawJson.isNullOrBlank()) {
                val json = JSONObject(rawJson)
                val presetName = json.optString("scenePreset", "")
                val validatedScene = WeatherSceneType.entries.find { it.name.equals(presetName, ignoreCase = true) }
                    ?: directMatch
                    ?: WeatherSceneType.SUNNY
                val explanation = json.optString(
                    "explanation",
                    "Switched 3D viewport to simulated ${validatedScene.title} preset. Observed live weather data remains unchanged."
                )
                val highlights = json.optString("visualHighlights", validatedScene.subtitle)
                return AiSceneCommandResult(
                    commandText = trimmed,
                    matchedScene = validatedScene,
                    explanation = explanation,
                    visualHighlights = highlights
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "AI scene classification timed out; using validated local preset matcher", e)
        } catch (e: Exception) {
            Log.w(TAG, "AI scene classification fallback to validated preset matcher", e)
        }

        val resolved = directMatch
            ?: throw IllegalArgumentException(
                "Could not match \"$trimmed\" to a validated 3D weather preset. Try commands like 'Show a rainy night', 'Create a golden sunset', 'Show a foggy morning', or 'Switch to a sunny scene'."
            )
        return AiSceneCommandResult(
            commandText = trimmed,
            matchedScene = resolved,
            explanation = "Activated simulated '${resolved.title}' 3D environment for command \"$trimmed\". Your real observed weather data remains separate and unmodified.",
            visualHighlights = resolved.subtitle
        )
    }

    /**
     * Compares Origin and Destination forecasts for a selected day offset (0..9)
     * and generates a grounded comparison summary.
     */
    fun buildTravelComparisonSummary(
        originLocation: SavedLocation,
        originBundle: WeatherBundle?,
        destinationLocation: SavedLocation,
        destinationBundle: WeatherBundle?,
        dayOffset: Int,
        settings: UserSettings
    ): TravelComparisonSummary {
        val isBeyondHorizon = dayOffset >= 7
        val originDay = if (!isBeyondHorizon) originBundle?.daily?.getOrNull(dayOffset) else null
        val destDay = if (!isBeyondHorizon) destinationBundle?.daily?.getOrNull(dayOffset) else null

        val dateLabel = when {
            dayOffset == 0 -> "Today"
            dayOffset == 1 -> "Tomorrow (+1d)"
            originDay != null -> "${originDay.dayOfWeek} (${originDay.shortDate})"
            destDay != null -> "${destDay.dayOfWeek} (${destDay.shortDate})"
            else -> "Day +$dayOffset"
        }

        val summary = when {
            isBeyondHorizon -> {
                "Selected travel date ($dateLabel, +$dayOffset days ahead) is beyond the 7-day reliable meteorological forecast window. Standard deterministic weather models do not provide reliable daily precipitation or temperature specifics beyond 7 days. Re-check within 7 days of departure."
            }
            destinationBundle == null || destDay == null -> {
                "Live forecast for destination ${destinationLocation.cityName} on $dateLabel is currently unavailable. Connect to the internet and tap 'Compare & Plan Trip' to fetch live destination telemetry."
            }
            originDay == null -> {
                "Destination ${destinationLocation.cityName} on $dateLabel: ${destDay.conditionText}, High ${
                    WeatherFormatter.formatTemp(destDay.maxTempC, settings.tempUnit)
                } / Low ${WeatherFormatter.formatTemp(destDay.minTempC, settings.tempUnit)}, Rain chance ${
                    WeatherFormatter.formatNullablePercent(destDay.precipProbabilityPercent)
                }, Max wind ${WeatherFormatter.formatWind(destDay.maxWindSpeedKmh, settings.windUnit)}. (Origin forecast for ${originLocation.cityName} is currently unavailable)."
            }
            else -> {
                val tempDiff = (destDay.maxTempC - originDay.maxTempC)
                val warmerCooler = when {
                    tempDiff > 2.0 -> "${String.format(Locale.US, "%.0f", tempDiff)}°C warmer than ${originLocation.cityName}"
                    tempDiff < -2.0 -> "${String.format(Locale.US, "%.0f", kotlin.math.abs(tempDiff))}°C cooler than ${originLocation.cityName}"
                    else -> "similar in peak temperature to ${originLocation.cityName}"
                }
                "On $dateLabel, ${destinationLocation.cityName} (${destDay.conditionText}, High ${
                    WeatherFormatter.formatTemp(destDay.maxTempC, settings.tempUnit)
                } / Low ${WeatherFormatter.formatTemp(destDay.minTempC, settings.tempUnit)}, Rain ${
                    WeatherFormatter.formatNullablePercent(destDay.precipProbabilityPercent)
                }) is expected to be $warmerCooler (${originDay.conditionText}, High ${
                    WeatherFormatter.formatTemp(originDay.maxTempC, settings.tempUnit)
                } / Low ${WeatherFormatter.formatTemp(originDay.minTempC, settings.tempUnit)}, Rain ${
                    WeatherFormatter.formatNullablePercent(originDay.precipProbabilityPercent)
                })."
            }
        }

        return TravelComparisonSummary(
            originCity = originLocation,
            destinationCity = destinationLocation,
            dayOffset = dayOffset,
            dateLabel = dateLabel,
            isBeyondReliableHorizon = isBeyondHorizon,
            originDayForecast = originDay,
            destinationDayForecast = destDay,
            deterministicSummary = summary
        )
    }
}
