package com.firepulse.app.data.local

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
import androidx.room.Transaction

@Entity(tableName = "fire_observations")
data class FireObservationEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val acquisitionDate: String,
    val acquisitionTime: String?,
    val satellite: String?,
    val instrument: String?,
    val confidence: String?,
    val frp: Double?,
    val brightness: Double?,
    val daynight: String?,
    val cachedAt: Long
)

@Dao
interface FireObservationDao {
    @Query("SELECT * FROM fire_observations ORDER BY acquisitionDate DESC, acquisitionTime DESC LIMIT 5000")
    suspend fun getRecent(): List<FireObservationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FireObservationEntity>)

    @Query("DELETE FROM fire_observations")
    suspend fun clear()

    @Query("DELETE FROM fire_observations WHERE cachedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Transaction
    suspend fun replaceSnapshot(items: List<FireObservationEntity>) {
        clear()
        if (items.isNotEmpty()) insertAll(items)
    }
}

@Database(entities = [FireObservationEntity::class], version = 1, exportSchema = false)
abstract class FirePulseDatabase : RoomDatabase() {
    abstract fun fireObservationDao(): FireObservationDao

    companion object {
        @Volatile private var INSTANCE: FirePulseDatabase? = null

        fun getInstance(context: Context): FirePulseDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FirePulseDatabase::class.java,
                    "firepulse-cache.db"
                ).build().also { INSTANCE = it }
            }
    }
}

fun com.firepulse.app.data.remote.FireObservation.toEntity(cachedAt: Long) =
    FireObservationEntity(
        id = listOf(latitude.toString(), longitude.toString(), acquisition_date,
            acquisition_time.orEmpty(), satellite.orEmpty()).joinToString("|"),
        latitude = latitude,
        longitude = longitude,
        acquisitionDate = acquisition_date,
        acquisitionTime = acquisition_time,
        satellite = satellite,
        instrument = instrument,
        confidence = confidence,
        frp = frp,
        brightness = brightness,
        daynight = daynight,
        cachedAt = cachedAt
    )

fun FireObservationEntity.toObservation() =
    com.firepulse.app.data.remote.FireObservation(
        latitude = latitude,
        longitude = longitude,
        acquisition_date = acquisitionDate,
        acquisition_time = acquisitionTime,
        satellite = satellite,
        instrument = instrument,
        confidence = confidence,
        frp = frp,
        brightness = brightness,
        daynight = daynight
    )
