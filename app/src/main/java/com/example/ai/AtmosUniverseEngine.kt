package com.example.ai

import com.example.model.SavedLocation
import com.example.model.TimeMachinePointType
import com.example.model.TimeMachineSlot
import com.example.model.TwinInterestCategory
import com.example.model.TwinRecommendation
import com.example.model.TwinUserPreference
import com.example.model.UserSettings
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.example.model.WhatIfSimulationState
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Engine powering the three Atmos 3D Universe signature features:
 * 1. ATMOS WHAT-IF LAB (Validated 3D Weather Experimentation & Natural-Language Command Mapper)
 * 2. ATMOS WEATHER TIME MACHINE (Historical Observations vs. Current vs. Forecasts vs. Unavailable Periods)
 * 3. PERSONAL WEATHER TWIN (Local Interest Profiles with Save/Edit/Delete & Grounded Forecast Evidence)
 */
object AtmosUniverseEngine {

    private const val MODEL_NAME = "gemini-3-flash-preview"

    // =========================================================================
    // 1. ATMOS WHAT-IF LAB: Validated 3D Scene Experimentation & AI Mapper
    // =========================================================================

    val sampleWhatIfCommands: List<String> = listOf(
        "Create a stormy sunset scene.",
        "Dense foggy morning with light rain.",
        "Clear sunny midday with strong wind.",
        "Starlit night with gentle breeze.",
        "Heavy monsoon thunderstorm at dusk."
    )

    /**
     * Maps natural-language commands into validated, bounded [WhatIfSimulationState] parameters
     * without ever executing arbitrary generated code.
     */
    fun parseWhatIfCommandLocally(command: String): WhatIfSimulationState? {
        val text = command.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) return null

        val mentionsStorm = text.contains("storm") || text.contains("thunder") || text.contains("lightning") ||
            text.contains("squall") || text.contains("monsoon") || text.contains("ঝড়") || text.contains("বজ্র")
        val mentionsRain = text.contains("rain") || text.contains("drizzle") || text.contains("shower") ||
            text.contains("downpour") || text.contains("wet") || text.contains("বৃষ্টি")
        val mentionsFog = text.contains("fog") || text.contains("mist") || text.contains("haze") ||
            text.contains("কুয়াশা")
        val mentionsSunset = text.contains("sunset") || text.contains("dusk") || text.contains("twilight") ||
            text.contains("golden") || text.contains("evening") || text.contains("সূর্যাস্ত") || text.contains("গোধূলি")
        val mentionsMorning = text.contains("morning") || text.contains("dawn") || text.contains("sunrise") ||
            text.contains("সকাল") || text.contains("ভোর")
        val mentionsNight = text.contains("night") || text.contains("midnight") || text.contains("star") ||
            text.contains("moon") || text.contains("রাত")
        val mentionsSunny = text.contains("sunny") || text.contains("clear") || text.contains("bright") ||
            text.contains("midday") || text.contains("noon") || text.contains("রোদ")
        val mentionsCloud = text.contains("cloud") || text.contains("overcast") || text.contains("মেঘ")
        val mentionsWind = text.contains("wind") || text.contains("gale") || text.contains("breeze") ||
            text.contains("gust") || text.contains("বাতাস")

        val matchedAny = mentionsStorm || mentionsRain || mentionsFog || mentionsSunset ||
            mentionsMorning || mentionsNight || mentionsSunny || mentionsCloud || mentionsWind
        if (!matchedAny) return null

        val timeOfDayHour = when {
            mentionsSunset -> 18.0f
            mentionsNight -> 22.0f
            mentionsMorning -> 6.5f
            else -> 13.5f
        }

        val windStrength = when {
            mentionsStorm -> 0.85f
            text.contains("strong wind") || text.contains("gale") || text.contains("gust") -> 0.82f
            mentionsWind -> 0.55f
            mentionsRain -> 0.50f
            else -> 0.30f
        }

        val rainIntensity = when {
            mentionsStorm && mentionsSunset -> 0.68f
            mentionsStorm -> 0.88f
            text.contains("light rain") || text.contains("drizzle") -> 0.38f
            mentionsRain -> 0.65f
            else -> 0.0f
        }

        val cloudDensity = when {
            mentionsStorm -> 0.88f
            mentionsRain -> 0.76f
            mentionsCloud -> 0.78f
            mentionsSunset -> 0.55f
            mentionsFog -> 0.62f
            mentionsSunny -> 0.18f
            else -> 0.35f
        }

        val fogDensity = when {
            mentionsFog -> 0.82f
            mentionsStorm -> 0.32f
            mentionsMorning && mentionsRain -> 0.55f
            else -> 0.10f
        }

        val sunlightIntensity = when {
            mentionsNight -> 0.05f
            mentionsSunset -> 0.78f
            mentionsSunny -> 0.95f
            mentionsStorm -> 0.25f
            mentionsFog -> 0.32f
            else -> 0.65f
        }

        // Special handling for "stormy sunset" so the sunset sky + storm clouds + rain + lightning coexist
        val derivedScene = if (mentionsStorm && mentionsSunset) {
            WeatherSceneType.SUNSET
        } else {
            WhatIfSimulationState.deriveSceneFromParameters(
                windStrength = windStrength,
                cloudDensity = cloudDensity,
                rainIntensity = rainIntensity,
                fogDensity = fogDensity,
                sunlightIntensity = sunlightIntensity,
                timeOfDayHour = timeOfDayHour
            )
        }

        val explanation = buildString {
            append("HYPOTHETICAL SIMULATION for \"${command.trim()}\": ")
            append("Wind ${(windStrength * 100).roundToInt()}%, ")
            append("Clouds ${(cloudDensity * 100).roundToInt()}%, ")
            append("Rain ${(rainIntensity * 100).roundToInt()}%, ")
            append("Fog ${(fogDensity * 100).roundToInt()}%, ")
            append("Sunlight ${(sunlightIntensity * 100).roundToInt()}%, ")
            append("Clock ${formatFractionalHour(timeOfDayHour)}. ")
            append("This simulation only animates the 3D viewport and does not alter or predict real weather.")
        }

        return WhatIfSimulationState(
            isActive = true,
            windStrength = windStrength,
            cloudDensity = cloudDensity,
            rainIntensity = rainIntensity,
            fogDensity = fogDensity,
            sunlightIntensity = sunlightIntensity,
            timeOfDayHour = timeOfDayHour,
            derivedSceneType = derivedScene,
            activeCommandLabel = command.trim(),
            simulationExplanation = explanation
        )
    }

    /**
     * Uses Gemini structured JSON output to map natural-language What-If commands to bounded
     * simulation parameters, with deterministic fallback.
     */
    suspend fun resolveWhatIfCommand(command: String): WhatIfSimulationState {
        val localMatch = parseWhatIfCommandLocally(command)
        try {
            val schema = Schema.obj(
                mapOf(
                    "windStrength" to Schema.double("Wind strength from 0.0 to 1.0"),
                    "cloudDensity" to Schema.double("Cloud density from 0.0 to 1.0"),
                    "rainIntensity" to Schema.double("Rain intensity from 0.0 to 1.0"),
                    "fogDensity" to Schema.double("Fog density from 0.0 to 1.0"),
                    "sunlightIntensity" to Schema.double("Sunlight intensity from 0.0 to 1.0"),
                    "timeOfDayHour" to Schema.double("Hour of day from 0.0 to 23.5 (e.g. 18.0 for sunset, 22.0 for night)"),
                    "explanation" to Schema.enumeration(
                        listOf("SUNNY", "CLOUDY", "RAINY", "THUNDERSTORM", "SNOWY", "FOGGY", "SUNSET", "NIGHT"),
                        "Primary base 3D atmosphere style"
                    ),
                    "summaryNote" to Schema.string("1-sentence description of this hypothetical 3D simulation")
                )
            )
            val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = MODEL_NAME,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = schema
                    temperature = 0.1f
                }
            )
            val prompt = """
                Map the user's hypothetical 3D weather experiment command into bounded visual simulation parameters (0.0 to 1.0) and timeOfDayHour (0.0 to 23.5).
                Never claim this changes or predicts real atmospheric weather.
                User Command: "${command.trim()}"
            """.trimIndent()

            val responseText = withTimeout(9_000L) {
                model.generateContent(prompt).text
            }
            if (!responseText.isNullOrBlank()) {
                val json = JSONObject(responseText)
                val wind = json.optDouble("windStrength", localMatch?.windStrength?.toDouble() ?: 0.5).toFloat().coerceIn(0f, 1f)
                val clouds = json.optDouble("cloudDensity", localMatch?.cloudDensity?.toDouble() ?: 0.5).toFloat().coerceIn(0f, 1f)
                val rain = json.optDouble("rainIntensity", localMatch?.rainIntensity?.toDouble() ?: 0.0).toFloat().coerceIn(0f, 1f)
                val fog = json.optDouble("fogDensity", localMatch?.fogDensity?.toDouble() ?: 0.1).toFloat().coerceIn(0f, 1f)
                val sun = json.optDouble("sunlightIntensity", localMatch?.sunlightIntensity?.toDouble() ?: 0.7).toFloat().coerceIn(0f, 1f)
                val hour = json.optDouble("timeOfDayHour", localMatch?.timeOfDayHour?.toDouble() ?: 14.0).toFloat().coerceIn(0f, 23.5f)
                val baseSceneStr = json.optString("explanation", "")
                val summaryNote = json.optString("summaryNote", "").ifBlank {
                    localMatch?.simulationExplanation ?: "Hypothetical 3D weather simulation active."
                }
                val derived = runCatching { WeatherSceneType.valueOf(baseSceneStr.uppercase(Locale.ROOT)) }.getOrElse {
                    WhatIfSimulationState.deriveSceneFromParameters(wind, clouds, rain, fog, sun, hour)
                }
                return WhatIfSimulationState(
                    isActive = true,
                    windStrength = wind,
                    cloudDensity = clouds,
                    rainIntensity = rain,
                    fogDensity = fog,
                    sunlightIntensity = sun,
                    timeOfDayHour = hour,
                    derivedSceneType = derived,
                    activeCommandLabel = command.trim(),
                    simulationExplanation = "HYPOTHETICAL SIMULATION: $summaryNote (Does not alter or predict real atmospheric conditions.)"
                )
            }
        } catch (_: Exception) {
            // Fall back to deterministic local parser below
        }

        if (localMatch != null) return localMatch

        return WhatIfSimulationState(
            isActive = true,
            windStrength = 0.50f,
            cloudDensity = 0.60f,
            rainIntensity = 0.30f,
            fogDensity = 0.20f,
            sunlightIntensity = 0.65f,
            timeOfDayHour = 17.5f,
            derivedSceneType = WeatherSceneType.SUNSET,
            activeCommandLabel = command.trim(),
            simulationExplanation = "HYPOTHETICAL SIMULATION for \"${command.trim()}\": Custom atmospheric mix applied to 3D viewport. Real weather telemetry remains unmodified."
        )
    }

    fun formatFractionalHour(hour: Float): String {
        val clamped = hour.coerceIn(0f, 23.9f)
        val h = clamped.toInt()
        val m = ((clamped - h) * 60f).roundToInt().coerceIn(0, 59)
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    // =========================================================================
    // 2. ATMOS WEATHER TIME MACHINE: Historical Obs vs Current vs Forecast vs Unavailable
    // =========================================================================

    fun buildTimeMachineSlots(
        location: SavedLocation,
        bundle: WeatherBundle?
    ): List<TimeMachineSlot> {
        if (bundle == null) {
            return listOf(
                TimeMachineSlot(
                    id = "offline_unavailable",
                    shortLabel = "Offline (Unavailable)",
                    timestampLabel = "No Live Sync",
                    pointType = TimeMachinePointType.UNAVAILABLE_PERIOD,
                    temperatureC = null,
                    feelsLikeC = null,
                    precipProbabilityPercent = null,
                    precipitationMm = null,
                    windSpeedKmh = null,
                    humidityPercent = null,
                    uvIndex = null,
                    conditionText = "Telemetry Unavailable Offline",
                    sceneType = null,
                    localHourOfDay = 12,
                    dataSourceLabel = "No Active Feed (${location.cityName})",
                    honestyNoteEn = "Historical observations and forecasts for ${location.cityName} are currently unavailable offline. Atmosyn never invents historical or forecast measurements.",
                    honestyNoteBn = "${location.cityName}-এর অতীতের পর্যবেক্ষণ ও পূর্বাভাস অফলাইনে অনুপলব্ধ। Atmosyn কখনোই কাল্পনিক ডেটা তৈরি করে না।"
                )
            )
        }

        val slots = mutableListOf<TimeMachineSlot>()

        // 1. Explicit Historical Archive Boundary (>24h ago) — demonstrates honest Unavailable Period handling
        slots.add(
            TimeMachineSlot(
                id = "archive_unavailable_past_7d",
                shortLabel = "-7 Days (Archive N/A)",
                timestampLabel = "Beyond 24h Live Observation Window",
                pointType = TimeMachinePointType.UNAVAILABLE_PERIOD,
                temperatureC = null,
                feelsLikeC = null,
                precipProbabilityPercent = null,
                precipitationMm = null,
                windSpeedKmh = null,
                humidityPercent = null,
                uvIndex = null,
                conditionText = "Historical Archive Period Unavailable",
                sceneType = null,
                localHourOfDay = 12,
                dataSourceLabel = "Open-Meteo Live Feed (past_hours=24 limit)",
                honestyNoteEn = "Unavailable Period: Multi-day historical archive observations (>24 hours ago) are not included in the live forecast endpoint. Atmosyn leaves this period empty rather than fabricating past weather.",
                honestyNoteBn = "তথ্য অনুপলব্ধ: ২৪ ঘণ্টার বেশি পুরনো আর্কাইভ পর্যবেক্ষণ লাইভ ফিডে নেই। Atmosyn কোনো কাল্পনিক অতীতের আবহাওয়া দেখায় না।"
            )
        )

        // 2. Historical Observations from pastHourly (or radarTimeline historical frames if pastHourly is shorter)
        val pastSource = bundle.pastHourly
        if (pastSource.isNotEmpty()) {
            val desiredOffsets = listOf(24, 18, 12, 6, 3, 1)
            val totalPast = pastSource.size
            val selectedIndices = desiredOffsets
                .mapNotNull { hoursAgo ->
                    val idx = totalPast - hoursAgo
                    if (idx in pastSource.indices) idx else null
                }
                .ifEmpty { pastSource.indices.toList().takeLast(6) }
                .distinct()
                .sorted()

            selectedIndices.forEach { idx ->
                val hr = pastSource[idx]
                val hoursAgo = (totalPast - idx).coerceAtLeast(1)
                val clockHour = extractHourFromIso(hr.timeIso, bundle.current.localHourOfDay)
                slots.add(
                    TimeMachineSlot(
                        id = "hist_${hr.timeIso}_$idx",
                        shortLabel = "-${hoursAgo}h Obs",
                        timestampLabel = formatIsoTimestampReadable(hr.timeIso, "-${hoursAgo}h Observed"),
                        pointType = TimeMachinePointType.HISTORICAL_OBSERVATION,
                        temperatureC = hr.temperatureC,
                        feelsLikeC = hr.feelsLikeC,
                        precipProbabilityPercent = hr.precipProbabilityPercent,
                        precipitationMm = hr.precipitationMm,
                        windSpeedKmh = hr.windSpeedKmh,
                        humidityPercent = hr.humidityPercent,
                        uvIndex = hr.uvIndex,
                        conditionText = hr.conditionText,
                        sceneType = hr.sceneType,
                        localHourOfDay = clockHour,
                        dataSourceLabel = "Open-Meteo Historical Hourly Observation (past_hours)",
                        honestyNoteEn = "Historical Observation recorded $hoursAgo hour(s) ago for ${location.cityName}. Verified telemetry from Open-Meteo past hourly analysis.",
                        honestyNoteBn = "${location.cityName}-এ $hoursAgo ঘণ্টা আগের প্রকৃত পর্যবেক্ষণকৃত আবহাওয়া।"
                    )
                )
            }
        }

        // 3. Current Live Observation ("Now")
        val cur = bundle.current
        val fetchedClock = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(bundle.fetchedAtMillis))
        slots.add(
            TimeMachineSlot(
                id = "current_live_now",
                shortLabel = "Now (Live)",
                timestampLabel = if (cur.cityLocalTimeLabel.isNotBlank()) {
                    "Local ${cur.cityLocalTimeLabel} · Synced $fetchedClock"
                } else {
                    "Synced $fetchedClock"
                },
                pointType = TimeMachinePointType.CURRENT_OBSERVATION,
                temperatureC = cur.temperatureC,
                feelsLikeC = cur.feelsLikeC,
                precipProbabilityPercent = cur.precipProbabilityPercent,
                precipitationMm = bundle.hourly.firstOrNull()?.precipitationMm ?: 0.0,
                windSpeedKmh = cur.windSpeedKmh,
                humidityPercent = cur.humidityPercent,
                uvIndex = cur.uvIndex,
                conditionText = cur.conditionText,
                sceneType = cur.sceneType,
                localHourOfDay = cur.localHourOfDay,
                dataSourceLabel = if (bundle.isFromCache) {
                    "Open-Meteo Current Observation (Room Offline Cache)"
                } else {
                    "Open-Meteo Real-Time Current Observation"
                },
                honestyNoteEn = "Current Live Observation for ${location.cityName}. Synchronized with current station & model analysis.",
                honestyNoteBn = "${location.cityName}-এর বর্তমান লাইভ আবহাওয়া পর্যবেক্ষণ।"
            )
        )

        // 4. Future Hourly Forecasts (+3h, +6h, +12h, +18h)
        val futureHourOffsets = listOf(3, 6, 12, 18)
        futureHourOffsets.forEach { offset ->
            val hr = bundle.hourly.getOrNull(offset)
            if (hr != null) {
                val clockHour = extractHourFromIso(hr.timeIso, (cur.localHourOfDay + offset) % 24)
                slots.add(
                    TimeMachineSlot(
                        id = "fcst_hr_${hr.timeIso}_$offset",
                        shortLabel = "+${offset}h Forecast",
                        timestampLabel = formatIsoTimestampReadable(hr.timeIso, "+${offset}h (${hr.displayHour})"),
                        pointType = TimeMachinePointType.FORECAST,
                        temperatureC = hr.temperatureC,
                        feelsLikeC = hr.feelsLikeC,
                        precipProbabilityPercent = hr.precipProbabilityPercent,
                        precipitationMm = hr.precipitationMm,
                        windSpeedKmh = hr.windSpeedKmh,
                        humidityPercent = hr.humidityPercent,
                        uvIndex = hr.uvIndex,
                        conditionText = hr.conditionText,
                        sceneType = hr.sceneType,
                        localHourOfDay = clockHour,
                        dataSourceLabel = "Open-Meteo Hourly Numerical Forecast (+${offset}h)",
                        honestyNoteEn = "Numerical Forecast (+${offset}h ahead): Subject to atmospheric model uncertainty — not a guaranteed outcome.",
                        honestyNoteBn = "+${offset} ঘণ্টার পূর্বাভাস: আবহাওয়া মডেলের পূর্বাভাস পরিবর্তনশীল, এটি শতভাগ নিশ্চিত ফলাফল নয়।"
                    )
                )
            }
        }

        // 5. Future Multi-Day Forecasts (Tomorrow, +3 Days, +6 Days)
        val futureDayIndices = listOf(1, 3, 6)
        futureDayIndices.forEach { dIdx ->
            val day = bundle.daily.getOrNull(dIdx)
            if (day != null) {
                slots.add(
                    TimeMachineSlot(
                        id = "fcst_day_${day.dateIso}_$dIdx",
                        shortLabel = "+${dIdx}d (${day.dayOfWeek})",
                        timestampLabel = "${day.dayOfWeek} · ${day.shortDate} (${day.dateIso})",
                        pointType = TimeMachinePointType.FORECAST,
                        temperatureC = day.maxTempC,
                        feelsLikeC = day.minTempC,
                        precipProbabilityPercent = day.precipProbabilityPercent,
                        precipitationMm = null,
                        windSpeedKmh = day.maxWindSpeedKmh,
                        humidityPercent = day.humidityAvgPercent,
                        uvIndex = day.uvIndexMax,
                        conditionText = "${day.conditionText} (High/Low)",
                        sceneType = day.sceneType,
                        localHourOfDay = 13,
                        dataSourceLabel = "Open-Meteo 7-Day Daily Forecast (Day +$dIdx)",
                        honestyNoteEn = "Multi-Day Forecast (+${dIdx}d · ${day.dateIso}): Shows projected daytime high (${day.maxTempC.roundToInt()}°C) and low (${day.minTempC.roundToInt()}°C). Forecast uncertainty increases with lead time.",
                        honestyNoteBn = "+${dIdx} দিনের পূর্বাভাস (${day.dateIso}): সময় বাড়ার সাথে সাথে পূর্বাভাসের অনিশ্চয়তা বৃদ্ধি পায়।"
                    )
                )
            }
        }

        // 6. Explicit Long-Range Unavailable Period (+14 Days)
        slots.add(
            TimeMachineSlot(
                id = "future_unavailable_14d",
                shortLabel = "+14 Days (Horizon N/A)",
                timestampLabel = "Beyond 7-Day Reliable Forecast Horizon",
                pointType = TimeMachinePointType.UNAVAILABLE_PERIOD,
                temperatureC = null,
                feelsLikeC = null,
                precipProbabilityPercent = null,
                precipitationMm = null,
                windSpeedKmh = null,
                humidityPercent = null,
                uvIndex = null,
                conditionText = "Beyond 7-Day Forecast Horizon (Unavailable)",
                sceneType = null,
                localHourOfDay = 12,
                dataSourceLabel = "Open-Meteo 7-Day Forecast Limit",
                honestyNoteEn = "Unavailable Period (+14 Days): Deterministic hourly/daily forecasts beyond 7 days are not provided because atmospheric chaos makes exact day-specific predictions unreliable.",
                honestyNoteBn = "+১৪ দিনের তথ্য অনুপলব্ধ: ৭ দিনের পরবর্তী সুনির্দিষ্ট পূর্বাভাস দেওয়া হয় না কারণ দীর্ঘমেয়াদী পূর্বাভাসে অনিশ্চয়তা অত্যন্ত বেশি।"
            )
        )

        return slots
    }

    private fun extractHourFromIso(iso: String, fallback: Int): Int {
        val tIndex = iso.indexOf('T')
        if (tIndex != -1 && iso.length >= tIndex + 3) {
            return iso.substring(tIndex + 1, tIndex + 3).toIntOrNull()?.coerceIn(0, 23) ?: fallback
        }
        return fallback
    }

    private fun formatIsoTimestampReadable(iso: String, suffix: String): String {
        if (iso.isBlank()) return suffix
        return "${iso.replace('T', ' ')} · $suffix"
    }

    // =========================================================================
    // 3. PERSONAL WEATHER TWIN: Local Preferences & Grounded Recommendations
    // =========================================================================

    fun defaultTwinPreferences(): List<TwinUserPreference> = listOf(
        TwinUserPreference(
            id = "twin_pref_photography",
            interest = TwinInterestCategory.OUTDOOR_PHOTOGRAPHY,
            customTitle = "Golden Hour & Atmospheric Shots",
            preferredWindow = "Golden Hour (16:30–18:30)",
            userNote = "Prefer dramatic clouds, clear horizon visibility, and low wind for tripod stability.",
            updatedAtLabel = "Default Profile"
        ),
        TwinUserPreference(
            id = "twin_pref_commuting",
            interest = TwinInterestCategory.COMMUTING,
            customTitle = "Daily City Transit & Umbrella Check",
            preferredWindow = "Commute Hours (08:00–10:00 & 17:00–20:00)",
            userNote = "Alert me when rain probability or gusty winds could slow down transit.",
            updatedAtLabel = "Default Profile"
        ),
        TwinUserPreference(
            id = "twin_pref_exercise",
            interest = TwinInterestCategory.EXERCISE,
            customTitle = "Outdoor Run & Fitness Session",
            preferredWindow = "Early Morning / Evening",
            userNote = "Check feels-like heat index, UV exposure, and US AQI before running.",
            updatedAtLabel = "Default Profile"
        )
    )

    private fun encodeField(raw: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray(Charsets.UTF_8))

    private fun decodeField(encoded: String): String =
        String(java.util.Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)

    fun encodePreferences(preferences: List<TwinUserPreference>): String {
        if (preferences.isEmpty()) return "EMPTY"
        return preferences.joinToString(separator = "\n") { pref ->
            listOf(
                encodeField(pref.id),
                encodeField(pref.interest.name),
                encodeField(pref.customTitle),
                encodeField(pref.preferredWindow),
                encodeField(pref.userNote),
                encodeField(pref.updatedAtLabel)
            ).joinToString(separator = "|")
        }
    }

    fun decodePreferences(jsonString: String?): List<TwinUserPreference> {
        if (jsonString.isNullOrBlank()) return defaultTwinPreferences()
        if (jsonString == "EMPTY") return emptyList()
        return try {
            val lines = jsonString.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return defaultTwinPreferences()
            val list = mutableListOf<TwinUserPreference>()
            lines.forEachIndexed { i, line ->
                val parts = line.split("|")
                if (parts.size >= 6) {
                    val id = decodeField(parts[0]).ifBlank { "twin_$i" }
                    val interestStr = decodeField(parts[1])
                    val interest = runCatching { TwinInterestCategory.valueOf(interestStr) }
                        .getOrDefault(TwinInterestCategory.COMMUTING)
                    val title = decodeField(parts[2]).ifBlank { interest.labelEn }
                    val window = decodeField(parts[3]).ifBlank { interest.defaultWindow }
                    val note = decodeField(parts[4])
                    val updated = decodeField(parts[5]).ifBlank { "Saved Locally" }
                    list.add(
                        TwinUserPreference(
                            id = id,
                            interest = interest,
                            customTitle = title,
                            preferredWindow = window,
                            userNote = note,
                            updatedAtLabel = updated
                        )
                    )
                }
            }
            if (list.isEmpty()) defaultTwinPreferences() else list
        } catch (_: Exception) {
            defaultTwinPreferences()
        }
    }

    /**
     * Generates personalized recommendations for every saved [TwinUserPreference] strictly grounded
     * in the active [WeatherBundle] telemetry. Never makes unsupported health/safety guarantees.
     */
    fun generateTwinRecommendations(
        preferences: List<TwinUserPreference>,
        location: SavedLocation,
        bundle: WeatherBundle?,
        settings: UserSettings
    ): List<TwinRecommendation> {
        if (preferences.isEmpty()) return emptyList()

        if (bundle == null) {
            return preferences.map { pref ->
                TwinRecommendation(
                    preference = pref,
                    suitabilityBadge = "Telemetry Unavailable",
                    headlineEn = "${pref.customTitle}: Awaiting Live Weather Sync",
                    headlineBn = "${pref.customTitle}: লাইভ আবহাওয়া তথ্যের অপেক্ষায়",
                    recommendationEn = "Live forecast data for ${location.cityName} is currently unavailable. Refresh weather data to generate grounded recommendations for your '${pref.preferredWindow}' window.",
                    recommendationBn = "${location.cityName}-এর লাইভ পূর্বাভাস বর্তমানে অনুপলব্ধ। আপনার '${pref.preferredWindow}' সময়ের পরামর্শ পেতে আবহাওয়া রিফ্রেশ করুন।",
                    forecastEvidence = "No live or cached API telemetry available for ${location.cityName}."
                )
            }
        }

        val cur = bundle.current
        val tempStr = WeatherFormatter.formatTemp(cur.temperatureC, settings.tempUnit)
        val feelsStr = WeatherFormatter.formatTemp(cur.feelsLikeC, settings.tempUnit)
        val windStr = WeatherFormatter.formatWind(cur.windSpeedKmh, settings.windUnit)
        val rainProb = cur.precipProbabilityPercent
        val rainStr = WeatherFormatter.formatNullablePercent(rainProb)
        val humStr = WeatherFormatter.formatNullablePercent(cur.humidityPercent)
        val visStr = WeatherFormatter.formatNullableVisibility(cur.visibilityKm)
        val uvStr = WeatherFormatter.formatNullableUv(cur.uvIndex)
        val aqiVal = bundle.airQuality?.usAqi
        val aqiStr = if (aqiVal != null) "$aqiVal (${WeatherFormatter.aqiCategoryLabel(aqiVal)})" else "Unavailable"
        val pm25Str = WeatherFormatter.formatNullablePollutant(bundle.airQuality?.pm25)

        val next12hMaxRain = bundle.hourly.take(12).mapNotNull { it.precipProbabilityPercent }.maxOrNull() ?: rainProb ?: 0
        val next12hMinWind = bundle.hourly.take(12).minByOrNull { it.windSpeedKmh }

        return preferences.map { pref ->
            val noteSuffix = pref.userNote.trim().takeIf { it.isNotEmpty() }?.let { " (Note: \"$it\")" } ?: ""
            when (pref.interest) {
                TwinInterestCategory.FARMING -> {
                    val favorable = next12hMaxRain < 50 && cur.windSpeedKmh < 32.0
                    val calmWindow = next12hMinWind?.let {
                        "${it.displayHour} (${WeatherFormatter.formatWind(it.windSpeedKmh, settings.windUnit)})"
                    } ?: "Current hour ($windStr)"
                    TwinRecommendation(
                        preference = pref,
                        suitabilityBadge = if (favorable) "Favorable Field Window" else "Rain / Wind Caution",
                        headlineEn = if (favorable) {
                            "Suitable window for ${pref.customTitle} around $calmWindow"
                        } else {
                            "Delay irrigation & open-air drying during elevated rain/wind"
                        },
                        headlineBn = if (favorable) {
                            "${pref.customTitle}-এর জন্য অনুকূল সময়: $calmWindow"
                        } else {
                            "বৃষ্টি বা বাতাসের কারণে সেচ ও ফসল শুকানোর কাজে সতর্কতা প্রয়োজন"
                        },
                        recommendationEn = if (favorable) {
                            "For your ${pref.preferredWindow} preference$noteSuffix: 12h peak rain probability is $next12hMaxRain% with current wind at $windStr and humidity at $humStr. Lowest wind window is $calmWindow for field inspection or drying."
                        } else {
                            "For your ${pref.preferredWindow} preference$noteSuffix: 12h rain probability reaches $next12hMaxRain% (current wind $windStr, humidity $humStr). Hold supplemental irrigation and protect harvested produce from moisture."
                        },
                        recommendationBn = "আপনার '${pref.preferredWindow}' পছন্দ অনুযায়ী পরবর্তী ১২ ঘণ্টায় সর্বোচ্চ বৃষ্টির সম্ভাবনা $next12hMaxRain%, বর্তমান বাতাস $windStr এবং আর্দ্রতা $humStr। সবচেয়ে কম বাতাসের সময়: $calmWindow।",
                        forecastEvidence = "${location.cityName} Telemetry · Temp: $tempStr · Rain Prob (now/12h): $rainStr / $next12hMaxRain% · Humidity: $humStr · Wind: $windStr · Sunrise/Sunset: ${cur.sunriseTime} / ${cur.sunsetTime}"
                    )
                }

                TwinInterestCategory.OUTDOOR_PHOTOGRAPHY -> {
                    val goodLight = (rainProb ?: 0) < 45 && (cur.visibilityKm ?: 10.0) >= 5.0
                    TwinRecommendation(
                        preference = pref,
                        suitabilityBadge = if (goodLight) "Prime Golden Hour" else "Atmospheric / Moody Sky",
                        headlineEn = if (goodLight) {
                            "Clear horizon & ${cur.conditionText} lighting for ${pref.customTitle}"
                        } else {
                            "Moody cloud/rain reflections expected — bring weather sealing"
                        },
                        headlineBn = if (goodLight) {
                            "${pref.customTitle}-এর জন্য চমৎকার আলো ও দৃশ্যমানতা ($visStr)"
                        } else {
                            "মেঘলা বা ভেজা আবহাওয়ার ফটোগ্রাফি — ক্যামেরা সুরক্ষার ব্যবস্থা রাখুন"
                        },
                        recommendationEn = "Target window: ${pref.preferredWindow}$noteSuffix. Sunrise is at ${cur.sunriseTime} and Sunset is at ${cur.sunsetTime}. Current condition is ${cur.conditionText} with visibility $visStr and wind $windStr (${if (cur.windSpeedKmh < 22) "stable for long-exposure tripod shots" else "breezy — weight down your tripod"}).",
                        recommendationBn = "সূর্যোদয় ${cur.sunriseTime} এবং সূর্যাস্ত ${cur.sunsetTime}। বর্তমান দৃশ্যমানতা $visStr, আকাশের অবস্থা '${cur.conditionText}' এবং বাতাসের গতি $windStr।",
                        forecastEvidence = "${location.cityName} Telemetry · Condition: ${cur.conditionText} · Visibility: $visStr · Sunset: ${cur.sunsetTime} · Rain Prob: $rainStr · Wind: $windStr"
                    )
                }

                TwinInterestCategory.COMMUTING -> {
                    val needsUmbrella = next12hMaxRain >= 40 || cur.conditionText.contains("Rain", ignoreCase = true)
                    TwinRecommendation(
                        preference = pref,
                        suitabilityBadge = if (needsUmbrella) "Carry Umbrella / Rain Gear" else "Smooth Transit Outlook",
                        headlineEn = if (needsUmbrella) {
                            "Carry an umbrella for ${pref.customTitle} ($next12hMaxRain% 12h rain chance)"
                        } else {
                            "Low rain risk ($next12hMaxRain%) for your ${pref.preferredWindow} commute"
                        },
                        headlineBn = if (needsUmbrella) {
                            "যাতায়াতের সময় ছাতা সাথে রাখুন (১২ ঘণ্টায় বৃষ্টির সম্ভাবনা $next12hMaxRain%)"
                        } else {
                            "আপনার '${pref.preferredWindow}' যাতায়াতে বৃষ্টির ঝুঁকি কম ($next12hMaxRain%)"
                        },
                        recommendationEn = "For ${pref.preferredWindow}$noteSuffix: Current temperature is $tempStr (feels like $feelsStr), road visibility is $visStr, wind is $windStr, and 12-hour peak rain probability is $next12hMaxRain%. ${if (needsUmbrella) "Allow extra transit buffer time for wet roads." else "Standard travel times expected."}",
                        recommendationBn = "বর্তমান তাপমাত্রা $tempStr (অনুভূত $feelsStr), দৃশ্যমানতা $visStr, বাতাসের গতি $windStr এবং পরবর্তী ১২ ঘণ্টায় বৃষ্টির সম্ভাবনা $next12hMaxRain%।",
                        forecastEvidence = "${location.cityName} Telemetry · Rain Prob (now/12h): $rainStr / $next12hMaxRain% · Visibility: $visStr · Wind: $windStr · Feels Like: $feelsStr"
                    )
                }

                TwinInterestCategory.EXERCISE -> {
                    val heatOrAirCaution = cur.feelsLikeC >= 33.0 || (aqiVal != null && aqiVal > 100) || (cur.uvIndex ?: 0.0) >= 7.0
                    TwinRecommendation(
                        preference = pref,
                        suitabilityBadge = if (heatOrAirCaution) "Pacing & Hydration Advisory" else "Comfortable Outdoor Window",
                        headlineEn = if (heatOrAirCaution) {
                            "Adjust intensity for ${pref.customTitle} (Feels like $feelsStr · US AQI $aqiStr)"
                        } else {
                            "Favorable conditions for ${pref.customTitle} in ${pref.preferredWindow}"
                        },
                        headlineBn = if (heatOrAirCaution) {
                            "অতিরিক্ত তাপমাত্রা ($feelsStr) বা বায়ুর মান ($aqiStr) বিবেচনায় হালকা ব্যায়াম ও পর্যাপ্ত পানি পান করুন"
                        } else {
                            "'${pref.preferredWindow}' সময়ে বাইরে ব্যায়ামের জন্য অনুকূল আবহাওয়া"
                        },
                        recommendationEn = "For ${pref.preferredWindow}$noteSuffix: Feels-like temperature is $feelsStr (humidity $humStr), UV index is $uvStr, and US AQI is $aqiStr (PM2.5 $pm25Str). ${if (heatOrAirCaution) "Consider shaded routes, early/late cooler hours, and extra hydration." else "Conditions are well within comfortable outdoor exercise ranges."}",
                        recommendationBn = "অনুভূত তাপমাত্রা $feelsStr (আর্দ্রতা $humStr), ইউভি ইনডেক্স $uvStr এবং US AQI $aqiStr (PM2.5 $pm25Str)।",
                        forecastEvidence = "${location.cityName} Telemetry · Feels Like: $feelsStr · Humidity: $humStr · UV Index: $uvStr · US AQI: $aqiStr · PM2.5: $pm25Str · Wind: $windStr"
                    )
                }

                TwinInterestCategory.TRAVEL -> {
                    val tomorrow = bundle.daily.getOrNull(1) ?: bundle.daily.firstOrNull()
                    val tmrwSummary = tomorrow?.let {
                        "${it.dayOfWeek} (${it.shortDate}): ${it.conditionText}, High ${WeatherFormatter.formatTemp(it.maxTempC, settings.tempUnit)} / Low ${WeatherFormatter.formatTemp(it.minTempC, settings.tempUnit)}, Rain ${WeatherFormatter.formatNullablePercent(it.precipProbabilityPercent)}"
                    } ?: "Multi-day outlook unavailable"
                    TwinRecommendation(
                        preference = pref,
                        suitabilityBadge = if ((tomorrow?.precipProbabilityPercent ?: 0) >= 50) "Pack Rain Layer" else "Favorable Sightseeing",
                        headlineEn = "${pref.customTitle} Outlook for ${location.cityName}",
                        headlineBn = "${location.cityName}-এ ভ্রমণ ও আউটিং পূর্বাভাস",
                        recommendationEn = "For ${pref.preferredWindow}$noteSuffix: Current weather is ${cur.conditionText} at $tempStr. Next-day outlook — $tmrwSummary. Pack layers suited for ${WeatherFormatter.formatTemp(cur.lowC, settings.tempUnit)} to ${WeatherFormatter.formatTemp(cur.highC, settings.tempUnit)}.",
                        recommendationBn = "বর্তমান আবহাওয়া '${cur.conditionText}' ($tempStr)। আগামীকালের পূর্বাভাস: $tmrwSummary।",
                        forecastEvidence = "${location.cityName} Telemetry · Today High/Low: ${WeatherFormatter.formatTemp(cur.highC, settings.tempUnit)} / ${WeatherFormatter.formatTemp(cur.lowC, settings.tempUnit)} · Next Day: $tmrwSummary"
                    )
                }
            }
        }
    }
}
