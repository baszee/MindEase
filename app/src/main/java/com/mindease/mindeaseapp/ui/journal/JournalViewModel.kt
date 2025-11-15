package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository // GANTI IMPORT INI
import kotlinx.coroutines.launch
import android.net.Uri // Tambahkan ini

/**
 * ViewModel untuk JournalFragment.
 * Sekarang menggunakan JournalCloudRepository.
 */
class JournalViewModel(private val repository: JournalCloudRepository) : ViewModel() { // GANTI TIPE REPOSITORY

    // Daftar semua jurnal, dikonversi dari Flow (Firestore) ke LiveData
    val allJournals = repository.getAllJournals().asLiveData()

    /**
     * Menyimpan entri jurnal baru ke cloud dan database.
     */
    fun saveJournalEntry(journal: JournalEntry, imageUri: Uri? = null) {
        viewModelScope.launch {
            // Repository menangani apakah ini insert baru atau update
            repository.saveJournal(journal, imageUri)
        }
    }

    /**
     * Fungsi yang sama dengan saveJournalEntry, hanya untuk kejelasan kode di Activity.
     */
    fun updateJournalEntry(journal: JournalEntry, imageUri: Uri? = null) {
        saveJournalEntry(journal, imageUri)
    }

    /**
     * Menghapus entri jurnal dari cloud.
     */
    fun deleteJournalEntry(journal: JournalEntry) {
        viewModelScope.launch {
            repository.deleteJournal(journal)
        }
    }
}