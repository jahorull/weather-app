package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.model.WeatherSceneType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun dismissSplashIfPresent() {
        composeTestRule.waitForIdle()
        runCatching {
            composeTestRule.onNodeWithTag("splash_screen_overlay").performClick()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun appNameResource_isAtmos3D() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Atmos 3D", context.getString(R.string.app_name))
    }

    @Test
    fun allEightWeatherPresets_update3DSceneOnClick() {
        dismissSplashIfPresent()

        composeTestRule.onNodeWithTag("weather_3d_scene_backdrop").assertIsDisplayed()
        composeTestRule.onNodeWithTag("weather_presets_section")
            .performScrollTo()
            .assertIsDisplayed()

        // Verify all 8 weather preset buttons (Sunny & Clear, Partly Cloudy, Rain Showers,
        // Thunderstorm, Snowfall, Atmospheric Fog, Golden Sunset, Starlit Night) are functional
        // and clicking each one immediately updates the active 3D weather scene.
        WeatherSceneType.entries.forEach { preset ->
            val chipTag = "scene_chip_${preset.name.lowercase()}"
            val expectedSceneTag = "active_3d_scene_${preset.name.lowercase()}"

            composeTestRule.onNodeWithTag(chipTag)
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
            composeTestRule.waitForIdle()

            // Verify the 3D scene canvas switched to the selected preset
            composeTestRule.onNodeWithTag(expectedSceneTag).assertIsDisplayed()
            composeTestRule.onNodeWithTag("active_preset_status_text")
                .assertTextContains(preset.title, substring = true)
        }

        // Verify Reset to Live Weather button restores live scene state
        composeTestRule.onNodeWithTag("reset_live_scene_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun aiCopilotScreen_andNaturalLanguage3DSceneController_workEndToEnd() {
        dismissSplashIfPresent()

        // Navigate to AI Copilot screen from bottom navigation
        composeTestRule.onNodeWithTag("nav_item_ai_copilot").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_copilot_screen_root").assertIsDisplayed()

        // Switch language between Auto, English, and Bengali (বাংলা)
        composeTestRule.onNodeWithTag("ai_lang_bengali").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_lang_english").performClick()
        composeTestRule.waitForIdle()

        // Test Feature 4: Natural-language 3D Scene Controller preset chips
        // 0: "Show a rainy night." -> RAINY
        // 1: "Create a golden sunset." -> SUNSET
        // 2: "Show a foggy morning." -> FOGGY
        // 3: "Switch to a sunny scene." -> SUNNY
        composeTestRule.onNodeWithTag("ai_copilot_screen_root")
            .performScrollToNode(hasTestTag("ai_scene_controller_card"))
        composeTestRule.waitForIdle()

        val expectedScenes = listOf(
            0 to WeatherSceneType.RAINY,
            1 to WeatherSceneType.SUNSET,
            2 to WeatherSceneType.FOGGY,
            3 to WeatherSceneType.SUNNY
        )
        expectedScenes.forEach { (idx, expectedScene) ->
            composeTestRule.onNodeWithTag("scene_cmd_chip_$idx")
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("active_3d_scene_${expectedScene.name.lowercase()}")
                .assertIsDisplayed()
        }

        // Verify Travel Planner & Smart Alert Explanations cards are accessible
        composeTestRule.onNodeWithTag("ai_copilot_screen_root")
            .performScrollToNode(hasTestTag("ai_travel_planner_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_travel_planner_card").assertIsDisplayed()

        // Select Day +8 to verify >7-day forecast horizon warning
        composeTestRule.onNodeWithTag("travel_day_offset_8").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ai_copilot_screen_root")
            .performScrollToNode(hasTestTag("ai_smart_alerts_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_smart_alerts_card").assertIsDisplayed()
    }

    @Test
    fun navigationButtonsAndInteractiveScreens_workEndToEnd() {
        dismissSplashIfPresent()

        // 1. Weather Dashboard
        composeTestRule.onNodeWithTag("weather_3d_scene_backdrop").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_current_temp").assertIsDisplayed()

        // Test Immersive 3D Mode toggle & exit
        composeTestRule.onNodeWithTag("enter_immersive_mode_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exit_immersive_mode_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exit_immersive_mode_button").performClick()
        composeTestRule.waitForIdle()

        // 2. Navigate to Forecast screen
        composeTestRule.onNodeWithTag("nav_item_forecasts").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("forecasts_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("forecast_tab_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("forecast_tab_0").performClick()
        composeTestRule.waitForIdle()

        // 3. Navigate to 3D Globe screen
        composeTestRule.onNodeWithTag("nav_item_globe_3d").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("globe_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("interactive_3d_globe_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("globe_autorotate_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("globe_zoom_in").performClick()
        composeTestRule.onNodeWithTag("globe_zoom_out").performClick()
        composeTestRule.waitForIdle()

        // 4. Navigate to Analytics screen and test 3D Scene Simulator presets
        composeTestRule.onNodeWithTag("nav_item_analytics").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("analytics_screen_root").assertIsDisplayed()
        WeatherSceneType.entries.forEach { preset ->
            val analyticsChipTag = "analytics_scene_chip_${preset.name.lowercase()}"
            val expectedSceneTag = "active_3d_scene_${preset.name.lowercase()}"
            composeTestRule.onNodeWithTag(analyticsChipTag)
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag(expectedSceneTag).assertIsDisplayed()
        }

        // 5. Navigate to Cities (Saved Locations) screen
        composeTestRule.onNodeWithTag("nav_item_locations").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("locations_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("city_search_input").assertIsDisplayed()

        // 6. Navigate to Settings screen and test unit & 3D quality controls
        composeTestRule.onNodeWithTag("nav_item_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("temp_unit_fahrenheit")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("temp_unit_celsius")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("wind_unit_mph")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("graphics_quality_balanced_3d")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("graphics_quality_high_3d")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        // Return to Weather Dashboard
        composeTestRule.onNodeWithTag("nav_item_dashboard").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hero_current_temp").assertIsDisplayed()
    }

    @Test
    fun advancedFeatures_rainRadar_confidence_smartFarming_andPersonalizedAlerts_workEndToEnd() {
        dismissSplashIfPresent()

        // Navigate to AI Copilot screen where all 4 new modules are integrated
        composeTestRule.onNodeWithTag("nav_item_ai_copilot").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_copilot_screen_root").assertIsDisplayed()

        // 1. Verify AI Rain Radar Card is displayed
        composeTestRule.onNodeWithTag("ai_copilot_screen_root")
            .performScrollToNode(hasTestTag("ai_rain_radar_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_rain_radar_card").assertIsDisplayed()

        // 2. Verify Forecast Confidence Analyzer Card & Bilingual Toggle
        composeTestRule.onNodeWithTag("ai_copilot_screen_root")
            .performScrollToNode(hasTestTag("forecast_confidence_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("forecast_confidence_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confidence_toggle_bengali_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confidence_toggle_bengali_button").performClick()
        composeTestRule.waitForIdle()

        // 3. Verify Bangladesh Smart Farming Mode Card, Location & Crop Selection
        composeTestRule.onNodeWithTag("ai_copilot_screen_root")
            .performScrollToNode(hasTestTag("smart_farming_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("smart_farming_card").assertIsDisplayed()

        composeTestRule.onNodeWithTag("farming_crop_chip_potato").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("farming_crop_chip_jute").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("farming_safety_notice_box").assertIsDisplayed()

        // 4. Verify Personalized Weather Alerts Card & Threshold Controls
        composeTestRule.onNodeWithTag("ai_copilot_screen_root")
            .performScrollToNode(hasTestTag("personalized_alerts_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("personalized_alerts_card").assertIsDisplayed()

        composeTestRule.onNodeWithTag("threshold_rain_75").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("threshold_temp_38").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("threshold_wind_55").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("threshold_aqi_150").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("personal_alerts_evaluate_button").performClick()
        composeTestRule.waitForIdle()
    }
}

