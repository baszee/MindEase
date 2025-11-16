package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository
import com.mindease.mindeaseapp.data.model.JournalEntry
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.net.Uri

/**
 * ViewModel untuk JournalFragment, AddJournalActivity, dan DetailJournalActivity.
 */
class JournalViewModel(
    private val repository: JournalCloudRepository
) : ViewModel() {

    // Daftar semua jurnal, diambil secara real-time (Flow) dari Cloud Repository dan diubah ke LiveData
    val allJournals: LiveData<List<JournalEntry>> = repository.getAllJournals().asLiveData() // 🔥 FIX: Menggunakan repository.getAllJournals()

    /**
     * Menyimpan atau mengupdate entri jurnal.
     * FIX: Menerima base64String (String?) alih-alih imageUri.
     */
    fun saveJournalEntry(journal: JournalEntry, imageBase64: String? = null) {
        viewModelScope.launch {
            // Kita harus membuat objek JournalEntry final di sini.
            val finalJournal = journal.copy(imageBase64 = imageBase64)
            repository.saveJournal(finalJournal) // Panggil repository yang sudah diupdate
        }
    }

    /**
     * Menghapus entri jurnal dari Cloud.
     */
    fun deleteJournalEntry(journal: JournalEntry) {
        viewModelScope.launch {
            repository.deleteJournal(journal)
        }
    }
}