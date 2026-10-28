package com.example.data.repository

import com.example.data.local.SavedLocationDao
import com.example.data.local.SavedLocationEntity
import com.example.data.local.WeatherCacheDao
import com.example.data.local.WeatherCacheEntity
import com.example.data.remote.NationalWeatherServiceAlertsApi
import com.example.data.remote.NetworkModule
import com.example.data.remote.NwsAlertsResponse
import com.example.data.remote.OpenMeteoAirQualityApi
import com.example.data.remote.OpenMeteoAirQualityResponse
import com.example.data.remote.OpenMeteoForecastApi
import com.example.data.remote.OpenMeteoForecastResponse
import com.example.data.remote.OpenMeteoGeocodingApi
import com.example.data.remote.RainViewerMapsApi
import com.example.data.remote.ReverseGeocodingApi
import com.example.data.remote.WeatherApiMapper
import com.example.model.DefaultGlobalCities
import com.example.model.SavedLocation
import com.example.model.WeatherBundle
import com.example.model.WeatherSceneType
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import java.util.Locale
import kotlin.math.roundToInt

class WeatherRepository(
    private val locationDao: SavedLocationDao,
    private val cacheDao: WeatherCacheDao,
    private val forecastApi: OpenMeteoForecastApi = NetworkModule.forecastApi,
    private val rainViewerApi: RainViewerMapsApi = NetworkModule.rainViewerApi,
    private val airQualityApi: OpenMeteoAirQualityApi = NetworkModule.airQualityApi,
    private val nwsAlertsApi: NationalWeatherServiceAlertsApi = NetworkModule.nwsAlertsApi,
    private val geocodingApi: OpenMeteoGeocodingApi = NetworkModule.geocodingApi,
    private val reverseGeocodingApi: ReverseGeocodingApi = NetworkModule.reverseGeocodingApi
) {
    private val moshi = NetworkModule.moshiInstance()
    private val forecastAdapter = moshi.adapter(OpenMeteoForecastResponse::class.java)
    private val airQualityAdapter = moshi.adapter(OpenMeteoAirQualityResponse::class.java)
    private val alertsAdapter = moshi.adapter(NwsAlertsResponse::class.java)

    val savedLocationsFlow: Flow<List<SavedLocation>> = locationDao.observeAllLocations()
        .map { entities -> entities.map { it.toDomain() } }

    suspend fun ensureSeededLocations() {
        val existing = locationDao.getAllLocationsOnce()
        if (existing.isEmpty()) {
            val defaultEntities = DefaultGlobalCities.hubs.take(5).mapIndexed { index, loc ->
                loc.toEntity(sortOrder = index)
            }
            locationDao.upsertLocations(defaultEntities)
        }
    }

    suspend fun getSavedLocationsOnce(): List<SavedLocation> {
        ensureSeededLocations()
        return locationDao.getAllLocationsOnce().map { it.toDomain() }
    }

    /**
     * Retrieves live weather, air quality, active alerts, RainViewer radar composite metadata,
     * and multi-model (ECMWF IFS, NOAA GFS, DWD ICON) comparison data for [location].
     * Never fabricates mock data as real weather.
     */
    suspend fun getWeatherForLocation(
        location: SavedLocation,
        forceRefresh: Boolean = false
    ): Result<WeatherBundle> {
        if (!forceRefresh && !shouldFetchFromNetwork(location.id)) {
            val cached = loadFromCache(location)
            if (cached != null) {
                return Result.success(cached)
            }
        }

        return try {
            supervisorScope {
                val tz = location.timezone.ifBlank { "auto" }
                val forecastDeferred = async {
                    forecastApi.getForecast(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timezone = tz
                    )
                }
                val airQualityDeferred = async {
                    runCatching {
                        airQualityApi.getAirQuality(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timezone = tz
                        )
                    }.getOrNull()
                }
                val rainViewerDeferred = async {
                    runCatching {
                        rainViewerApi.getWeatherMaps()
                    }.getOrNull()
                }
                val ecmwfDeferred = async {
                    runCatching {
                        forecastApi.getSingleModelDailyForecast(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            model = "ecmwf_ifs025",
                            timezone = tz
                        )
                    }.getOrNull()
                }
                val gfsDeferred = async {
                    runCatching {
                        forecastApi.getSingleModelDailyForecast(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            model = "gfs_seamless",
                            timezone = tz
                        )
                    }.getOrNull()
                }
                val iconDeferred = async {
                    runCatching {
                        forecastApi.getSingleModelDailyForecast(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            model = "icon_seamless",
                            timezone = tz
                        )
                    }.getOrNull()
                }
                val nwsAlertsDeferred = async {
                    val isUsRegion = location.country.contains("United States", ignoreCase = true) ||
                        (location.latitude in 17.0..72.0 && location.longitude in -180.0..-64.0)
                    if (!isUsRegion) {
                        null
                    } else {
                        runCatching {
                            val pointStr = String.format(
                                Locale.US,
                                "%.4f,%.4f",
                                location.latitude,
                                location.longitude
                            )
                            nwsAlertsApi.getActiveAlertsForPoint(pointStr)
                        }.getOrNull()
                    }
                }

                val forecastResponse = forecastDeferred.await()
                val airQualityResponse = airQualityDeferred.await()
                val rainViewerResponse = rainViewerDeferred.await()
                val alertsResponse = nwsAlertsDeferred.await()

                val multiModelMap = buildMap<String, OpenMeteoForecastResponse> {
                    ecmwfDeferred.await()?.let { put("ECMWF IFS 0.25°", it) }
                    gfsDeferred.await()?.let { put("NOAA GFS Seamless", it) }
                    iconDeferred.await()?.let { put("DWD ICON Seamless", it) }
                }

                val now = System.currentTimeMillis()

                cacheDao.upsertCache(
                    WeatherCacheEntity(
                        locationId = location.id,
                        rawResponseJson = forecastAdapter.toJson(forecastResponse),
                        rawAirQualityJson = airQualityResponse?.let { airQualityAdapter.toJson(it) },
                        rawAlertsJson = alertsResponse?.let { alertsAdapter.toJson(it) },
                        fetchedAtMillis = now
                    )
                )

                val bundle = WeatherApiMapper.mapToWeatherBundle(
                    location = location,
                    response = forecastResponse,
                    airQualityResponse = airQualityResponse,
                    nwsAlertsResponse = alertsResponse,
                    rainViewerResponse = rainViewerResponse,
                    multiModelResponses = multiModelMap,
                    fetchedAtMillis = now,
                    isFromCache = false
                )

                val existingEntity = locationDao.getLocationById(location.id)
                if (existingEntity != null) {
                    locationDao.upsertLocation(
                        existingEntity.copy(
                            cachedTempC = bundle.current.temperatureC,
                            cachedCondition = bundle.current.conditionText,
                            cachedSceneName = bundle.current.sceneType.name
                        )
                    )
                }
                Result.success(bundle)
            }
        } catch (networkError: Exception) {
            val cached = loadFromCache(location)
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(networkError)
            }
        }
    }

    private suspend fun shouldFetchFromNetwork(locationId: String): Boolean {
        val cache = cacheDao.getCacheForLocation(locationId) ?: return true
        val ageMs = System.currentTimeMillis() - cache.fetchedAtMillis
        return ageMs > 10 * 60 * 1000L
    }

    suspend fun loadFromCache(location: SavedLocation): WeatherBundle? {
        val cache = cacheDao.getCacheForLocation(location.id) ?: return null
        return try {
            val dto = forecastAdapter.fromJson(cache.rawResponseJson) ?: return null
            val aqDto = cache.rawAirQualityJson?.let {
                runCatching { airQualityAdapter.fromJson(it) }.getOrNull()
            }
            val alertsDto = cache.rawAlertsJson?.let {
                runCatching { alertsAdapter.fromJson(it) }.getOrNull()
            }
            WeatherApiMapper.mapToWeatherBundle(
                location = location,
                response = dto,
                airQualityResponse = aqDto,
                nwsAlertsResponse = alertsDto,
                fetchedAtMillis = cache.fetchedAtMillis,
                isFromCache = true
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun searchCities(query: String): Result<List<SavedLocation>> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return Result.success(emptyList())
        return try {
            val response = geocodingApi.searchCities(query = trimmed, count = 10)
            val mapped = response.results.orEmpty().map { dto ->
                SavedLocation(
                    id = "geo_${dto.id}_${dto.latitude.roundToInt()}_${dto.longitude.roundToInt()}",
                    cityName = dto.name,
                    region = dto.admin1.orEmpty(),
                    country = dto.country.orEmpty(),
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    timezone = dto.timezone ?: "auto"
                )
            }
            Result.success(mapped)
        } catch (e: Exception) {
            val localMatches = (DefaultGlobalCities.hubs + DefaultGlobalCities.bangladeshFarmingHubs)
                .distinctBy { it.id }
                .filter {
                    it.cityName.contains(trimmed, ignoreCase = true) ||
                        it.country.contains(trimmed, ignoreCase = true)
                }
            if (localMatches.isNotEmpty()) {
                Result.success(localMatches)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun resolveGpsCoordinates(latitude: Double, longitude: Double): SavedLocation {
        val resolvedName = try {
            val rev = reverseGeocodingApi.reverseGeocode(latitude = latitude, longitude = longitude)
            val city = rev.city?.takeIf { it.isNotBlank() }
                ?: rev.locality?.takeIf { it.isNotBlank() }
                ?: "Current Location"
            val region = rev.principalSubdivision.orEmpty()
            val country = rev.countryName?.takeIf { it.isNotBlank() } ?: "GPS Coordinates"
            Triple(city, region, country)
        } catch (_: Exception) {
            Triple(
                "Current Location",
                "${String.format(Locale.US, "%.2f", latitude)}°, ${String.format(Locale.US, "%.2f", longitude)}°",
                "GPS Telemetry"
            )
        }

        val gpsLocation = SavedLocation(
            id = "gps_current_location",
            cityName = resolvedName.first,
            region = resolvedName.second,
            country = resolvedName.third,
            latitude = latitude,
            longitude = longitude,
            timezone = "auto",
            isCurrentLocation = true,
            isFavorite = true
        )
        locationDao.upsertLocation(gpsLocation.toEntity(sortOrder = -1))
        return gpsLocation
    }

    suspend fun saveLocation(location: SavedLocation) {
        val currentCount = locationDao.getAllLocationsOnce().size
        locationDao.upsertLocation(location.toEntity(sortOrder = currentCount))
    }

    suspend fun removeLocation(locationId: String) {
        locationDao.deleteLocationById(locationId)
    }
}

private fun SavedLocationEntity.toDomain(): SavedLocation = SavedLocation(
    id = id,
    cityName = cityName,
    region = region,
    country = country,
    latitude = latitude,
    longitude = longitude,
    timezone = timezone,
    isCurrentLocation = isCurrentLocation,
    isFavorite = isFavorite,
    cachedTempC = cachedTempC,
    cachedCondition = cachedCondition,
    cachedSceneType = cachedSceneName?.let {
        runCatching { WeatherSceneType.valueOf(it) }.getOrNull()
    }
)

private fun SavedLocation.toEntity(sortOrder: Int = 0): SavedLocationEntity = SavedLocationEntity(
    id = id,
    cityName = cityName,
    region = region,
    country = country,
    latitude = latitude,
    longitude = longitude,
    timezone = timezone,
    isCurrentLocation = isCurrentLocation,
    isFavorite = isFavorite,
    cachedTempC = cachedTempC,
    cachedCondition = cachedCondition,
    cachedSceneName = cachedSceneType?.name,
    sortOrder = sortOrder
)
