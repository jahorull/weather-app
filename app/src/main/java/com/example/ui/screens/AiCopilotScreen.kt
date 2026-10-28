package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AiSceneCommandResult
import com.example.ai.AtmosAiIntelligence
import com.example.ai.CopilotChatMessage
import com.example.ai.CopilotLanguage
import com.example.ai.LifeAdvisorItem
import com.example.model.BangladeshCrop
import com.example.model.DefaultGlobalCities
import com.example.model.PersonalThresholdAlert
import com.example.model.SavedLocation
import com.example.model.UserSettings
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.example.ui.components.AnimatedWeatherConditionIcon
import com.example.ui.components.FrostedGlassCard

private val ExampleCopilotPrompts = listOf(
    "Will it rain in Dhaka this afternoon?",
    "Should I carry an umbrella today?",
    "Explain today's weather in simple Bengali.",
    "আজকে আবহাওয়া কেমন থাকবে এবং ছাতা নেওয়া উচিত কি?",
    "Summarize the next 24 hours and 7-day forecast.",
    "How is the air quality (US AQI & PM2.5) right now?"
)

private val ExampleSceneCommands = listOf(
    "Show a rainy night.",
    "Create a golden sunset.",
    "Show a foggy morning.",
    "Switch to a sunny scene."
)

/**
 * Dedicated AI Weather Intelligence Screen integrating:
 * 1. AI Weather Copilot (English & Bengali conversational Q&A grounded in live API data)
 * 2. AI 3D Scene Controller (Natural-language commands mapped to validated 3D scene presets)
 * 3. AI Daily Life Advisor (Umbrella, outdoor activity, heat/UV, clothing & AQI recommendations)
 * 4. AI Forecast Explainer (24-hour & 7-day summaries with uncertainty notes)
 * 5. AI Travel Planner (Origin vs. Destination forecast comparison with 7-day horizon checks)
 * 6. Smart Weather Alert Explanations (Official NWS & live WMO threshold alert explanations)
 */
@Composable
fun AiCopilotScreen(
    activeLocation: SavedLocation,
    weatherBundle: WeatherBundle?,
    savedLocations: List<SavedLocation>,
    settings: UserSettings,
    activeSceneOverride: WeatherSceneType?,
    copilotLanguage: CopilotLanguage,
    chatMessages: List<CopilotChatMessage>,
    isAiGenerating: Boolean,
    lastFailedPrompt: String?,
    aiAdvisorHeadline: String?,
    aiAdvisorItems: List<LifeAdvisorItem>,
    isGeneratingAdvisor: Boolean,
    advisorErrorMessage: String?,
    lastSceneCommandResult: AiSceneCommandResult?,
    sceneCommandErrorMessage: String?,
    isProcessingSceneCommand: Boolean,
    travelDestination: SavedLocation,
    travelDestinationBundle: WeatherBundle?,
    travelDayOffset: Int,
    isLoadingTravelDestination: Boolean,
    onSelectLanguage: (CopilotLanguage) -> Unit,
    onSendCopilotPrompt: (String) -> Unit,
    onRetryLastPrompt: () -> Unit,
    onExecuteSceneCommand: (String) -> Unit,
    onResetSceneOverride: () -> Unit,
    onGenerateAiAdvisor: () -> Unit,
    onSelectTravelDestination: (SavedLocation) -> Unit,
    onSelectTravelDayOffset: (Int) -> Unit,
    onAskAiTravelPlan: () -> Unit,
    isRefreshing: Boolean = false,
    selectedFarmingCrop: BangladeshCrop = BangladeshCrop.AMAN_BORO_RICE,
    triggeredPersonalAlerts: List<PersonalThresholdAlert> = emptyList(),
    hasNotificationPermission: Boolean = false,
    lastNotificationStatusMessage: String? = null,
    onSelectFarmingLocation: (SavedLocation) -> Unit = {},
    onSelectFarmingCrop: (BangladeshCrop) -> Unit = {},
    onSetPersonalizedAlertsEnabled: (Boolean) -> Unit = {},
    onSetMonitorAllFavorites: (Boolean) -> Unit = {},
    onSetRainThreshold: (Int) -> Unit = {},
    onSetTempThreshold: (Int) -> Unit = {},
    onSetWindThreshold: (Int) -> Unit = {},
    onSetAqiThreshold: (Int) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onEvaluateAndNotifyNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    var sceneCommandInput by remember { mutableStateOf("") }

    val effectiveScene = activeSceneOverride
        ?: weatherBundle?.current?.sceneType
        ?: activeLocation.cachedSceneType
        ?: WeatherSceneType.SUNNY
    val accentColor = Color(effectiveScene.accentColorHex)

    val deterministicAdvisorItems = remember(weatherBundle, settings) {
        AtmosAiIntelligence.buildDeterministicDailyAdvisor(weatherBundle, settings)
    }
    val displayedAdvisorItems = aiAdvisorItems.ifEmpty { deterministicAdvisorItems }

    val travelCandidates = remember(savedLocations, activeLocation) {
        val all = (savedLocations + DefaultGlobalCities.hubs).distinctBy { it.id }
        all.filter { it.id != activeLocation.id }.ifEmpty { DefaultGlobalCities.hubs }
    }

    val travelSummary = remember(
        activeLocation,
        weatherBundle,
        travelDestination,
        travelDestinationBundle,
        travelDayOffset,
        settings
    ) {
        AtmosAiIntelligence.buildTravelComparisonSummary(
            originLocation = activeLocation,
            originBundle = weatherBundle,
            destinationLocation = travelDestination,
            destinationBundle = travelDestinationBundle,
            dayOffset = travelDayOffset,
            settings = settings
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_copilot_screen_root"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Grounded Data Status Bar
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI WEATHER INTELLIGENCE · GEMINI",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }
                        Text(
                            text = "Atmos AI Copilot & Advisor",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }

                    // Grounded Location & Update Time Pill
                    val isCached = weatherBundle?.isFromCache == true
                    val hasData = weatherBundle != null
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            !hasData -> Color(0xFFEF4444).copy(alpha = 0.25f)
                            isCached -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                            else -> Color(0xFF10B981).copy(alpha = 0.25f)
                        },
                        modifier = Modifier.testTag("ai_grounding_status_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hasData && !isCached) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (hasData && !isCached) Color(0xFF34D399) else Color(0xFFFBBF24),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    !hasData -> "${activeLocation.cityName}: Offline"
                                    isCached -> "${activeLocation.cityName}: Cached"
                                    else -> "${activeLocation.cityName}: Live API"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                val updateLabel = weatherBundle?.let {
                    val localPrefix = it.current.cityLocalTimeLabel
                        .takeIf { lbl -> lbl.isNotBlank() && lbl != "Unavailable" }
                        ?.let { lbl -> "Local $lbl · " } ?: ""
                    "Grounded in ${activeLocation.cityName}, ${activeLocation.country} ($localPrefix${
                        WeatherFormatter.formatRelativeUpdateTime(it.fetchedAtMillis)
                    })"
                } ?: "Live weather data for ${activeLocation.cityName} is currently unavailable — AI will not invent measurements."

                Text(
                    text = updateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.testTag("ai_location_update_subtitle")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Language Selector Strip (Auto, English, বাংলা)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    CopilotLanguage.entries.forEach { lang ->
                        val selected = copilotLanguage == lang
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) accentColor.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    1.dp,
                                    if (selected) accentColor else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectLanguage(lang) }
                                .testTag("ai_lang_${lang.name.lowercase()}")
                        ) {
                            Text(
                                text = lang.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. FEATURE 1 & 3: AI Weather Copilot & Forecast Explainer Card
        item {
            FrostedGlassCard(
                accentTint = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_copilot_chat_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "AI WEATHER COPILOT (ENGLISH & বাংলা)",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ask about current weather, hourly rain probability, 7-day trends, wind, humidity, UV, or US AQI.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Example Prompt Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(ExampleCopilotPrompts) { idx, example ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        promptInput = example
                                        onSendCopilotPrompt(example)
                                    }
                                    .testTag("ai_prompt_chip_$idx")
                            ) {
                                Text(
                                    text = example,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Forecast Explainer Buttons (Feature 3)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSendCopilotPrompt(
                                    "Summarize the next 24 hours in ${activeLocation.cityName}, including significant temperature changes, rain probability, and wind conditions."
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_explain_24h_button")
                        ) {
                            Text("Explain Next 24h", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = {
                                onSendCopilotPrompt(
                                    "Summarize the 7-day forecast for ${activeLocation.cityName}, highlighting temperature trends, rain risks, and forecast uncertainty."
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_explain_7d_button")
                        ) {
                            Text("Explain 7-Day", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Box + Send Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = {
                                Text(
                                    text = "Ask in English or বাংলা (e.g., Will it rain today?)",
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.10f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_copilot_input")
                        )

                        IconButton(
                            onClick = {
                                val trimmed = promptInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendCopilotPrompt(trimmed)
                                    promptInput = ""
                                }
                            },
                            enabled = !isAiGenerating && promptInput.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .testTag("ai_copilot_send_button")
                        ) {
                            if (isAiGenerating) {
                                CircularProgressIndicator(
                                    color = Color(0xFF041322),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send question to AI Copilot",
                                    tint = Color(0xFF041322)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Conversation Messages List
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chatMessages.forEach { msg ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = when {
                                    msg.isError -> Color(0xFFEF4444).copy(alpha = 0.22f)
                                    msg.isUser -> accentColor.copy(alpha = 0.22f)
                                    else -> Color.White.copy(alpha = 0.09f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            msg.isError -> Color(0xFFEF4444)
                                            msg.isUser -> accentColor.copy(alpha = 0.6f)
                                            else -> Color.White.copy(alpha = 0.14f)
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (msg.isUser) "YOU" else "ATMOS AI COPILOT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (msg.isError) Color(0xFFFCA5A5) else accentColor
                                        )
                                        Text(
                                            text = msg.timestampLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.60f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    msg.groundingBadge?.let { badge ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }
                        }

                        if (!lastFailedPrompt.isNullOrBlank() && !isAiGenerating) {
                            Button(
                                onClick = onRetryLastPrompt,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFBBF24),
                                    contentColor = Color(0xFF041322)
                                ),
                                modifier = Modifier.testTag("ai_copilot_retry_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry AI Question")
                            }
                        }
                    }
                }
            }
        }

        // 3. FEATURE 4: AI 3D Scene Controller Card
        item {
            FrostedGlassCard(
                accentTint = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_scene_controller_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI 3D SCENE CONTROLLER",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }

                        if (activeSceneOverride != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onResetSceneOverride() }
                                    .testTag("ai_scene_restore_live_button")
                            ) {
                                Text(
                                    text = "Simulated · Restore Live Scene",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFDE68A),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    val observedSceneTitle = weatherBundle?.current?.sceneType?.title ?: "Unavailable"
                    Text(
                        text = if (activeSceneOverride != null) {
                            "Simulated 3D Scene: ${activeSceneOverride.title} · Real Observed Weather ($observedSceneTitle) is unchanged."
                        } else {
                            "Synchronized with Live Observed Weather ($observedSceneTitle). Enter a natural-language command to simulate any 3D atmosphere."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.testTag("ai_scene_controller_status")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Natural Language Scene Command Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(ExampleSceneCommands) { idx, cmd ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        sceneCommandInput = cmd
                                        onExecuteSceneCommand(cmd)
                                    }
                                    .testTag("scene_cmd_chip_$idx")
                            ) {
                                Text(
                                    text = cmd,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sceneCommandInput,
                            onValueChange = { sceneCommandInput = it },
                            placeholder = {
                                Text(
                                    text = "e.g., Show a rainy night / Create a golden sunset",
                                    color = Color.White.copy(alpha = 0.55f)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.10f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.20f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_scene_command_input")
                        )

                        Button(
                            onClick = {
                                onExecuteSceneCommand(sceneCommandInput)
                            },
                            enabled = !isProcessingSceneCommand && sceneCommandInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color(0xFF041322)
                            ),
                            modifier = Modifier.testTag("ai_scene_command_submit")
                        ) {
                            if (isProcessingSceneCommand) {
                                CircularProgressIndicator(
                                    color = Color(0xFF041322),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text("Apply 3D")
                            }
                        }
                    }

                    lastSceneCommandResult?.let { res ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(res.matchedScene.accentColorHex).copy(alpha = 0.20f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_scene_command_result")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedWeatherConditionIcon(
                                    sceneType = res.matchedScene,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Validated Preset: ${res.matchedScene.title} (Simulated Visuals)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = res.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    if (!sceneCommandErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sceneCommandErrorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }
        }

        // 4. FEATURE 2: AI Daily Life Advisor Card
        item {
            FrostedGlassCard(
                accentTint = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_daily_advisor_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI DAILY LIFE ADVISOR",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFBBF24)
                            )
                        }

                        Button(
                            onClick = onGenerateAiAdvisor,
                            enabled = !isGeneratingAdvisor && weatherBundle != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFBBF24),
                                contentColor = Color(0xFF041322)
                            ),
                            modifier = Modifier.testTag("ai_refresh_advisor_button")
                        ) {
                            if (isGeneratingAdvisor) {
                                CircularProgressIndicator(
                                    color = Color(0xFF041322),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("AI Deep Advice")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = aiAdvisorHeadline
                            ?: "Practical recommendations based on ${activeLocation.cityName}'s actual forecast (labeled as recommendations, not guarantees).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f)
                    )

                    if (!advisorErrorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = advisorErrorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFFCA5A5)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (displayedAdvisorItems.isEmpty()) {
                        Text(
                            text = "Live forecast data is required to generate grounded daily life recommendations.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            displayedAdvisorItems.forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.category.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentColor
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFBBF24).copy(alpha = 0.20f)
                                            ) {
                                                Text(
                                                    text = item.badgeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFDE68A),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.recommendation,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Weather Basis: ${item.weatherReason}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. FEATURE 5: AI Travel Planner Card
        item {
            FrostedGlassCard(
                accentTint = Color(0xFF38BDF8),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_travel_planner_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI TRAVEL WEATHER PLANNER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Origin: ${activeLocation.cityName} → Select Destination & Travel Date",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Select Destination City:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(travelCandidates, key = { "travel_${it.id}" }) { city ->
                            val selected = city.id == travelDestination.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) Color(0xFF38BDF8).copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        1.dp,
                                        if (selected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.14f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectTravelDestination(city) }
                                    .testTag("travel_dest_${city.id}")
                            ) {
                                Text(
                                    text = city.cityName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "2. Select Travel Date Window:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val dateOptions = listOf(
                        0 to "Today",
                        1 to "Tomorrow",
                        3 to "+3 Days",
                        5 to "+5 Days",
                        8 to "+8 Days (Beyond 7d)"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(dateOptions, key = { it.first }) { (offset, label) ->
                            val selected = travelDayOffset == offset
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) Color(0xFF38BDF8).copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        1.dp,
                                        if (selected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.14f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectTravelDayOffset(offset) }
                                    .testTag("travel_day_offset_$offset")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (travelSummary.isBeyondReliableHorizon) {
                            Color(0xFFF59E0B).copy(alpha = 0.20f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("travel_comparison_summary_box")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (isLoadingTravelDestination) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF38BDF8),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Fetching live forecast for ${travelDestination.cityName}…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = travelSummary.deterministicSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onAskAiTravelPlan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF041322)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_travel_plan_button")
                    ) {
                        Text("Compare & Generate AI Travel Preparation Advice")
                    }
                }
            }
        }

        // 6. FEATURE 6: Smart Weather Alert Explanations Card
        item {
            val alerts = weatherBundle?.alerts.orEmpty()
            FrostedGlassCard(
                accentTint = if (alerts.isEmpty()) Color(0xFF34D399) else Color(0xFFF59E0B),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_smart_alerts_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (alerts.isEmpty()) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (alerts.isEmpty()) Color(0xFF34D399) else Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SMART WEATHER ALERT EXPLANATIONS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (alerts.isEmpty()) Color(0xFF34D399) else Color(0xFFF59E0B)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (alerts.isEmpty()) {
                        Text(
                            text = "No active official NWS alerts or WMO severe threshold advisories for ${activeLocation.cityName}. Atmos AI never invents warnings when conditions are normal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    } else {
                        alerts.forEach { alert ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "[${alert.severity.uppercase()}] ${alert.headline}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Source: ${alert.source} · Area: ${alert.affectedArea}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFDE68A)
                                    )
                                    Text(
                                        text = "Issued: ${alert.issuedAt} · Expires: ${alert.expiresAt}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.72f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = alert.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onSendCopilotPrompt(
                                    "Explain the active weather alerts for ${activeLocation.cityName} in plain language, including severity, source, issue/expiration times, and recommended safety precautions."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF041322)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_explain_alerts_button")
                        ) {
                            Text("Explain Active Alerts with AI")
                        }
                    }
                }
            }
        }

        // 7. NEW FEATURE 1: AI Rain Radar & Precipitation Timeline Card
        item {
            AiRainRadarCard(
                activeLocation = activeLocation,
                weatherBundle = weatherBundle,
                settings = settings,
                isRefreshing = isRefreshing,
                onAskAiRadarSummary = { prompt -> onSendCopilotPrompt(prompt) }
            )
        }

        // 8. NEW FEATURE 2: Forecast Confidence & Multi-Model Analyzer Card
        item {
            ForecastConfidenceAnalyzerCard(
                activeLocation = activeLocation,
                weatherBundle = weatherBundle,
                settings = settings,
                onAskAiModelSpread = { prompt -> onSendCopilotPrompt(prompt) }
            )
        }

        // 9. NEW FEATURE 3: Bangladesh Smart Farming Mode Card
        item {
            BangladeshSmartFarmingCard(
                activeLocation = activeLocation,
                weatherBundle = weatherBundle,
                savedLocations = savedLocations,
                selectedCrop = selectedFarmingCrop,
                settings = settings,
                onSelectLocation = onSelectFarmingLocation,
                onSelectCrop = onSelectFarmingCrop,
                onAskAiKrishiAdvisor = { prompt -> onSendCopilotPrompt(prompt) }
            )
        }

        // 10. NEW FEATURE 4: Personalized Weather Alerts (Custom Thresholds) Card
        item {
            PersonalizedWeatherAlertsCard(
                activeLocation = activeLocation,
                settings = settings,
                triggeredPersonalAlerts = triggeredPersonalAlerts,
                hasNotificationPermission = hasNotificationPermission,
                lastNotificationStatusMessage = lastNotificationStatusMessage,
                onSetPersonalizedAlertsEnabled = onSetPersonalizedAlertsEnabled,
                onSetMonitorAllFavorites = onSetMonitorAllFavorites,
                onSetRainThreshold = onSetRainThreshold,
                onSetTempThreshold = onSetTempThreshold,
                onSetWindThreshold = onSetWindThreshold,
                onSetAqiThreshold = onSetAqiThreshold,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onEvaluateAndNotifyNow = onEvaluateAndNotifyNow
            )
        }
    }
}

