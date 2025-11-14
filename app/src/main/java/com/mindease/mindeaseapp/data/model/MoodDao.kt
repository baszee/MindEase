package com.mindease.mindeaseapp.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Antarmuka untuk mengakses dan memanipulasi data MoodEntry.
 */
@Dao
interface MoodDao {

    /** Menyimpan MoodEntry baru. Jika sudah ada, ganti (OnConflictStrategy.REPLACE). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(mood: MoodEntry)

    /** Mengambil semua riwayat mood yang diurutkan berdasarkan waktu terbaru (untuk riwayat/grafik). */
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllMoods(): Flow<List<MoodEntry>>

    /** Mengambil mood spesifik hari ini berdasarkan rentang waktu. */
    @Query("SELECT * FROM mood_entries WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay LIMIT 1")
    suspend fun getMoodForToday(startOfDay: Long, endOfDay: Long): MoodEntry?
}