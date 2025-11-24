package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
// FIX: Menggunakan MoodCloudRepository
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository
import com.mindease.mindeaseapp.data.model.MoodEntry
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.Calendar

// Filter Type
enum class MoodFilter {
    ALL, WEEK, MONTH, YEAR
}

/**
 * ViewModel untuk MoodHistoryActivity.
 */
@OptIn(ExperimentalCoroutinesApi::class)
// FIX: Mengubah tipe repository yang diterima menjadi MoodCloudRepository
class MoodHistoryViewModel(private val repository: MoodCloudRepository) : ViewModel() {

    // Status filter yang saat ini aktif, default ke WEEK
    private val _filter = MutableStateFlow(MoodFilter.WEEK)
    // Dibuat public agar Activity dapat mengambil nilai filter saat ini (digunakan di setupChart)
    val filter: StateFlow<MoodFilter> = _filter // PROPERTI INI PENTING UNTUK DIGUNAKAN DI ACTIVITY

    // LiveData yang akan diperbarui secara reaktif berdasarkan filter
    val filteredMoods: LiveData<List<MoodEntry>> = _filter.flatMapLatest { moodFilter ->
        // FIX: Mengakses data dari MoodCloudRepository.getAllMoods()
        repository.getAllMoods().map { moods ->
            filterMoods(moods, moodFilter)
        }
    }.asLiveData()

    // Fungsi untuk mengubah filter
    fun setFilter(moodFilter: MoodFilter) {
        _filter.value = moodFilter
    }

    /**
     * Fungsi untuk melakukan filtering data mood berdasarkan rentang waktu.
     * Logika rentang waktu 7, 30, dan 365 hari dipertahankan.
     */
    private fun filterMoods(moods: List<MoodEntry>, filter: MoodFilter): List<MoodEntry> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        // Hitung waktu mulai berdasarkan filter
        val startTime: Long = when (filter) {
            MoodFilter.ALL -> 0L // Semua waktu
            MoodFilter.WEEK -> {
                // Rentang waktu 7 hari ke belakang (termasuk hari ini)
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            MoodFilter.MONTH -> {
                // Rentang waktu 30 hari ke belakang (termasuk hari ini)
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            MoodFilter.YEAR -> {
                // Rentang waktu 365 hari ke belakang (termasuk hari ini)
                calendar.add(Calendar.DAY_OF_YEAR, -364)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }

        // Filter dan sort data (data dari Cloud sudah DESCENDING, kita balik ke ASCENDING untuk chart)
        return moods
            .filter { it.timestamp >= startTime }
            .sortedBy { it.timestamp } // Urutkan kembali berdasarkan timestamp (ASCENDING)
    }
}