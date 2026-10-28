package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey val id: String,
    val cityName: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val isCurrentLocation: Boolean,
    val isFavorite: Boolean,
    val cachedTempC: Double?,
    val cachedCondition: String?,
    val cachedSceneName: String?,
    val sortOrder: Int = 0
)

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val locationId: String,
    val rawResponseJson: String,
    val rawAirQualityJson: String? = null,
    val rawAlertsJson: String? = null,
    val fetchedAtMillis: Long
)

@Dao
interface SavedLocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY isCurrentLocation DESC, sortOrder ASC, cityName ASC")
    fun observeAllLocations(): Flow<List<SavedLocationEntity>>

    @Query("SELECT * FROM saved_locations ORDER BY isCurrentLocation DESC, sortOrder ASC")
    suspend fun getAllLocationsOnce(): List<SavedLocationEntity>

    @Query("SELECT * FROM saved_locations WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: String): SavedLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(location: SavedLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocations(locations: List<SavedLocationEntity>)

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: String)
}

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE locationId = :locationId LIMIT 1")
    suspend fun getCacheForLocation(locationId: String): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCache(cache: WeatherCacheEntity)
}

@Database(
    entities = [SavedLocationEntity::class, WeatherCacheEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AtmosDatabase : RoomDatabase() {
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun weatherCacheDao(): WeatherCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AtmosDatabase? = null

        fun getInstance(context: Context): AtmosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtmosDatabase::class.java,
                    "atmos_3d_weather.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
