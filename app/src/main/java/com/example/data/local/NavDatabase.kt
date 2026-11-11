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

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val address: String,
    val category: String, // "常用", "书签", "枢纽"
    val latitude: Double, // WGS-84
    val longitude: Double, // WGS-84
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "route_history")
data class RouteHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startName: String,
    val destName: String,
    val destAddress: String,
    val destLat: Double,
    val destLon: Double,
    val travelMode: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val engineUsed: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface NavDao {
    @Query("SELECT * FROM saved_places ORDER BY id ASC")
    fun getAllSavedPlaces(): Flow<List<SavedPlaceEntity>>

    @Query("SELECT COUNT(*) FROM saved_places")
    suspend fun getSavedPlacesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPlace(place: SavedPlaceEntity)

    @Query("DELETE FROM saved_places WHERE id = :id")
    suspend fun deleteSavedPlaceById(id: Int)

    @Query("SELECT * FROM route_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentRoutes(): Flow<List<RouteHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteHistory(route: RouteHistoryEntity)

    @Query("DELETE FROM route_history WHERE id = :id")
    suspend fun deleteRouteHistoryById(id: Int)

    @Query("DELETE FROM route_history")
    suspend fun clearAllHistory()
}

@Database(
    entities = [SavedPlaceEntity::class, RouteHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NavDatabase : RoomDatabase() {
    abstract fun navDao(): NavDao

    companion object {
        @Volatile
        private var INSTANCE: NavDatabase? = null

        fun getInstance(context: Context): NavDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NavDatabase::class.java,
                    "purenav_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class NavRepository(private val dao: NavDao) {
    val savedPlaces: Flow<List<SavedPlaceEntity>> = dao.getAllSavedPlaces()
    val recentRoutes: Flow<List<RouteHistoryEntity>> = dao.getRecentRoutes()

    suspend fun ensureDefaultPresets() {
        if (dao.getSavedPlacesCount() == 0) {
            val defaults = listOf(
                SavedPlaceEntity(
                    title = "王府井步行街",
                    address = "北京市东城区王府井大街255号（步行/骑行友好）",
                    category = "书签",
                    latitude = 39.9143,
                    longitude = 116.4114
                ),
                SavedPlaceEntity(
                    title = "故宫博物院（午门）",
                    address = "北京市东城区景山前街4号",
                    category = "书签",
                    latitude = 39.9163,
                    longitude = 116.3972
                ),
                SavedPlaceEntity(
                    title = "北京南站",
                    address = "北京市丰台区永外大街车站路12号",
                    category = "枢纽",
                    latitude = 39.8651,
                    longitude = 116.3785
                ),
                SavedPlaceEntity(
                    title = "中关村软件园",
                    address = "北京市海淀区东北旺西路8号",
                    category = "常用",
                    latitude = 40.0442,
                    longitude = 116.2869
                )
            )
            defaults.forEach { dao.insertSavedPlace(it) }
        }
    }

    suspend fun savePlace(place: SavedPlaceEntity) = dao.insertSavedPlace(place)

    suspend fun deletePlace(id: Int) = dao.deleteSavedPlaceById(id)

    suspend fun recordRoute(route: RouteHistoryEntity) = dao.insertRouteHistory(route)

    suspend fun deleteRoute(id: Int) = dao.deleteRouteHistoryById(id)

    suspend fun clearHistory() = dao.clearAllHistory()
}
