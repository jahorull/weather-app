package com.example

import com.example.model.TempUnit
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.example.model.WindUnit
import org.junit.Assert.assertEquals
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
    fun uvAndComfortLabels_classifyCorrectly() {
        assertEquals("Low", WeatherFormatter.uvCategoryLabel(2.0))
        assertEquals("Moderate", WeatherFormatter.uvCategoryLabel(4.5))
        assertEquals("High", WeatherFormatter.uvCategoryLabel(7.0))
        assertEquals("Very High", WeatherFormatter.uvCategoryLabel(9.5))
        assertEquals("Extreme", WeatherFormatter.uvCategoryLabel(11.5))

        assertTrue(WeatherFormatter.formatRelativeUpdateTime(1000L, 5000L).contains("just now"))
    }
}
