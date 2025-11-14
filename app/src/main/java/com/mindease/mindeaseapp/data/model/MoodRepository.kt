// app/src/main/java/com/mindease/mindeaseapp/data/repository/MoodRepository.kt
package com.mindease.mindeaseapp.data.repository

import com.mindease.mindeaseapp.data.model.MoodDao
import com.mindease.mindeaseapp.data.model.MoodEntry
import kotlinx.coroutines.flow.Flow

class MoodRepository(private val moodDao: MoodDao) {

    // Menggunakan LiveData di ViewModel untuk memicu UI update
    val allMoods: Flow<List<MoodEntry>> = moodDao.getAllMoods()

    /**
     * Menyimpan MoodEntry baru. Disesuaikan agar panggilannya hanya 'insert'
     * untuk konsistensi dengan Room.
     */
    suspend fun insert(mood: MoodEntry) {
        moodDao.insertMood(mood)
    }

    /**
     * Mendapatkan Mood hari ini berdasarkan rentang waktu.
     */
    suspend fun getMoodForToday(startOfDay: Long, endOfDay: Long): MoodEntry? {
        return moodDao.getMoodForToday(startOfDay, endOfDay)
    }
}