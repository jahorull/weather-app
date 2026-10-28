package com.example.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiSceneCommandResult
import com.example.ai.AtmosAiIntelligence
import com.example.ai.AtmosUniverseEngine
import com.example.ai.CopilotChatMessage
import com.example.ai.CopilotLanguage
import com.example.ai.LifeAdvisorItem
import com.example.ai.SmartFarmingAndAlertsEngine
import com.example.data.local.AtmosDatabase
import com.example.data.local.UserSettingsStore
import com.example.data.repository.WeatherRepository
import com.example.model.BangladeshCrop
import com.example.model.DefaultGlobalCities
import com.example.model.GraphicsQuality
import com.example.model.PersonalThresholdAlert
import com.example.model.SavedLocation
import com.example.model.TempUnit
import com.example.model.ThemeMode
import com.example.model.TimeMachineSlot
import com.example.model.TwinInterestCategory
import com.example.model.TwinUserPreference
import com.example.model.UserSettings
import com.example.model.WeatherBundle
import com.example.model.WeatherFormatter
import com.example.model.WeatherSceneType
import com.example.model.WhatIfSimulationState
import com.example.model.WindUnit
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

enum class AtmosDestination(val route: String, val label: String) {
    DASHBOARD("dashboard", "Weather"),
    FORECASTS("forecasts", "Forecast"),
    AI_COPILOT("ai_copilot", "AI Copilot"),
    GLOBE_3D("globe_3d", "3D Globe"),
    ANALYTICS("analytics", "Analytics"),
    LOCATIONS("locations", "Cities"),
    SETTINGS("settings", "Settings")
}

data class AtmosUiState(
    val showSplash: Boolean = true,
    val currentDestination: AtmosDestination = AtmosDestination.DASHBOARD,
    val activeLocation: SavedLocation = DefaultGlobalCities.hubs.first(),
    val weatherBundle: WeatherBundle? = null,
    val savedLocations: List<SavedLocation> = DefaultGlobalCities.hubs.take(5),
    val settings: UserSettings = UserSettings(),
    val activeSceneOverride: WeatherSceneType? = null,
    val isImmersiveMode: Boolean = false,
    val isRefreshing: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<SavedLocation> = emptyList(),
    val isSearching: Boolean = false,
    val searchErrorMessage: String? = null,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null,
    // AI Weather Intelligence State
    val copilotLanguage: CopilotLanguage = CopilotLanguage.AUTO,
    val chatMessages: List<CopilotChatMessage> = listOf(
        CopilotChatMessage(
            id = "welcome_msg",
            isUser = false,
            text = "Welcome to Atmosyn AI Weather Copilot! Ask me any question in English or Bengali (বাংলা) about current conditions, hourly rain chances, the 7-day outlook, UV index, or US AQI. Every answer is strictly grounded in your live Open-Meteo & NWS weather telemetry.",
            timestampLabel = "Ready",
            groundingBadge = "Grounded AI · EN / বাংলা"
        )
    ),
    val isAiGenerating: Boolean = false,
    val lastFailedPrompt: String? = null,
    val aiAdvisorHeadline: String? = null,
    val aiAdvisorItems: List<LifeAdvisorItem> = emptyList(),
    val isGeneratingAdvisor: Boolean = false,
    val advisorErrorMessage: String? = null,
    val lastSceneCommandResult: AiSceneCommandResult? = null,
    val sceneCommandErrorMessage: String? = null,
    val isProcessingSceneCommand: Boolean = false,
    val travelDestination: SavedLocation = DefaultGlobalCities.hubs.getOrElse(1) { DefaultGlobalCities.hubs.first() },
    val travelDestinationBundle: WeatherBundle? = null,
    val travelDayOffset: Int = 1,
    val isLoadingTravelDestination: Boolean = false,
    // Advanced Features: Smart Farming & Personalized Weather Alerts
    val selectedFarmingCrop: BangladeshCrop = BangladeshCrop.AMAN_BORO_RICE,
    val triggeredPersonalAlerts: List<PersonalThresholdAlert> = emptyList(),
    val hasNotificationPermission: Boolean = false,
    val lastNotificationStatusMessage: String? = null,
    // Signature Universe Features: What-If Lab, Weather Time Machine & Personal Weather Twin
    val whatIfSimulation: WhatIfSimulationState = WhatIfSimulationState(),
    val isProcessingWhatIfCommand: Boolean = false,
    val timeMachineSelectedSlot: TimeMachineSlot? = null,
    val twinPreferences: List<TwinUserPreference> = AtmosUniverseEngine.defaultTwinPreferences()
)

class AtmosViewModel(
    private val repository: WeatherRepository,
    private val settingsStore: UserSettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AtmosUiState())
    val uiState: StateFlow<AtmosUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var autoRefreshJob: Job? = null
    private var copilotJob: Job? = null
    private var initialFetchTriggered = false

    init {
        observeSettingsAndLocations()
    }

    private fun observeSettingsAndLocations() {
        viewModelScope.launch {
            settingsStore.twinPreferencesFlow.collectLatest { prefs ->
                _uiState.update { it.copy(twinPreferences = prefs) }
            }
        }

        viewModelScope.launch {
            repository.ensureSeededLocations()
            repository.savedLocationsFlow.collectLatest { locations ->
                if (locations.isNotEmpty()) {
                    _uiState.update { state ->
                        val active = locations.find { it.id == state.activeLocation.id } ?: state.activeLocation
                        val defaultDest = locations.firstOrNull { it.id != active.id }
                            ?: DefaultGlobalCities.hubs.firstOrNull { it.id != active.id }
                            ?: state.travelDestination
                        state.copy(
                            savedLocations = locations,
                            activeLocation = active,
                            travelDestination = if (state.travelDestination.id == active.id) defaultDest else state.travelDestination
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            settingsStore.settingsFlow.collectLatest { userSettings ->
                val prevLocationId = _uiState.value.settings.activeLocationId
                val prevRefreshMins = _uiState.value.settings.autoRefreshMinutes
                _uiState.update { state ->
                    val alerts = SmartFarmingAndAlertsEngine.evaluatePersonalThresholdAlerts(
                        activeBundle = state.weatherBundle,
                        additionalFavoriteBundles = listOfNotNull(state.travelDestinationBundle),
                        settings = userSettings
                    )
                    state.copy(
                        settings = userSettings,
                        triggeredPersonalAlerts = alerts
                    )
                }

                if (!initialFetchTriggered || prevLocationId != userSettings.activeLocationId) {
                    initialFetchTriggered = true
                    loadLocationById(userSettings.activeLocationId, forceRefresh = false)
                }
                if (prevRefreshMins != userSettings.autoRefreshMinutes || autoRefreshJob == null) {
                    restartAutoRefreshLoop(userSettings.autoRefreshMinutes)
                }
            }
        }
    }

    private fun restartAutoRefreshLoop(intervalMinutes: Int) {
        autoRefreshJob?.cancel()
        if (intervalMinutes <= 0) return
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalMinutes * 60_000L)
                refreshCurrentWeather()
            }
        }
    }

    private suspend fun loadLocationById(locationId: String, forceRefresh: Boolean) {
        val allSaved = repository.getSavedLocationsOnce()
        val target = allSaved.find { it.id == locationId }
            ?: DefaultGlobalCities.hubs.find { it.id == locationId }
            ?: allSaved.firstOrNull()
            ?: DefaultGlobalCities.hubs.first()

        _uiState.update { it.copy(activeLocation = target) }
        fetchWeatherForLocation(target, forceRefresh = forceRefresh)
        fetchTravelDestinationWeather(_uiState.value.travelDestination, forceRefresh = false)
    }

    fun selectLocation(location: SavedLocation, navigateToDashboard: Boolean = false) {
        viewModelScope.launch {
            repository.saveLocation(location)
            settingsStore.setActiveLocationId(location.id)
            _uiState.update {
                it.copy(
                    activeLocation = location,
                    activeSceneOverride = null,
                    aiAdvisorItems = emptyList(),
                    aiAdvisorHeadline = null,
                    currentDestination = if (navigateToDashboard) AtmosDestination.DASHBOARD else it.currentDestination
                )
            }
            fetchWeatherForLocation(location, forceRefresh = true)
        }
    }

    fun saveLocationToFavorites(location: SavedLocation) {
        viewModelScope.launch {
            repository.saveLocation(location)
            _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), searchErrorMessage = null) }
            selectLocation(location, navigateToDashboard = true)
        }
    }

    fun removeSavedLocation(locationId: String) {
        viewModelScope.launch {
            repository.removeLocation(locationId)
            val remaining = repository.getSavedLocationsOnce()
            if (_uiState.value.activeLocation.id == locationId && remaining.isNotEmpty()) {
                selectLocation(remaining.first(), navigateToDashboard = false)
            }
        }
    }

    fun refreshCurrentWeather() {
        val currentLoc = _uiState.value.activeLocation
        viewModelScope.launch {
            fetchWeatherForLocation(currentLoc, forceRefresh = true)
        }
    }

    private suspend fun fetchWeatherForLocation(location: SavedLocation, forceRefresh: Boolean) {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        val result = repository.getWeatherForLocation(location, forceRefresh = forceRefresh)
        result.onSuccess { bundle ->
            _uiState.update { state ->
                val personalAlerts = SmartFarmingAndAlertsEngine.evaluatePersonalThresholdAlerts(
                    activeBundle = bundle,
                    additionalFavoriteBundles = listOfNotNull(state.travelDestinationBundle),
                    settings = state.settings
                )
                state.copy(
                    activeLocation = bundle.location,
                    weatherBundle = bundle,
                    triggeredPersonalAlerts = personalAlerts,
                    isRefreshing = false,
                    errorMessage = if (bundle.isFromCache && forceRefresh) {
                        "Live network unreachable — displaying cached forecast from Room database."
                    } else {
                        null
                    }
                )
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    errorMessage = "Could not connect to live weather service (${err.localizedMessage ?: "Network error"})."
                )
            }
        }
    }

    private suspend fun fetchTravelDestinationWeather(destination: SavedLocation, forceRefresh: Boolean) {
        _uiState.update { it.copy(isLoadingTravelDestination = true) }
        repository.getWeatherForLocation(destination, forceRefresh = forceRefresh)
            .onSuccess { bundle ->
                _uiState.update {
                    it.copy(
                        travelDestinationBundle = bundle,
                        isLoadingTravelDestination = false
                    )
                }
            }
            .onFailure {
                _uiState.update {
                    it.copy(
                        travelDestinationBundle = null,
                        isLoadingTravelDestination = false
                    )
                }
            }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, searchErrorMessage = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, searchErrorMessage = null) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchErrorMessage = null) }
            delay(280)
            repository.searchCities(query).onSuccess { matches ->
                _uiState.update {
                    it.copy(
                        searchResults = matches,
                        isSearching = false,
                        searchErrorMessage = if (matches.isEmpty()) {
                            "No cities found matching \"${query.trim()}\"."
                        } else {
                            null
                        }
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        searchErrorMessage = "Unable to search cities while offline. Check your connection."
                    )
                }
            }
        }
    }

    fun dismissSplash() {
        _uiState.update { it.copy(showSplash = false) }
    }

    fun navigateTo(destination: AtmosDestination) {
        _uiState.update { it.copy(currentDestination = destination, isImmersiveMode = false) }
    }

    fun toggleImmersiveMode() {
        _uiState.update { it.copy(isImmersiveMode = !it.isImmersiveMode) }
    }

    fun setSceneOverride(sceneType: WeatherSceneType?) {
        _uiState.update {
            it.copy(
                activeSceneOverride = sceneType,
                lastSceneCommandResult = if (sceneType == null) null else it.lastSceneCommandResult,
                sceneCommandErrorMessage = null,
                whatIfSimulation = if (sceneType == null) WhatIfSimulationState(isActive = false) else it.whatIfSimulation.copy(isActive = false),
                timeMachineSelectedSlot = if (sceneType == null) null else it.timeMachineSelectedSlot
            )
        }
    }

    // =========================================================================
    // AI WEATHER INTELLIGENCE ACTIONS (Features 1 to 6)
    // =========================================================================

    fun setCopilotLanguage(language: CopilotLanguage) {
        _uiState.update { it.copy(copilotLanguage = language) }
    }

    fun sendCopilotPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        val nowLabel = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val userMessage = CopilotChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed,
            timestampLabel = nowLabel
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val stateSnapshot = _uiState.value
        val bundle = stateSnapshot.weatherBundle
        val location = stateSnapshot.activeLocation
        val settings = stateSnapshot.settings
        val language = stateSnapshot.copilotLanguage

        val groundingLabel = when {
            bundle == null -> "${location.cityName} · Offline (No Telemetry)"
            bundle.isFromCache -> "${location.cityName} · Cached (${WeatherFormatter.formatRelativeUpdateTime(bundle.fetchedAtMillis)})"
            else -> "${location.cityName} · Live (${WeatherFormatter.formatRelativeUpdateTime(bundle.fetchedAtMillis)})"
        }

        val placeholderAssistant = CopilotChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "Analyzing live meteorological telemetry for ${location.cityName}…",
            timestampLabel = nowLabel,
            isStreaming = true,
            groundingBadge = groundingLabel
        )

        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMessage + placeholderAssistant,
                isAiGenerating = true,
                lastFailedPrompt = null
            )
        }

        copilotJob?.cancel()
        copilotJob = viewModelScope.launch {
            try {
                AtmosAiIntelligence.streamGroundedCopilotAnswer(
                    userPrompt = trimmed,
                    activeLocation = location,
                    weatherBundle = bundle,
                    settings = settings,
                    language = language
                ).collect { partialText ->
                    _uiState.update { current ->
                        current.copy(
                            chatMessages = current.chatMessages.map { msg ->
                                if (msg.id == assistantMsgId) {
                                    msg.copy(text = partialText, isStreaming = true, isError = false)
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                }
                _uiState.update { current ->
                    current.copy(
                        isAiGenerating = false,
                        chatMessages = current.chatMessages.map { msg ->
                            if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: TimeoutCancellationException) {
                val fallback = buildGroundedOfflineFallbackAnswer(trimmed, location, bundle, settings, language)
                _uiState.update { current ->
                    current.copy(
                        isAiGenerating = false,
                        lastFailedPrompt = trimmed,
                        chatMessages = current.chatMessages.map { msg ->
                            if (msg.id == assistantMsgId) {
                                msg.copy(
                                    text = "$fallback\n\n(Note: Gemini AI request timed out; showing direct grounded API summary above. Tap Retry to re-query AI.)",
                                    isStreaming = false,
                                    isError = true
                                )
                            } else {
                                msg
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                val fallback = buildGroundedOfflineFallbackAnswer(trimmed, location, bundle, settings, language)
                _uiState.update { current ->
                    current.copy(
                        isAiGenerating = false,
                        lastFailedPrompt = trimmed,
                        chatMessages = current.chatMessages.map { msg ->
                            if (msg.id == assistantMsgId) {
                                msg.copy(
                                    text = "$fallback\n\n(Note: Live Gemini connection unavailable [${e.localizedMessage ?: "Network/Service Error"}]; showing verified telemetry report above. Tap Retry to re-query AI.)",
                                    isStreaming = false,
                                    isError = true
                                )
                            } else {
                                msg
                            }
                        }
                    )
                }
            }
        }
    }

    fun retryLastCopilotPrompt() {
        val lastPrompt = _uiState.value.lastFailedPrompt ?: return
        sendCopilotPrompt(lastPrompt)
    }

    fun askAiAboutForecastPeriod(periodPrompt: String) {
        _uiState.update { it.copy(currentDestination = AtmosDestination.AI_COPILOT, isImmersiveMode = false) }
        sendCopilotPrompt(periodPrompt)
    }

    fun executeAiSceneCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(sceneCommandErrorMessage = "Please enter a 3D scene command such as 'Show a rainy night.' or 'Create a golden sunset.'")
            }
            return
        }

        // Apply fast local match immediately if recognized so the 3D scene updates with zero latency
        val immediateMatch = AtmosAiIntelligence.matchLocalSceneCommand(trimmed)
        if (immediateMatch != null) {
            _uiState.update {
                it.copy(
                    activeSceneOverride = immediateMatch,
                    sceneCommandErrorMessage = null,
                    lastSceneCommandResult = AiSceneCommandResult(
                        commandText = trimmed,
                        matchedScene = immediateMatch,
                        explanation = "Activated simulated '${immediateMatch.title}' 3D environment for \"$trimmed\". Real observed weather data remains separate and unmodified.",
                        visualHighlights = immediateMatch.subtitle
                    )
                )
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingSceneCommand = true, sceneCommandErrorMessage = null) }
            try {
                val observed = _uiState.value.weatherBundle?.current?.sceneType
                val resolved = AtmosAiIntelligence.resolve3DSceneCommand(
                    command = trimmed,
                    observedScene = observed
                )
                _uiState.update {
                    it.copy(
                        activeSceneOverride = resolved.matchedScene,
                        lastSceneCommandResult = resolved,
                        sceneCommandErrorMessage = null,
                        isProcessingSceneCommand = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingSceneCommand = false,
                        sceneCommandErrorMessage = if (immediateMatch == null) {
                            e.localizedMessage ?: "Could not map command to a validated 3D preset."
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    fun generateAiDailyAdvisor() {
        val snapshot = _uiState.value
        val bundle = snapshot.weatherBundle
        if (bundle == null) {
            _uiState.update {
                it.copy(advisorErrorMessage = "Cannot generate AI Daily Advisor while weather data is unavailable.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAdvisor = true, advisorErrorMessage = null) }
            try {
                val (headline, items) = AtmosAiIntelligence.generateAiDailyLifeAdvisor(
                    activeLocation = snapshot.activeLocation,
                    weatherBundle = bundle,
                    settings = snapshot.settings,
                    language = snapshot.copilotLanguage
                )
                _uiState.update {
                    it.copy(
                        aiAdvisorHeadline = headline,
                        aiAdvisorItems = items,
                        isGeneratingAdvisor = false,
                        advisorErrorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val deterministic = AtmosAiIntelligence.buildDeterministicDailyAdvisor(bundle, snapshot.settings)
                _uiState.update {
                    it.copy(
                        aiAdvisorHeadline = "Grounded Daily Life Recommendations for ${snapshot.activeLocation.cityName}",
                        aiAdvisorItems = deterministic,
                        isGeneratingAdvisor = false,
                        advisorErrorMessage = "Gemini advisor refresh unavailable (${e.localizedMessage ?: "Network error"}) — displaying grounded telemetry recommendations."
                    )
                }
            }
        }
    }

    fun selectTravelDestination(destination: SavedLocation) {
        _uiState.update { it.copy(travelDestination = destination) }
        viewModelScope.launch {
            fetchTravelDestinationWeather(destination, forceRefresh = false)
        }
    }

    fun selectTravelDayOffset(dayOffset: Int) {
        _uiState.update { it.copy(travelDayOffset = dayOffset.coerceIn(0, 9)) }
    }

    fun askAiForTravelPlan() {
        val state = _uiState.value
        val summary = AtmosAiIntelligence.buildTravelComparisonSummary(
            originLocation = state.activeLocation,
            originBundle = state.weatherBundle,
            destinationLocation = state.travelDestination,
            destinationBundle = state.travelDestinationBundle,
            dayOffset = state.travelDayOffset,
            settings = state.settings
        )
        val prompt = buildString {
            append("Compare travel weather from ${state.activeLocation.cityName} to ${state.travelDestination.cityName} for ${summary.dateLabel} (+${state.travelDayOffset} days). ")
            append("Telemetry comparison: ${summary.deterministicSummary} ")
            if (summary.isBeyondReliableHorizon) {
                append("Explain clearly why forecasts beyond 7 days have high meteorological uncertainty and what general preparation is advisable.")
            } else {
                append("Provide practical packing, clothing, umbrella, and transit preparation recommendations based strictly on these numbers.")
            }
        }
        sendCopilotPrompt(prompt)
    }

    private fun buildGroundedOfflineFallbackAnswer(
        prompt: String,
        location: SavedLocation,
        bundle: WeatherBundle?,
        settings: UserSettings,
        language: CopilotLanguage
    ): String {
        val wantsBengali = language == CopilotLanguage.BENGALI ||
            prompt.lowercase(Locale.ROOT).contains("bengali") ||
            prompt.contains("বাংলা") ||
            prompt.any { it in '\u0980'..'\u09FF' }

        if (bundle == null) {
            return if (wantsBengali) {
                "${location.cityName}-এর লাইভ আবহাওয়া তথ্য এই মুহূর্তে পাওয়া যাচ্ছে না। অনুগ্রহ করে ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।"
            } else {
                "Live weather telemetry for ${location.cityName} is currently unavailable. Please check your connection and refresh weather data."
            }
        }

        val cur = bundle.current
        val temp = WeatherFormatter.formatTemp(cur.temperatureC, settings.tempUnit)
        val feels = WeatherFormatter.formatTemp(cur.feelsLikeC, settings.tempUnit)
        val rain = WeatherFormatter.formatNullablePercent(cur.precipProbabilityPercent)
        val wind = WeatherFormatter.formatWind(cur.windSpeedKmh, settings.windUnit)
        val hum = WeatherFormatter.formatNullablePercent(cur.humidityPercent)
        val uv = WeatherFormatter.formatNullableUv(cur.uvIndex)
        val aqi = bundle.airQuality?.usAqi?.toString() ?: "Unavailable"
        val updated = WeatherFormatter.formatRelativeUpdateTime(bundle.fetchedAtMillis)

        return if (wantsBengali) {
            """
            📍 ${location.cityName}, ${location.country} ($updated):
            • বর্তমান আবহাওয়া: ${cur.conditionText}, তাপমাত্রা $temp (অনুভূত হচ্ছে $feels)
            • বৃষ্টির সম্ভাবনা: $rain | আর্দ্রতা: $hum | বাতাসের গতি: $wind
            • ইউভি ইনডেক্স (UV): $uv | বায়ুর মান (US AQI): $aqi
            • পরামর্শ (Recommendation): বৃষ্টির সম্ভাবনা $rain এবং অনুভূত তাপমাত্রা $feels অনুযায়ী বাইরে বের হওয়ার প্রস্তুতি নিন (এটি একটি পরামর্শ, নিশ্চয়তা নয়)।
            """.trimIndent()
        } else {
            """
            Grounded Telemetry Report for ${location.cityName}, ${location.country} ($updated):
            • Current Condition: ${cur.conditionText} at $temp (Feels like $feels, High ${WeatherFormatter.formatTemp(cur.highC, settings.tempUnit)} / Low ${WeatherFormatter.formatTemp(cur.lowC, settings.tempUnit)})
            • Rain Probability: $rain · Humidity: $hum · Wind: $wind
            • UV Index: $uv · US AQI: $aqi
            • Recommendation (not a guarantee): Based on $rain rain probability and $feels feels-like temperature, plan umbrella and hydration readiness accordingly.
            """.trimIndent()
        }
    }

    fun updateLocationPermissionStatus(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
    }

    @SuppressLint("MissingPermission")
    fun detectDeviceGpsLocation(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val tokenSource = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            onCoordinatesDetected(loc.latitude, loc.longitude)
                        } else {
                            fallbackLocationManagerLookup(context)
                        }
                    }
                    .addOnFailureListener {
                        fallbackLocationManagerLookup(context)
                    }
            } catch (_: Exception) {
                fallbackLocationManagerLookup(context)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackLocationManagerLookup(context: Context) {
        viewModelScope.launch {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val providers = lm?.getProviders(true).orEmpty()
                val bestLoc = providers.mapNotNull { provider ->
                    runCatching { lm?.getLastKnownLocation(provider) }.getOrNull()
                }.firstOrNull()

                if (bestLoc != null) {
                    onCoordinatesDetected(bestLoc.latitude, bestLoc.longitude)
                } else {
                    val fallback = _uiState.value.activeLocation
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = "GPS signal unavailable on device — using saved location (${fallback.cityName})."
                        )
                    }
                    fetchWeatherForLocation(fallback, forceRefresh = true)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun onCoordinatesDetected(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val resolved = repository.resolveGpsCoordinates(latitude, longitude)
            settingsStore.setActiveLocationId(resolved.id)
            _uiState.update {
                it.copy(
                    activeLocation = resolved,
                    currentDestination = AtmosDestination.DASHBOARD
                )
            }
            fetchWeatherForLocation(resolved, forceRefresh = true)
        }
    }

    fun onLocationPermissionDenied() {
        val currentCity = _uiState.value.activeLocation.cityName
        _uiState.update {
            it.copy(
                hasLocationPermission = false,
                errorMessage = "Location permission denied — showing weather for saved city ($currentCity)."
            )
        }
    }

    fun setTempUnit(unit: TempUnit) {
        viewModelScope.launch { settingsStore.setTempUnit(unit) }
    }

    fun setWindUnit(unit: WindUnit) {
        viewModelScope.launch { settingsStore.setWindUnit(unit) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setGraphicsQuality(quality: GraphicsQuality) {
        viewModelScope.launch { settingsStore.setGraphicsQuality(quality) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setReducedMotion(enabled) }
    }

    fun setAutoRefreshMinutes(minutes: Int) {
        viewModelScope.launch { settingsStore.setAutoRefreshMinutes(minutes) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) }
    }

    // =========================================================================
    // SMART FARMING & PERSONALIZED WEATHER ALERT ACTIONS
    // =========================================================================

    fun selectFarmingCrop(crop: BangladeshCrop) {
        _uiState.update { it.copy(selectedFarmingCrop = crop) }
    }

    fun setPersonalizedAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setPersonalizedAlertsEnabled(enabled) }
    }

    fun setMonitorAllFavoriteCities(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setMonitorAllFavoriteCities(enabled) }
    }

    fun setRainAlertThreshold(percent: Int) {
        viewModelScope.launch { settingsStore.setRainAlertThreshold(percent) }
    }

    fun setTempAlertThreshold(tempC: Int) {
        viewModelScope.launch { settingsStore.setTempAlertThreshold(tempC) }
    }

    fun setWindAlertThreshold(speedKmh: Int) {
        viewModelScope.launch { settingsStore.setWindAlertThreshold(speedKmh) }
    }

    fun setAqiAlertThreshold(usAqi: Int) {
        viewModelScope.launch { settingsStore.setAqiAlertThreshold(usAqi) }
    }

    fun updateNotificationPermissionStatus(granted: Boolean) {
        _uiState.update { it.copy(hasNotificationPermission = granted) }
    }

    fun evaluateAndNotifyPersonalAlerts(context: Context) {
        val state = _uiState.value
        val hasPerm = SmartFarmingAndAlertsEngine.hasNotificationPermission(context)
        val alerts = SmartFarmingAndAlertsEngine.evaluatePersonalThresholdAlerts(
            activeBundle = state.weatherBundle,
            additionalFavoriteBundles = listOfNotNull(state.travelDestinationBundle),
            settings = state.settings
        )

        var dispatchedCount = 0
        if (hasPerm && state.settings.personalizedAlertsEnabled) {
            alerts.forEach { alert ->
                if (SmartFarmingAndAlertsEngine.dispatchPersonalAlertNotification(context, alert)) {
                    dispatchedCount++
                }
            }
        }

        val statusMsg = when {
            !state.settings.personalizedAlertsEnabled ->
                "Personalized alerts are currently disabled. Enable the toggle above to evaluate thresholds."
            alerts.isEmpty() ->
                "Evaluated live telemetry for ${state.activeLocation.cityName}: 0 thresholds exceeded right now."
            !hasPerm ->
                "Evaluated live telemetry: ${alerts.size} threshold(s) exceeded in-app (grant Notification Permission to receive system tray notifications)."
            dispatchedCount > 0 ->
                "Evaluated live telemetry: ${alerts.size} threshold(s) exceeded — dispatched $dispatchedCount local notification(s)."
            else ->
                "Evaluated live telemetry: ${alerts.size} threshold(s) active in-app."
        }

        _uiState.update {
            it.copy(
                hasNotificationPermission = hasPerm,
                triggeredPersonalAlerts = alerts,
                lastNotificationStatusMessage = statusMsg
            )
        }
    }

    // =========================================================================
    // SIGNATURE UNIVERSE FEATURES: WHAT-IF LAB, TIME MACHINE & WEATHER TWIN
    // =========================================================================

    fun updateWhatIfParameters(
        windStrength: Float = _uiState.value.whatIfSimulation.windStrength,
        cloudDensity: Float = _uiState.value.whatIfSimulation.cloudDensity,
        rainIntensity: Float = _uiState.value.whatIfSimulation.rainIntensity,
        fogDensity: Float = _uiState.value.whatIfSimulation.fogDensity,
        sunlightIntensity: Float = _uiState.value.whatIfSimulation.sunlightIntensity,
        timeOfDayHour: Float = _uiState.value.whatIfSimulation.timeOfDayHour
    ) {
        val w = windStrength.coerceIn(0f, 1f)
        val c = cloudDensity.coerceIn(0f, 1f)
        val r = rainIntensity.coerceIn(0f, 1f)
        val f = fogDensity.coerceIn(0f, 1f)
        val s = sunlightIntensity.coerceIn(0f, 1f)
        val hr = timeOfDayHour.coerceIn(0f, 23.5f)
        val derivedScene = WhatIfSimulationState.deriveSceneFromParameters(
            windStrength = w,
            cloudDensity = c,
            rainIntensity = r,
            fogDensity = f,
            sunlightIntensity = s,
            timeOfDayHour = hr
        )
        val explanation = "HYPOTHETICAL SIMULATION: Wind ${(w * 100).roundToInt()}%, Clouds ${(c * 100).roundToInt()}%, Rain ${(r * 100).roundToInt()}%, Fog ${(f * 100).roundToInt()}%, Sunlight ${(s * 100).roundToInt()}%, Time ${AtmosUniverseEngine.formatFractionalHour(hr)} (${derivedScene.title}). Does not change or predict real atmospheric conditions."

        _uiState.update {
            it.copy(
                whatIfSimulation = WhatIfSimulationState(
                    isActive = true,
                    windStrength = w,
                    cloudDensity = c,
                    rainIntensity = r,
                    fogDensity = f,
                    sunlightIntensity = s,
                    timeOfDayHour = hr,
                    derivedSceneType = derivedScene,
                    activeCommandLabel = "Manual Parameter Experiment",
                    simulationExplanation = explanation
                ),
                activeSceneOverride = derivedScene,
                timeMachineSelectedSlot = null
            )
        }
    }

    fun executeWhatIfCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        val immediate = AtmosUniverseEngine.parseWhatIfCommandLocally(trimmed)
        if (immediate != null) {
            _uiState.update {
                it.copy(
                    whatIfSimulation = immediate,
                    activeSceneOverride = immediate.derivedSceneType,
                    timeMachineSelectedSlot = null
                )
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingWhatIfCommand = true) }
            try {
                val resolved = AtmosUniverseEngine.resolveWhatIfCommand(trimmed)
                _uiState.update {
                    it.copy(
                        whatIfSimulation = resolved,
                        activeSceneOverride = resolved.derivedSceneType,
                        timeMachineSelectedSlot = null,
                        isProcessingWhatIfCommand = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isProcessingWhatIfCommand = false) }
            }
        }
    }

    fun resetWhatIfSimulation() {
        _uiState.update {
            it.copy(
                whatIfSimulation = WhatIfSimulationState(isActive = false),
                activeSceneOverride = null,
                timeMachineSelectedSlot = null,
                isProcessingWhatIfCommand = false
            )
        }
    }

    fun selectTimeMachineSlot(slot: TimeMachineSlot) {
        _uiState.update {
            it.copy(
                timeMachineSelectedSlot = slot,
                whatIfSimulation = it.whatIfSimulation.copy(isActive = false),
                activeSceneOverride = slot.sceneType ?: it.activeSceneOverride
            )
        }
    }

    fun exitTimeMachineMode() {
        _uiState.update {
            it.copy(
                timeMachineSelectedSlot = null,
                activeSceneOverride = null
            )
        }
    }

    fun saveOrUpdateTwinPreference(
        existingId: String?,
        interest: TwinInterestCategory,
        customTitle: String,
        preferredWindow: String,
        userNote: String
    ) {
        val title = customTitle.trim().ifEmpty { interest.labelEn }
        val window = preferredWindow.trim().ifEmpty { interest.defaultWindow }
        val note = userNote.trim()
        val nowStamp = "Saved " + SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())

        val currentList = _uiState.value.twinPreferences
        val updatedList = if (existingId != null && currentList.any { it.id == existingId }) {
            currentList.map { item ->
                if (item.id == existingId) {
                    item.copy(
                        interest = interest,
                        customTitle = title,
                        preferredWindow = window,
                        userNote = note,
                        updatedAtLabel = nowStamp
                    )
                } else {
                    item
                }
            }
        } else {
            val newPref = TwinUserPreference(
                id = existingId ?: "twin_${UUID.randomUUID().toString().take(8)}",
                interest = interest,
                customTitle = title,
                preferredWindow = window,
                userNote = note,
                updatedAtLabel = nowStamp
            )
            currentList + newPref
        }

        _uiState.update { it.copy(twinPreferences = updatedList) }
        viewModelScope.launch {
            settingsStore.saveTwinPreferences(updatedList)
        }
    }

    fun deleteTwinPreference(preferenceId: String) {
        val updatedList = _uiState.value.twinPreferences.filterNot { it.id == preferenceId }
        _uiState.update { it.copy(twinPreferences = updatedList) }
        viewModelScope.launch {
            settingsStore.saveTwinPreferences(updatedList)
        }
    }

    fun resetTwinPreferencesToDefault() {
        val defaults = AtmosUniverseEngine.defaultTwinPreferences()
        _uiState.update { it.copy(twinPreferences = defaults) }
        viewModelScope.launch {
            settingsStore.saveTwinPreferences(defaults)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AtmosDatabase.getInstance(context)
            val repository = WeatherRepository(
                locationDao = db.savedLocationDao(),
                cacheDao = db.weatherCacheDao()
            )
            val settingsStore = UserSettingsStore(context.applicationContext)
            return AtmosViewModel(repository, settingsStore) as T
        }
    }
}
