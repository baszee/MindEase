package com.mindease.mindeaseapp.ui.journal

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.AppDatabase
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.data.repository.JournalRepository
import com.mindease.mindeaseapp.databinding.ActivityAddJournalBinding
import java.util.Date
import android.content.Intent // FIX: Wajib untuk FLAG

class AddJournalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddJournalBinding
    private lateinit var viewModel: JournalViewModel
    private var selectedMoodScore: Int = 0
    private var selectedMoodName: String = ""

    private var selectedImageUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddJournalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        // FIX: Inisialisasi Launcher dengan logika Izin Persisten
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                // FIX: Ambil izin persisten agar URI dapat dibaca nanti
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedImageUri = uri
                binding.ivImagePreview.setImageURI(uri)
                binding.ivImagePreview.visibility = View.VISIBLE
            } else {
                selectedImageUri = null
                binding.ivImagePreview.visibility = View.GONE
            }
        }


        // 2. Setup Toolbar (Tombol Back)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 3. Setup Listeners
        setupMoodListeners()
        binding.btnSaveJournal.setOnClickListener {
            showSaveConfirmationDialog()
        }

        binding.btnAddPicture.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun setupViewModel() {
        val journalDao = AppDatabase.getDatabase(applicationContext).journalDao()
        val repository = JournalRepository(journalDao)

        val factory = JournalViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[JournalViewModel::class.java]
    }

    private fun setupMoodListeners() {
        val moodIcons = listOf(
            binding.ivMoodHappyExtreme, binding.ivMoodHappy, binding.ivMoodNeutral,
            binding.ivMoodSad, binding.ivMoodSadExtreme
        )

        moodIcons.forEach { imageView ->
            imageView.setOnClickListener { view ->
                val score = view.tag.toString().toIntOrNull() ?: return@setOnClickListener

                onMoodSelected(score, view as ImageView)
            }
        }
    }

    private fun onMoodSelected(score: Int, selectedView: ImageView) {
        resetMoodSelection()

        selectedView.alpha = 1.0f

        selectedMoodScore = score
        selectedMoodName = when (score) {
            5 -> "Very Happy"
            4 -> "Happy"
            3 -> "Neutral"
            2 -> "Sad"
            1 -> "Very Sad"
            else -> ""
        }
        binding.tvMoodName.text = selectedMoodName
    }

    /**
     * Mengatur ulang alpha semua ikon mood menjadi 0.5 (belum dipilih).
     */
    private fun resetMoodSelection() {
        binding.moodSelectionContainer.children.filterIsInstance<ImageView>().forEach { imageView ->
            imageView.alpha = 0.5f
        }
    }

    private fun showSaveConfirmationDialog() {
        val content = binding.etJournalContent.text.toString()

        if (selectedMoodScore == 0) {
            Toast.makeText(this, "Mohon pilih Mood Anda hari ini.", Toast.LENGTH_SHORT).show()
            return
        }

        if (content.isBlank()) {
            Toast.makeText(this, "Tuliskan sedikit tentang hari Anda.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Menyimpan Jurnal...", Toast.LENGTH_LONG).show()
        saveJournalEntry(content)
    }

    /**
     * Menyimpan data Jurnal ke Room Database.
     */
    private fun saveJournalEntry(content: String) {
        val newJournalEntry = JournalEntry(
            moodScore = selectedMoodScore,
            moodName = selectedMoodName,
            content = content,
            // Menyimpan URI gambar sebagai String path di database
            imagePath = selectedImageUri?.toString(),
            timestamp = Date().time
        )

        viewModel.insertJournalEntry(newJournalEntry)

        Toast.makeText(this, "Jurnal berhasil disimpan!", Toast.LENGTH_SHORT).show()

        // Tutup Activity
        finish()
    }
}