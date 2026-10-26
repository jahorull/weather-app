package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNameResource_isAtmos3D() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Atmos 3D", context.getString(R.string.app_name))
    }

    @Test
    fun essentialNavigationAndDashboard_rendersAndNavigatesCorrectly() {
        composeTestRule.waitForIdle()

        // Dismiss splash screen if still visible
        runCatching {
            composeTestRule.onNodeWithTag("splash_screen_overlay").performClick()
        }
        composeTestRule.waitForIdle()

        // Verify 3D Weather backdrop & Dashboard hero temperature are displayed
        composeTestRule.onNodeWithTag("weather_3d_scene_backdrop").assertIsDisplayed()
        composeTestRule.onNodeWithTag("hero_current_temp").assertIsDisplayed()

        // Navigate to Forecasts screen
        composeTestRule.onNodeWithTag("nav_item_forecasts").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("forecasts_screen_root").assertIsDisplayed()

        // Navigate to 3D Globe screen
        composeTestRule.onNodeWithTag("nav_item_globe_3d").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("globe_screen_root").assertIsDisplayed()

        // Navigate to Saved Locations screen
        composeTestRule.onNodeWithTag("nav_item_locations").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("locations_screen_root").assertIsDisplayed()

        // Navigate to Settings screen
        composeTestRule.onNodeWithTag("nav_item_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen_root").assertIsDisplayed()
    }
}
