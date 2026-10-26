package com.example.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AtmosDatabase
import com.example.data.local.UserSettingsStore
import com.example.data.repository.WeatherRepository
import com.example.model.DefaultGlobalCities
import com.example.model.GraphicsQuality
import com.example.model.SavedLocation
import com.example.model.TempUnit
import com.example.model.ThemeMode
import com.example.model.UserSettings
import com.example.model.WeatherBundle
import com.example.model.WeatherSceneType
import com.example.model.WindUnit
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AtmosDestination(val route: String, val label: String) {
    DASHBOARD("dashboard", "Weather"),
    FORECASTS("forecasts", "Forecasts"),
    GLOBE_3D("globe_3d", "3D Globe"),
    ANALYTICS("analytics", "Analytics"),
    LOCATIONS("locations", "Cities"),
    SETTINGS("settings", "Settings")
}

data class AtmosUiState(
    val showSplash: Boolean = true,
    val currentDestination: AtmosDestination = AtmosDestination.DASHBOARD,
    val weatherBundle: WeatherBundle = WeatherRepositoryPlaceholder.defaultDhakaBundle(),
    val savedLocations: List<SavedLocation> = DefaultGlobalCities.hubs.take(5),
    val settings: UserSettings = UserSettings(),
    val activeSceneOverride: WeatherSceneType? = null,
    val isImmersiveMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SavedLocation> = emptyList(),
    val isSearching: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null
)

private object WeatherRepositoryPlaceholder {
    fun defaultDhakaBundle(): WeatherBundle {
        val dhaka = DefaultGlobalCities.hubs.first()
        return WeatherRepository(
            locationDao = object : com.example.data.local.SavedLocationDao {
                override fun observeAllLocations() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.SavedLocationEntity>())
                override suspend fun getAllLocationsOnce() = emptyList<com.example.data.local.SavedLocationEntity>()
                override suspend fun getLocationById(id: String) = null
                override suspend fun upsertLocation(location: com.example.data.local.SavedLocationEntity) = Unit
                override suspend fun upsertLocations(locations: List<com.example.data.local.SavedLocationEntity>) = Unit
                override suspend fun deleteLocationById(id: String) = Unit
            },
            cacheDao = object : com.example.data.local.WeatherCacheDao {
                override suspend fun getCacheForLocation(locationId: String) = null
                override suspend fun upsertCache(cache: com.example.data.local.WeatherCacheEntity) = Unit
            }
        ).buildOfflineSnapshot(dhaka)
    }
}

class AtmosViewModel(
    private val repository: WeatherRepository,
    private val settingsStore: UserSettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AtmosUiState())
    val uiState: StateFlow<AtmosUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var autoRefreshJob: Job? = null

    init {
        observeSettingsAndLocations()
    }

    private fun observeSettingsAndLocations() {
        viewModelScope.launch {
            repository.ensureSeededLocations()
            repository.savedLocationsFlow.collectLatest { locations ->
                if (locations.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(savedLocations = locations)
                    }
                }
            }
        }

        viewModelScope.launch {
            settingsStore.settingsFlow.collectLatest { userSettings ->
                val prevLocationId = _uiState.value.settings.activeLocationId
                val prevRefreshMins = _uiState.value.settings.autoRefreshMinutes
                _uiState.update { it.copy(settings = userSettings) }

                if (prevLocationId != userSettings.activeLocationId || _uiState.value.weatherBundle.isFromCache) {
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

        fetchWeatherForLocation(target, forceRefresh = forceRefresh)
    }

    fun selectLocation(location: SavedLocation, navigateToDashboard: Boolean = false) {
        viewModelScope.launch {
            repository.saveLocation(location)
            settingsStore.setActiveLocationId(location.id)
            _uiState.update {
                it.copy(
                    activeSceneOverride = null,
                    currentDestination = if (navigateToDashboard) AtmosDestination.DASHBOARD else it.currentDestination
                )
            }
            fetchWeatherForLocation(location, forceRefresh = true)
        }
    }

    fun saveLocationToFavorites(location: SavedLocation) {
        viewModelScope.launch {
            repository.saveLocation(location)
            _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
            selectLocation(location, navigateToDashboard = true)
        }
    }

    fun removeSavedLocation(locationId: String) {
        viewModelScope.launch {
            repository.removeLocation(locationId)
            val remaining = repository.getSavedLocationsOnce()
            if (_uiState.value.weatherBundle.location.id == locationId && remaining.isNotEmpty()) {
                selectLocation(remaining.first(), navigateToDashboard = false)
            }
        }
    }

    fun refreshCurrentWeather() {
        val currentLoc = _uiState.value.weatherBundle.location
        viewModelScope.launch {
            fetchWeatherForLocation(currentLoc, forceRefresh = true)
        }
    }

    private suspend fun fetchWeatherForLocation(location: SavedLocation, forceRefresh: Boolean) {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        val result = repository.getWeatherForLocation(location, forceRefresh = forceRefresh)
        result.onSuccess { bundle ->
            _uiState.update {
                it.copy(
                    weatherBundle = bundle,
                    isRefreshing = false,
                    errorMessage = null
                )
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    errorMessage = "Live network unavailable — showing cached forecast (${err.localizedMessage ?: "Offline"})"
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(280)
            repository.searchCities(query).onSuccess { matches ->
                _uiState.update { it.copy(searchResults = matches, isSearching = false) }
            }.onFailure {
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
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
        _uiState.update { it.copy(activeSceneOverride = sceneType) }
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
                    // Use last saved location when hardware GPS fix is unavailable in emulator
                    val fallback = _uiState.value.weatherBundle.location
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
            _uiState.update { it.copy(currentDestination = AtmosDestination.DASHBOARD) }
            fetchWeatherForLocation(resolved, forceRefresh = true)
        }
    }

    fun onLocationPermissionDenied() {
        val currentCity = _uiState.value.weatherBundle.location.cityName
        _uiState.update {
            it.copy(
                hasLocationPermission = false,
                errorMessage = "Location permission declined — displaying saved weather for $currentCity."
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

    fun setAutoRefreshMinutes(minutes: Int) {
        viewModelScope.launch { settingsStore.setAutoRefreshMinutes(minutes) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) }
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
