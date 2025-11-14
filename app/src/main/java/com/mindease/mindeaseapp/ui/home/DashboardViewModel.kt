package com.mindease.mindeaseapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindease.mindeaseapp.data.model.MoodEntry
import com.mindease.mindeaseapp.data.repository.MoodRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.core.view.children
import java.util.concurrent.TimeUnit

/**
 * ViewModel untuk DashboardFragment (Home).
 * Menangani logika tampilan mood harian dan penyimpanan mood baru.
 */
class DashboardViewModel(private val repository: MoodRepository) : ViewModel() {

    // LiveData untuk menyimpan mood yang terakhir dicatat hari ini
    private val _currentDayMood = MutableLiveData<MoodEntry?>()
    val currentDayMood: LiveData<MoodEntry?> = _currentDayMood

    init {
        // Coba muat mood hari ini saat ViewModel pertama kali dibuat
        loadMoodForToday()
    }

    /**
     * Menyimpan MoodEntry baru ke database dan memperbarui LiveData.
     */
    fun saveMood(mood: MoodEntry) {
        viewModelScope.launch {
            repository.insertMood(mood)
            // Setelah disimpan, perbarui tampilan mood hari ini
            _currentDayMood.value = mood
        }
    }

    /**
     * Memuat mood yang dicatat pengguna hari ini.
     */
    fun loadMoodForToday() {
        viewModelScope.launch {
            // Hitung batas waktu mulai dan akhir hari (00:00:00 hingga 23:59:59)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()

            // Atur waktu ke awal hari (StartOfDay)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            // Atur waktu ke akhir hari (EndOfDay)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = calendar.timeInMillis

            // Panggil repository untuk mendapatkan mood hari ini
            val mood = repository.getMoodForToday(startOfDay, endOfDay)
            _currentDayMood.postValue(mood)
        }
    }
}