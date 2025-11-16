package com.mindease.mindeaseapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindease.mindeaseapp.data.model.MoodEntry
import com.mindease.mindeaseapp.data.model.Quote
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository
import com.mindease.mindeaseapp.data.repository.QuoteRepository
import com.mindease.mindeaseapp.data.repository.AuthRepository
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel untuk DashboardFragment (Home).
 * Menangani logika tampilan mood harian dan penyimpanan mood baru, serta memuat Quote.
 */
class DashboardViewModel(
    private val moodRepository: MoodCloudRepository,
    private val quoteRepository: QuoteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // LiveData untuk menyimpan mood yang terakhir dicatat hari ini
    private val _currentDayMood = MutableLiveData<MoodEntry?>()
    val currentDayMood: LiveData<MoodEntry?> = _currentDayMood

    // BARU: LiveData untuk menyimpan kutipan harian
    private val _currentQuote = MutableLiveData<Quote>()
    val currentQuote: LiveData<Quote> = _currentQuote

    init {
        // Coba muat mood hari ini saat ViewModel pertama kali dibuat
        loadMoodForToday()
        // BARU: Muat kutipan saat ViewModel dibuat
        loadRandomQuote()
    }

    // FIX: Fungsi baru untuk mendapatkan nama terbaru dari server
    suspend fun getUpdatedUserName(): String {
        authRepository.reloadCurrentUser()
        return authRepository.getCurrentUserName() ?: "User MindEase"
    }


    // BARU: Fungsi untuk memuat kutipan acak dari Repository
    fun loadRandomQuote() {
        viewModelScope.launch {
            val quote = quoteRepository.getRandomQuote()
            _currentQuote.postValue(quote)
        }
    }

    /**
     * Menyimpan MoodEntry baru ke Cloud (Firestore) dan memperbarui LiveData.
     * 🔥 FIX: Mengimplementasikan aturan ONE MOOD PER DAY (Keep the Latest).
     */
    fun saveMood(mood: MoodEntry) {
        viewModelScope.launch {
            // 1. Dapatkan batas waktu hari ini (sama seperti di loadMoodForToday)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            val endOfDay = calendar.timeInMillis

            // 2. Cek apakah mood hari ini sudah ada
            val existingMood = moodRepository.getMoodForToday(startOfDay, endOfDay)

            // 3. Jika sudah ada, gunakan ID dokumen yang sudah ada untuk menimpa data (overwrite)
            val moodToSave = if (existingMood != null) {
                // Gunakan documentId yang sudah ada untuk memaksa UPDATE (bukan CREATE baru)
                mood.copy(documentId = existingMood.documentId)
            } else {
                // Mood baru, biarkan Repository yang membuat ID
                mood
            }

            // 4. Simpan/Timpa ke Cloud
            val savedMood = moodRepository.saveMood(moodToSave)

            // 5. Perbarui LiveData dan paksa refresh Mood for Today
            _currentDayMood.postValue(savedMood)
            loadMoodForToday()
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

            // Panggil repository Cloud untuk mendapatkan mood hari ini
            val mood = moodRepository.getMoodForToday(startOfDay, endOfDay)
            _currentDayMood.postValue(mood)
        }
    }
}