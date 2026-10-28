package com.example

import com.example.ai.AtmosAiIntelligence
import com.example.data.remote.AirQualityCurrentDto
import com.example.data.remote.CurrentDto
import com.example.data.remote.OpenMeteoAirQualityResponse
import com.example.data.remote.OpenMeteoForecastResponse
import com.example.data.remote.WeatherApiMapper
import com.example.model.DefaultGlobalCities
import com.example.model.TempUnit
import com.example.model.UserSettings
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.example.model.WindUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun temperatureConversion_celsiusAndFahrenheit_isAccurate() {
        assertEquals(28, WeatherFormatter.convertTemp(28.0, TempUnit.CELSIUS))
        assertEquals(82, WeatherFormatter.convertTemp(28.0, TempUnit.FAHRENHEIT))
        assertEquals("28°C", WeatherFormatter.formatTemp(28.0, TempUnit.CELSIUS))
        assertEquals("82°F", WeatherFormatter.formatTemp(28.0, TempUnit.FAHRENHEIT))
        assertEquals(32, WeatherFormatter.convertTemp(0.0, TempUnit.FAHRENHEIT))
    }

    @Test
    fun windSpeedAndCardinalDirection_conversion_isAccurate() {
        assertEquals("36 km/h", WeatherFormatter.formatWind(36.0, WindUnit.KMH))
        assertEquals("10 m/s", WeatherFormatter.formatWind(36.0, WindUnit.MS))
        assertEquals("22 mph", WeatherFormatter.formatWind(36.0, WindUnit.MPH))

        assertEquals("N", WeatherFormatter.windDirectionCardinal(0))
        assertEquals("E", WeatherFormatter.windDirectionCardinal(90))
        assertEquals("S", WeatherFormatter.windDirectionCardinal(180))
        assertEquals("W", WeatherFormatter.windDirectionCardinal(270))
    }

    @Test
    fun nullVsZeroDistinction_formatsCorrectly() {
        assertEquals("Unavailable", WeatherFormatter.formatNullablePercent(null))
        assertEquals("0%", WeatherFormatter.formatNullablePercent(0))

        assertEquals("Unavailable", WeatherFormatter.formatNullableUv(null))
        assertEquals("0.0 · Low", WeatherFormatter.formatNullableUv(0.0))

        assertEquals("Unavailable", WeatherFormatter.formatNullableVisibility(null))
        assertEquals("0.0 km", WeatherFormatter.formatNullableVisibility(0.0))

        assertEquals("Unavailable", WeatherFormatter.aqiCategoryLabel(null))
        assertEquals("Good", WeatherFormatter.aqiCategoryLabel(0))
        assertEquals("Moderate", WeatherFormatter.aqiCategoryLabel(75))
        assertEquals("Unhealthy", WeatherFormatter.aqiCategoryLabel(165))
    }

    @Test
    fun wmoWeatherCodeTo3DSceneMapping_coversAllEightEnvironments() {
        assertEquals(WeatherSceneType.SUNNY, WeatherSceneType.fromWmoCode(0, isDay = true, hourOfDay = 12))
        assertEquals(WeatherSceneType.SUNSET, WeatherSceneType.fromWmoCode(0, isDay = true, hourOfDay = 18))
        assertEquals(WeatherSceneType.NIGHT, WeatherSceneType.fromWmoCode(0, isDay = false, hourOfDay = 23))
        assertEquals(WeatherSceneType.CLOUDY, WeatherSceneType.fromWmoCode(2, isDay = true, hourOfDay = 14))
        assertEquals(WeatherSceneType.FOGGY, WeatherSceneType.fromWmoCode(45, isDay = true, hourOfDay = 9))
        assertEquals(WeatherSceneType.RAINY, WeatherSceneType.fromWmoCode(63, isDay = true, hourOfDay = 15))
        assertEquals(WeatherSceneType.SNOWY, WeatherSceneType.fromWmoCode(73, isDay = true, hourOfDay = 11))
        assertEquals(WeatherSceneType.THUNDERSTORM, WeatherSceneType.fromWmoCode(95, isDay = false, hourOfDay = 21))
    }

    @Test
    fun weatherAlerts_neverInventedWhenNormal_andGeneratedForSevereThresholds() {
        val dhaka = DefaultGlobalCities.hubs.first()
        val calmBundle = WeatherApiMapper.mapToWeatherBundle(
            location = dhaka,
            response = OpenMeteoForecastResponse(
                current = CurrentDto(
                    time = "2026-10-28T12:00",
                    temperature2m = 26.0,
                    weatherCode = 1,
                    windSpeed10m = 12.0,
                    uvIndex = 4.0
                )
            ),
            airQualityResponse = OpenMeteoAirQualityResponse(
                current = AirQualityCurrentDto(usAqi = 42, pm25 = 9.5, pm10 = 18.0)
            )
        )
        assertTrue(calmBundle.alerts.isEmpty())
        assertEquals(42, calmBundle.airQuality?.usAqi)

        val stormBundle = WeatherApiMapper.mapToWeatherBundle(
            location = dhaka,
            response = OpenMeteoForecastResponse(
                current = CurrentDto(
                    time = "2026-10-28T15:00",
                    temperature2m = 24.0,
                    weatherCode = 95,
                    windSpeed10m = 62.0,
                    uvIndex = 9.0
                )
            )
        )
        assertTrue(stormBundle.alerts.size >= 2)
    }

    @Test
    fun ai3DSceneController_mapsNaturalLanguageCommandsToValidatedPresets() {
        assertEquals(WeatherSceneType.RAINY, AtmosAiIntelligence.matchLocalSceneCommand("Show a rainy night."))
        assertEquals(WeatherSceneType.SUNSET, AtmosAiIntelligence.matchLocalSceneCommand("Create a golden sunset."))
        assertEquals(WeatherSceneType.FOGGY, AtmosAiIntelligence.matchLocalSceneCommand("Show a foggy morning."))
        assertEquals(WeatherSceneType.SUNNY, AtmosAiIntelligence.matchLocalSceneCommand("Switch to a sunny scene."))
        assertEquals(WeatherSceneType.THUNDERSTORM, AtmosAiIntelligence.matchLocalSceneCommand("Show a thunderstorm with lightning"))
        assertEquals(WeatherSceneType.SNOWY, AtmosAiIntelligence.matchLocalSceneCommand("Show snowfall blizzard"))
        assertEquals(WeatherSceneType.NIGHT, AtmosAiIntelligence.matchLocalSceneCommand("Starlit night sky"))
        assertEquals(WeatherSceneType.CLOUDY, AtmosAiIntelligence.matchLocalSceneCommand("Partly cloudy sky"))
        // Bengali scene commands
        assertEquals(WeatherSceneType.RAINY, AtmosAiIntelligence.matchLocalSceneCommand("বৃষ্টির দৃশ্য দেখাও"))
        assertEquals(WeatherSceneType.SUNSET, AtmosAiIntelligence.matchLocalSceneCommand("সোনালী সূর্যাস্ত দেখাও"))
    }

    @Test
    fun aiDailyLifeAdvisor_andTravelHorizon_groundStrictlyInWeatherData() {
        val dhaka = DefaultGlobalCities.hubs.first()
        val tokyo = DefaultGlobalCities.hubs[2]
        val settings = UserSettings()

        val bundle = WeatherApiMapper.mapToWeatherBundle(
            location = dhaka,
            response = OpenMeteoForecastResponse(
                current = CurrentDto(
                    time = "2026-10-28T14:00",
                    temperature2m = 32.0,
                    apparentTemperature = 36.0,
                    relativeHumidity2m = 78,
                    weatherCode = 61,
                    windSpeed10m = 18.0,
                    uvIndex = 7.5
                ),
                hourly = com.example.data.remote.HourlyDto(
                    time = listOf("2026-10-28T14:00"),
                    temperature2m = listOf(32.0),
                    apparentTemperature = listOf(36.0),
                    precipitationProbability = listOf(65),
                    weatherCode = listOf(61),
                    windSpeed10m = listOf(18.0)
                )
            ),
            airQualityResponse = OpenMeteoAirQualityResponse(
                current = AirQualityCurrentDto(usAqi = 88, pm25 = 24.5, pm10 = 42.0)
            )
        )

        val advisorItems = AtmosAiIntelligence.buildDeterministicDailyAdvisor(bundle, settings)
        assertEquals(5, advisorItems.size)
        assertTrue(advisorItems.all { it.badgeLabel.contains("Recommendation") })
        assertTrue(advisorItems.first().weatherReason.contains("65%"))

        val contextText = AtmosAiIntelligence.buildGroundedWeatherContext(dhaka, bundle, settings)
        assertTrue(contextText.contains("Dhaka"))
        assertTrue(contextText.contains("US AQI=88"))

        // Travel Planner horizon validation (Day +8 is beyond 7-day reliable window)
        val withinHorizon = AtmosAiIntelligence.buildTravelComparisonSummary(
            originLocation = dhaka,
            originBundle = bundle,
            destinationLocation = tokyo,
            destinationBundle = null,
            dayOffset = 2,
            settings = settings
        )
        assertFalse(withinHorizon.isBeyondReliableHorizon)

        val beyondHorizon = AtmosAiIntelligence.buildTravelComparisonSummary(
            originLocation = dhaka,
            originBundle = bundle,
            destinationLocation = tokyo,
            destinationBundle = bundle,
            dayOffset = 8,
            settings = settings
        )
        assertTrue(beyondHorizon.isBeyondReliableHorizon)
        assertTrue(beyondHorizon.deterministicSummary.contains("beyond the 7-day reliable meteorological forecast window"))
    }

    @Test
    fun radarTimeline_andMultiModelConfidence_separateObservationsAndNeverInventPercentages() {
        val dhaka = DefaultGlobalCities.hubs.first()
        val primaryResponse = OpenMeteoForecastResponse(
            current = CurrentDto(
                time = "2026-10-28T14:00",
                temperature2m = 30.0,
                apparentTemperature = 33.0,
                weatherCode = 61,
                windSpeed10m = 16.0
            ),
            hourly = com.example.data.remote.HourlyDto(
                time = listOf(
                    "2026-10-28T12:00",
                    "2026-10-28T13:00",
                    "2026-10-28T14:00",
                    "2026-10-28T15:00"
                ),
                temperature2m = listOf(29.0, 29.5, 30.0, 30.5),
                apparentTemperature = listOf(31.0, 32.0, 33.0, 33.5),
                precipitationProbability = listOf(20, 35, 50, 70),
                precipitation = listOf(0.0, 0.4, 1.2, 2.8),
                weatherCode = listOf(2, 61, 61, 63),
                windSpeed10m = listOf(12.0, 14.0, 16.0, 20.0),
                windDirection10m = listOf(170, 175, 180, 185)
            ),
            daily = com.example.data.remote.DailyDto(
                time = listOf("2026-10-28", "2026-10-29"),
                weatherCode = listOf(61, 3),
                temperature2mMax = listOf(31.0, 30.0),
                temperature2mMin = listOf(24.0, 23.5),
                precipitationProbabilityMax = listOf(60, 30),
                windSpeed10mMax = listOf(20.0, 15.0)
            )
        )

        val ecmwfResponse = OpenMeteoForecastResponse(
            daily = com.example.data.remote.DailyDto(
                time = listOf("2026-10-28", "2026-10-29"),
                weatherCode = listOf(61, 3),
                temperature2mMax = listOf(31.5, 30.2),
                temperature2mMin = listOf(24.2, 23.8),
                precipitationProbabilityMax = listOf(65, 35),
                windSpeed10mMax = listOf(22.0, 16.0)
            )
        )

        val gfsResponse = OpenMeteoForecastResponse(
            daily = com.example.data.remote.DailyDto(
                time = listOf("2026-10-28", "2026-10-29"),
                weatherCode = listOf(80, 2),
                temperature2mMax = listOf(35.0, 30.5),
                temperature2mMin = listOf(25.0, 24.0),
                precipitationProbabilityMax = listOf(20, 25),
                windSpeed10mMax = listOf(38.0, 18.0)
            )
        )

        val bundle = WeatherApiMapper.mapToWeatherBundle(
            location = dhaka,
            response = primaryResponse,
            multiModelResponses = mapOf(
                "ECMWF IFS 0.25°" to ecmwfResponse,
                "NOAA GFS Seamless" to gfsResponse
            )
        )

        val radar = bundle.radarTimeline!!
        assertEquals(3, radar.frames.count { it.isHistoricalObservation })
        assertEquals(1, radar.frames.count { !it.isHistoricalObservation })
        assertTrue(radar.frames[0].isHistoricalObservation)
        assertTrue(radar.frames[1].isHistoricalObservation)
        assertTrue(radar.frames[2].isHistoricalObservation)
        assertFalse(radar.frames[3].isHistoricalObservation)

        val confidence = bundle.confidenceReport!!
        assertEquals(2, confidence.dailyComparisons.size)
        assertTrue(confidence.observedSummaryEn.contains("Current Observation"))
        assertTrue(confidence.observedSummaryBn.contains("বর্তমান পর্যবেক্ষণ"))

        // Day 0 has large spread between ECMWF (65% rain, 31.5°C) and GFS (20% rain, 35.0°C) -> LOW
        val day0 = confidence.dailyComparisons[0]
        assertEquals(3, day0.models.size)
        assertEquals(com.example.model.AgreementLevel.LOW, day0.agreementLevel)
        assertEquals(45, day0.precipSpreadPercent)
        assertFalse(day0.explanationEn.contains("% confidence"))

        // Day 1 has tight spread (25-35% rain, 30.0-30.5°C) -> HIGH
        val day1 = confidence.dailyComparisons[1]
        assertEquals(com.example.model.AgreementLevel.HIGH, day1.agreementLevel)
    }

    @Test
    fun bangladeshSmartFarming_andPersonalizedThresholdAlerts_areGroundedAndSafe() {
        val rajshahi = DefaultGlobalCities.bangladeshFarmingHubs[1]
        val settings = UserSettings(
            personalizedAlertsEnabled = true,
            rainAlertThresholdPercent = 60,
            tempAlertThresholdC = 34,
            windAlertThresholdKmh = 35,
            aqiAlertThresholdUs = 100
        )

        val bundle = WeatherApiMapper.mapToWeatherBundle(
            location = rajshahi,
            response = OpenMeteoForecastResponse(
                current = CurrentDto(
                    time = "2026-10-28T14:00",
                    temperature2m = 35.5,
                    apparentTemperature = 39.0,
                    relativeHumidity2m = 80,
                    weatherCode = 63,
                    windSpeed10m = 42.0,
                    uvIndex = 7.0
                ),
                hourly = com.example.data.remote.HourlyDto(
                    time = listOf("2026-10-28T14:00", "2026-10-28T15:00"),
                    temperature2m = listOf(35.5, 34.0),
                    apparentTemperature = listOf(39.0, 37.5),
                    precipitationProbability = listOf(75, 80),
                    precipitation = listOf(3.5, 4.2),
                    weatherCode = listOf(63, 63),
                    windSpeed10m = listOf(42.0, 38.0)
                )
            ),
            airQualityResponse = OpenMeteoAirQualityResponse(
                current = AirQualityCurrentDto(usAqi = 125, pm25 = 45.0, pm10 = 78.0)
            )
        )

        val potatoAdvisory = com.example.ai.SmartFarmingAndAlertsEngine.buildSmartFarmingAdvisory(
            location = rajshahi,
            bundle = bundle,
            crop = com.example.model.BangladeshCrop.POTATO,
            settings = settings
        )
        assertTrue(potatoAdvisory.isDataAvailable)
        assertTrue(potatoAdvisory.rainfallGuidanceEn.contains("80%"))
        assertTrue(potatoAdvisory.irrigationGuidanceEn.contains("Postpone supplemental irrigation"))
        assertTrue(potatoAdvisory.expertGuidanceNoticeEn.contains("never prescribes pesticide"))
        assertTrue(potatoAdvisory.expertGuidanceNoticeBn.contains("১৬১২৩"))

        val triggeredAlerts = com.example.ai.SmartFarmingAndAlertsEngine.evaluatePersonalThresholdAlerts(
            activeBundle = bundle,
            settings = settings
        )
        // All 4 thresholds (Rain >= 60%, Temp >= 34°C, Wind >= 35 km/h, AQI >= 100) should trigger
        assertEquals(4, triggeredAlerts.size)
        assertTrue(triggeredAlerts.all { it.sourceBadge.contains("Not an Official Government Warning") })

        val disabledAlerts = com.example.ai.SmartFarmingAndAlertsEngine.evaluatePersonalThresholdAlerts(
            activeBundle = bundle,
            settings = settings.copy(personalizedAlertsEnabled = false)
        )
        assertTrue(disabledAlerts.isEmpty())
    }

    @Test
    fun whatIfLab_mapsNaturalLanguageCommandsToValidatedParameters_andLabelsHypotheticalSimulation() {
        val stormySunset = com.example.ai.AtmosUniverseEngine.parseWhatIfCommandLocally("Create a stormy sunset scene.")!!
        assertTrue(stormySunset.isActive)
        assertEquals(WeatherSceneType.SUNSET, stormySunset.derivedSceneType)
        assertTrue(stormySunset.windStrength >= 0.70f)
        assertTrue(stormySunset.rainIntensity >= 0.50f)
        assertEquals(18.0f, stormySunset.timeOfDayHour, 0.1f)
        assertTrue(stormySunset.simulationExplanation.contains("HYPOTHETICAL SIMULATION"))
        assertTrue(stormySunset.simulationExplanation.contains("does not alter or predict real weather"))

        val foggyMorning = com.example.ai.AtmosUniverseEngine.parseWhatIfCommandLocally("Dense foggy morning with light rain.")!!
        assertTrue(foggyMorning.isActive)
        assertTrue(foggyMorning.fogDensity >= 0.70f)
        assertTrue(foggyMorning.rainIntensity > 0.20f)
        assertEquals(6.5f, foggyMorning.timeOfDayHour, 0.1f)
    }

    @Test
    fun weatherTimeMachine_separatesHistoricalCurrentForecastAndUnavailablePeriods() {
        val dhaka = DefaultGlobalCities.hubs.first()

        // 1. Offline bundle -> returns explicit UNAVAILABLE_PERIOD without inventing data
        val offlineSlots = com.example.ai.AtmosUniverseEngine.buildTimeMachineSlots(dhaka, null)
        assertEquals(1, offlineSlots.size)
        assertEquals(com.example.model.TimeMachinePointType.UNAVAILABLE_PERIOD, offlineSlots.first().pointType)
        assertEquals(null, offlineSlots.first().temperatureC)

        // 2. Live bundle with pastHourly, current, hourly, and daily
        val bundle = WeatherApiMapper.mapToWeatherBundle(
            location = dhaka,
            response = OpenMeteoForecastResponse(
                current = CurrentDto(
                    time = "2026-10-28T14:00",
                    temperature2m = 30.0,
                    apparentTemperature = 33.0,
                    weatherCode = 1,
                    windSpeed10m = 15.0
                ),
                hourly = com.example.data.remote.HourlyDto(
                    time = listOf(
                        "2026-10-28T11:00",
                        "2026-10-28T12:00",
                        "2026-10-28T13:00",
                        "2026-10-28T14:00",
                        "2026-10-28T15:00",
                        "2026-10-28T16:00",
                        "2026-10-28T17:00",
                        "2026-10-28T18:00"
                    ),
                    temperature2m = listOf(27.0, 28.0, 29.0, 30.0, 30.5, 29.5, 28.0, 27.0),
                    apparentTemperature = listOf(29.0, 30.0, 31.0, 33.0, 33.5, 32.0, 30.0, 29.0),
                    precipitationProbability = listOf(10, 15, 20, 25, 30, 40, 50, 60),
                    weatherCode = listOf(1, 1, 2, 1, 2, 61, 61, 63),
                    windSpeed10m = listOf(10.0, 12.0, 14.0, 15.0, 16.0, 18.0, 20.0, 22.0)
                ),
                daily = com.example.data.remote.DailyDto(
                    time = listOf("2026-10-28", "2026-10-29", "2026-10-30", "2026-10-31"),
                    weatherCode = listOf(1, 61, 2, 0),
                    temperature2mMax = listOf(31.0, 29.0, 30.0, 32.0),
                    temperature2mMin = listOf(24.0, 23.0, 23.5, 24.5),
                    precipitationProbabilityMax = listOf(30, 70, 20, 10),
                    windSpeed10mMax = listOf(18.0, 25.0, 15.0, 12.0)
                )
            )
        )

        assertEquals(3, bundle.pastHourly.size)
        assertTrue(bundle.pastHourly.all { it.isHistoricalObservation })

        val slots = com.example.ai.AtmosUniverseEngine.buildTimeMachineSlots(dhaka, bundle)
        assertTrue(slots.any { it.pointType == com.example.model.TimeMachinePointType.HISTORICAL_OBSERVATION })
        assertEquals(1, slots.count { it.pointType == com.example.model.TimeMachinePointType.CURRENT_OBSERVATION })
        assertTrue(slots.any { it.pointType == com.example.model.TimeMachinePointType.FORECAST })
        assertEquals(2, slots.count { it.pointType == com.example.model.TimeMachinePointType.UNAVAILABLE_PERIOD })

        // Unavailable periods must never fabricate temperatures
        slots.filter { it.pointType == com.example.model.TimeMachinePointType.UNAVAILABLE_PERIOD }.forEach { unavail ->
            assertEquals(null, unavail.temperatureC)
            assertTrue(unavail.honestyNoteEn.contains("Unavailable Period"))
        }
    }

    @Test
    fun personalWeatherTwin_encodesDecodesAndGeneratesForecastGroundedRecommendations() {
        val dhaka = DefaultGlobalCities.hubs.first()
        val settings = UserSettings()
        val prefs = com.example.ai.AtmosUniverseEngine.defaultTwinPreferences()
        assertEquals(3, prefs.size)

        val encoded = com.example.ai.AtmosUniverseEngine.encodePreferences(prefs)
        val decoded = com.example.ai.AtmosUniverseEngine.decodePreferences(encoded)
        assertEquals(3, decoded.size)
        assertEquals(prefs.first().customTitle, decoded.first().customTitle)

        val bundle = WeatherApiMapper.mapToWeatherBundle(
            location = dhaka,
            response = OpenMeteoForecastResponse(
                current = CurrentDto(
                    time = "2026-10-28T16:00",
                    temperature2m = 31.0,
                    apparentTemperature = 34.0,
                    relativeHumidity2m = 72,
                    weatherCode = 2,
                    windSpeed10m = 14.0,
                    visibilityMeters = 9000.0,
                    uvIndex = 5.0
                ),
                hourly = com.example.data.remote.HourlyDto(
                    time = listOf("2026-10-28T16:00"),
                    temperature2m = listOf(31.0),
                    apparentTemperature = listOf(34.0),
                    precipitationProbability = listOf(35),
                    weatherCode = listOf(2),
                    windSpeed10m = listOf(14.0)
                )
            ),
            airQualityResponse = OpenMeteoAirQualityResponse(
                current = AirQualityCurrentDto(usAqi = 68, pm25 = 18.0, pm10 = 32.0)
            )
        )

        val recs = com.example.ai.AtmosUniverseEngine.generateTwinRecommendations(
            preferences = decoded,
            location = dhaka,
            bundle = bundle,
            settings = settings
        )
        assertEquals(3, recs.size)
        assertTrue(recs.all { it.forecastEvidence.contains("Dhaka Telemetry") })
        assertTrue(recs.all { it.safetyBoundaryNote.contains("not a health, medical, or safety guarantee") })
    }
}

