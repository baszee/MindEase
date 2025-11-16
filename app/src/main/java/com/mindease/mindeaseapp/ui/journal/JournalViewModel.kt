package com.mindease.mindeaseapp.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository
import com.mindease.mindeaseapp.data.model.JournalEntry
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.net.Uri // Diperlukan untuk saveJournalEntry

/**
 * ViewModel untuk JournalFragment, AddJournalActivity, dan DetailJournalActivity.
 */
class JournalViewModel(
    private val repository: JournalCloudRepository
) : ViewModel() {

    // Daftar semua jurnal, diambil secara real-time (Flow) dari Cloud Repository dan diubah ke LiveData
    val allJournals: LiveData<List<JournalEntry>> = repository.getAllJournals().asLiveData()

    /**
     * Menyimpan atau mengupdate entri jurnal (digunakan di AddJournalActivity).
     * FIX 1: Menggunakan nama 'saveJournalEntry' dan tipe 'Uri?' yang benar. (Memperbaiki error 3)
     */
    fun saveJournalEntry(journal: JournalEntry, imageUri: Uri? = null) {
        viewModelScope.launch {
            repository.saveJournal(journal, imageUri)
        }
    }

    // Catatan: Fungsi getJournalById di Repository adalah 'suspend fun',
    // jadi sebaiknya dipanggil di coroutine scope Activity, bukan dibungkus LiveData di ViewModel.
    // Oleh karena itu, kita HAPUS fungsi getJournalById yang bermasalah di sini.


    /**
     * Menghapus entri jurnal dari Cloud (digunakan di DetailJournalActivity).
     * FIX 2: Menambahkan fungsi yang sesuai dengan panggilan di DetailJournalActivity. (Memperbaiki error 2)
     */
    fun deleteJournalEntry(journal: JournalEntry) {
        viewModelScope.launch {
            repository.deleteJournal(journal)
        }
    }
}