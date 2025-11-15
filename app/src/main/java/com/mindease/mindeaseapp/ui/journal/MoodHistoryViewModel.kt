package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.mindease.mindeaseapp.data.repository.MoodRepository
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
class MoodHistoryViewModel(private val repository: MoodRepository) : ViewModel() {

    // Status filter yang saat ini aktif, default ke WEEK
    private val _filter = MutableStateFlow(MoodFilter.WEEK)
    val filter: StateFlow<MoodFilter> = _filter

    // LiveData yang akan diperbarui secara reaktif berdasarkan filter
    val filteredMoods: LiveData<List<MoodEntry>> = _filter.flatMapLatest { moodFilter ->
        repository.allMoods.map { moods ->
            filterMoods(moods, moodFilter)
        }
    }.asLiveData()

    // Fungsi untuk mengubah filter
    fun setFilter(moodFilter: MoodFilter) {
        _filter.value = moodFilter
    }

    /**
     * Fungsi untuk melakukan filtering data mood berdasarkan rentang waktu.
     */
    private fun filterMoods(moods: List<MoodEntry>, filter: MoodFilter): List<MoodEntry> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        // Hitung waktu mulai berdasarkan filter
        val startTime: Long = when (filter) {
            MoodFilter.ALL -> 0L // Semua waktu
            MoodFilter.WEEK -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            MoodFilter.MONTH -> {
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            MoodFilter.YEAR -> {
                calendar.add(Calendar.DAY_OF_YEAR, -364)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }

        // Filter dan sort data
        return moods
            .filter { it.timestamp >= startTime }
            .sortedBy { it.timestamp }
    }
}