package com.mindease.mindeaseapp.data.repository

import com.mindease.mindeaseapp.data.model.MoodDao
import com.mindease.mindeaseapp.data.model.MoodEntry

/**
 * Repository yang bertanggung jawab untuk menangani operasi data Mood.
 * Bertindak sebagai perantara antara ViewModel dan sumber data (DAO).
 */
class MoodRepository(private val moodDao: MoodDao) {

    /** Mendapatkan semua mood yang dicatat sebagai Flow (untuk pembaruan real-time) */
    val allMoods = moodDao.getAllMoods()

    /**
     * Menyimpan mood baru di database.
     * @param mood data MoodEntry yang akan disimpan.
     */
    suspend fun insert(mood: MoodEntry) {
        moodDao.insertMood(mood)
    }

    /**
     * Mendapatkan Mood hari ini (Implementasi membutuhkan logika tanggal yang lebih detail).
     */
    suspend fun getTodayMood(): MoodEntry? {
        // Logika sederhana untuk mendapatkan awal dan akhir hari ini
        val startOfDay = System.currentTimeMillis() - 86400000 // Contoh sederhana: 24 jam terakhir
        val endOfDay = System.currentTimeMillis()

        // TODO: Ganti dengan logika yang lebih akurat (menggunakan Calendar atau LocalDateTime)
        return moodDao.getMoodForToday(startOfDay, endOfDay)
    }
}