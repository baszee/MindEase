package com.mindease.mindeaseapp.ui.journal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.data.repository.JournalCloudRepository // GANTI INI
import com.mindease.mindeaseapp.databinding.ActivityAddJournalBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import androidx.core.widget.ImageViewCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth // Tambahkan ini
import com.google.firebase.firestore.FirebaseFirestore // Tambahkan ini
import com.google.firebase.storage.FirebaseStorage // Tambahkan ini

class AddJournalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddJournalBinding
    private lateinit var viewModel: JournalViewModel
    private lateinit var cloudRepository: JournalCloudRepository

    private var currentJournalDocumentId: String? = null // GANTI DARI ID INT KE DOC ID STRING
    private var selectedMoodScore: Int = 0
    private var selectedMoodName: String = ""
    private var selectedImageUri: Uri? = null // URI lokal baru yang akan diupload
    private var existingImagePath: String? = null // URL cloud yang sudah ada

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    companion object {
        const val EXTRA_JOURNAL_ID = "extra_journal_document_id_to_edit" // GANTI NAMA EXTRA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddJournalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        currentJournalDocumentId = intent.getStringExtra(EXTRA_JOURNAL_ID)

        if (currentJournalDocumentId != null) {
            loadExistingJournal(currentJournalDocumentId!!)
            binding.toolbar.title = "Edit Journal"
        } else {
            binding.toolbar.title = "Add Journal"
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedImageUri = uri
                binding.ivImagePreview.setImageURI(uri)
                binding.ivImagePreview.visibility = View.VISIBLE
            } else {
                selectedImageUri = null
                // Jika URI dibatalkan, kita tetap mempertahankan gambar yang sudah ada (jika ada)
                if (existingImagePath == null) {
                    binding.ivImagePreview.visibility = View.GONE
                }
            }
        }


        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupMoodListeners()
        binding.btnSaveJournal.setOnClickListener {
            showSaveConfirmationDialog()
        }

        binding.btnAddPicture.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun setupViewModel() {
        // INISIALISASI CLOUD REPOSITORY
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val auth = FirebaseAuth.getInstance()

        val repo = JournalCloudRepository(firestore, storage, auth)
        cloudRepository = repo

        val factory = JournalViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(JournalViewModel::class.java)
    }

    private fun loadExistingJournal(documentId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val journal = cloudRepository.getJournalById(documentId)
            withContext(Dispatchers.Main) {
                if (journal != null) {
                    // Jurnal yang dimuat
                    onMoodSelected(journal.moodScore, getMoodImageView(journal.moodScore))
                    binding.etJournalContent.setText(journal.content)

                    if (journal.imagePath != null && journal.imagePath.isNotEmpty()) {
                        existingImagePath = journal.imagePath // Simpan URL cloud yang sudah ada

                        binding.ivImagePreview.visibility = View.VISIBLE
                        // Gunakan Glide untuk memuat dari URL cloud
                        Glide.with(this@AddJournalActivity).load(journal.imagePath).into(binding.ivImagePreview)
                    } else {
                        existingImagePath = null
                        binding.ivImagePreview.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@AddJournalActivity, "Gagal memuat jurnal.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun getMoodImageView(score: Int): ImageView? {
        return when (score) {
            5 -> binding.ivMoodHappyExtreme
            4 -> binding.ivMoodHappy
            3 -> binding.ivMoodNeutral
            2 -> binding.ivMoodSad
            1 -> binding.ivMoodSadExtreme
            else -> null
        }
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
        resetMoodSelection()
    }

    private fun onMoodSelected(score: Int, selectedView: ImageView?) {
        resetMoodSelection()

        if (selectedView != null) {
            selectedView.alpha = 1.0f
            val moodColor = getMoodColor(score)
            ImageViewCompat.setImageTintList(selectedView, ContextCompat.getColorStateList(this, moodColor))
        }

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

    private fun resetMoodSelection() {
        val moodViewScores = mapOf(
            binding.ivMoodHappyExtreme to 5,
            binding.ivMoodHappy to 4,
            binding.ivMoodNeutral to 3,
            binding.ivMoodSad to 2,
            binding.ivMoodSadExtreme to 1
        )

        moodViewScores.forEach { (imageView, score) ->
            imageView.alpha = 0.5f
            val moodColor = getMoodColor(score)
            ImageViewCompat.setImageTintList(imageView, ContextCompat.getColorStateList(this, moodColor))
        }
    }

    private fun getMoodColor(score: Int): Int {
        return when (score) {
            5 -> R.color.mood_very_happy
            4 -> R.color.mood_happy
            3 -> R.color.mood_neutral
            2 -> R.color.mood_sad
            else -> R.color.mood_very_sad
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

        // Pastikan pengguna terautentikasi sebelum menyimpan
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Anda harus login untuk menyimpan jurnal.", Toast.LENGTH_LONG).show()
            return
        }

        val action = if (currentJournalDocumentId != null) "Memperbarui" else "Menyimpan"
        Toast.makeText(this, "$action Jurnal...", Toast.LENGTH_LONG).show()
        saveOrUpdateJournalEntry(content)
    }

    private fun saveOrUpdateJournalEntry(content: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val entry = JournalEntry(
            documentId = currentJournalDocumentId, // Document ID dari Firestore
            userId = currentUser.uid,
            moodScore = selectedMoodScore,
            moodName = selectedMoodName,
            content = content,
            // Jika ada gambar baru (selectedImageUri), akan di-upload.
            // Jika tidak, path lama (existingImagePath) akan dipertahankan atau dihapus (jika dihapus di UI).
            imagePath = existingImagePath,
            timestamp = Date().time
        )

        // Panggil ViewModel untuk menyimpan ke Cloud, termasuk upload gambar
        viewModel.saveJournalEntry(entry, selectedImageUri)

        val actionMessage = if (currentJournalDocumentId != null) "diperbarui" else "disimpan"
        Toast.makeText(this, "Jurnal berhasil $actionMessage!", Toast.LENGTH_SHORT).show()
        finish()
    }
}