package com.mindease.mindeaseapp.ui.journal

import android.net.Uri // Import untuk Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide // FIX: Import Glide
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.AppDatabase
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.data.repository.JournalRepository
import com.mindease.mindeaseapp.databinding.ActivityDetailJournalBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailJournalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailJournalBinding
    private lateinit var repository: JournalRepository

    companion object {
        const val EXTRA_JOURNAL_ID = "extra_journal_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailJournalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 1. Inisialisasi Repository
        val journalDao = AppDatabase.getDatabase(applicationContext).journalDao()
        repository = JournalRepository(journalDao)

        // 2. Ambil ID dari Intent
        val journalId = intent.getIntExtra(EXTRA_JOURNAL_ID, -1)

        if (journalId != -1) {
            loadJournalDetail(journalId)
        } else {
            Toast.makeText(this, "ID Jurnal tidak valid.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadJournalDetail(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val journal = repository.getJournalById(id)
            withContext(Dispatchers.Main) {
                if (journal != null) {
                    displayJournal(journal)
                } else {
                    Toast.makeText(this@DetailJournalActivity, "Entri jurnal tidak ditemukan.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    /**
     * Menampilkan data jurnal ke UI.
     */
    private fun displayJournal(journal: JournalEntry) {
        binding.apply {
            // Data Teks
            tvMoodNameDetail.text = journal.moodName
            tvDateDetail.text = formatDate(journal.timestamp)
            tvContentDetail.text = journal.content

            // Ikon Mood
            ivMoodIconDetail.setImageResource(getMoodIconResource(journal.moodScore))

            // FIX: Logika untuk memuat gambar penuh menggunakan Glide
            if (journal.imagePath != null && journal.imagePath.isNotEmpty()) {
                ivJournalImageDetail.visibility = View.VISIBLE
                Glide.with(this@DetailJournalActivity)
                    .load(Uri.parse(journal.imagePath))
                    .centerCrop()
                    .placeholder(R.drawable.ic_add_image)
                    .into(ivJournalImageDetail)
            } else {
                ivJournalImageDetail.visibility = View.GONE
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val locale = Locale.Builder().setLanguage("id").setRegion("ID").build()
        val sdf = SimpleDateFormat("dd MMMM yyyy", locale)
        return sdf.format(Date(timestamp))
    }

    private fun getMoodIconResource(score: Int): Int {
        return when (score) {
            5 -> R.drawable.ic_mood_happy_extreme
            4 -> R.drawable.ic_mood_happy
            3 -> R.drawable.ic_mood_neutral
            2 -> R.drawable.ic_mood_sad
            1 -> R.drawable.ic_mood_sad_extreme
            else -> R.drawable.ic_mood_neutral
        }
    }
}