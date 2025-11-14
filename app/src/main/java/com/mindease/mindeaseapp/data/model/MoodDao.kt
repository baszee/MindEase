// app/src/main/java/com/mindease/mindeaseapp/data/model/MoodDao.kt
package com.mindease.mindeaseapp.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntry)

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllMoods(): Flow<List<MoodEntry>>

    // Fungsi yang dipanggil oleh Repository untuk mendapatkan mood hari ini
    @Query("SELECT * FROM mood_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay LIMIT 1")
    suspend fun getMoodForToday(startOfDay: Long, endOfDay: Long): MoodEntry?
}