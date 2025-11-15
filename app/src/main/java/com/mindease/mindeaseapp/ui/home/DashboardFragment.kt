package com.mindease.mindeaseapp.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.MoodEntry
import com.mindease.mindeaseapp.databinding.FragmentDashboardBinding
import com.mindease.mindeaseapp.ui.journal.MoodHistoryActivity
import java.util.Calendar
import androidx.core.widget.ImageViewCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository
import com.mindease.mindeaseapp.data.repository.QuoteRepository // BARU: Import Quote Repository

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private var selectedMoodScore: Int? = null
    private var selectedMoodName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inisialisasi Database dan ViewModel (FIXED untuk menggunakan Cloud + Quotes)
        val firestore = FirebaseFirestore.getInstance()
        val auth = Firebase.auth

        // Buat instance dari kedua repository
        val moodRepository = MoodCloudRepository(firestore, auth)
        val quoteRepository = QuoteRepository(firestore) // BARU: Inisialisasi Quote Repository

        // Berikan kedua repository ke Factory
        val factory = DashboardViewModelFactory(moodRepository, quoteRepository) // FIX: Mengganti parameter factory
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        // 2. Siapkan Listener
        setupMoodListeners()
        setupObservers()
        setupGreeting()

        // Listener untuk Mood History
        binding.tvMoodHistoryLink.setOnClickListener {
            val intent = Intent(requireContext(), MoodHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Menampilkan sapaan dinamis berdasarkan waktu dan nama pengguna.
     */
    private fun setupGreeting() {
        val user = Firebase.auth.currentUser
        val userName = user?.displayName ?: "User MindEase"

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting: String
        val emoji: String

        when (hour) {
            in 5..11 -> {
                greeting = getString(R.string.good_morning)
                emoji = "🌤️"
            }
            in 12..17 -> {
                greeting = getString(R.string.good_afternoon)
                emoji = "☀️"
            }
            in 18..23 -> {
                greeting = getString(R.string.good_evening)
                emoji = "🌙"
            }
            else -> {
                greeting = getString(R.string.good_night)
                emoji = "🌌"
            }
        }
        binding.tvGreeting.text = "$greeting, $userName $emoji"
    }

    private fun setupMoodListeners() {
        val moodViews = mapOf(
            binding.ivMoodHappyExtreme to 5,
            binding.ivMoodHappy to 4,
            binding.ivMoodNeutral to 3,
            binding.ivMoodSad to 2,
            binding.ivMoodSadExtreme to 1
        )

        moodViews.forEach { entry ->
            entry.key.setOnClickListener {
                onMoodSelected(entry.value, getMoodName(entry.value))
            }
        }
    }

    /**
     * Dipanggil saat pengguna mengklik salah satu emoji mood.
     */
    private fun onMoodSelected(score: Int, moodName: String) {
        // Reset tampilan semua emoji
        resetMoodSelection()

        val selectedViewId = when (score) {
            5 -> R.id.iv_mood_happy_extreme
            4 -> R.id.iv_mood_happy
            3 -> R.id.iv_mood_neutral
            2 -> R.id.iv_mood_sad
            else -> R.id.iv_mood_sad_extreme
        }

        val selectedView = view?.findViewById<ImageView>(selectedViewId)
        if (selectedView != null) {
            selectedView.alpha = 1.0f
            val moodColor = getMoodColor(score)
            ImageViewCompat.setImageTintList(selectedView, ContextCompat.getColorStateList(requireContext(), moodColor))
        }

        // Simpan mood yang dipilih ke database
        selectedMoodScore = score
        selectedMoodName = moodName

        val newMoodEntry = MoodEntry(
            score = score,
            moodName = moodName,
            timestamp = System.currentTimeMillis()
        )
        // FIX: saveMood sekarang memanggil Cloud Repository
        viewModel.saveMood(newMoodEntry)
        Toast.makeText(requireContext(), "Mood Hari Ini Dicatat: $moodName", Toast.LENGTH_SHORT).show()
    }

    /**
     * Mengatur ulang alpha dan warna (tint) semua ImageView mood ke 0.5f dan warna MOOD ASLI.
     */
    private fun resetMoodSelection() {
        val moodViewScores = mapOf(
            binding.ivMoodHappyExtreme to 5,
            binding.ivMoodHappy to 4,
            binding.ivMoodNeutral to 3,
            binding.ivMoodSad to 2,
            binding.ivMoodSadExtreme to 1
        )

        moodViewScores.forEach { entry ->
            entry.key.alpha = 0.5f
            val moodColor = getMoodColor(entry.value)
            ImageViewCompat.setImageTintList(entry.key, ContextCompat.getColorStateList(requireContext(), moodColor))
        }
    }

    /**
     * Fungsi helper untuk mengonversi skor ke nama mood.
     */
    private fun getMoodName(score: Int): String {
        return when (score) {
            5 -> "Very Happy"
            4 -> "Happy"
            3 -> "Neutral"
            2 -> "Sad"
            else -> "Very Sad"
        }
    }

    /**
     * Fungsi helper untuk mengonversi skor ke resource ID warna.
     */
    private fun getMoodColor(score: Int): Int {
        return when (score) {
            5 -> R.color.mood_very_happy
            4 -> R.color.mood_happy
            3 -> R.color.mood_neutral
            2 -> R.color.mood_sad
            else -> R.color.mood_very_sad
        }
    }


    /**
     * Mengamati LiveData dari ViewModel dan memperbarui tampilan mood hari ini dan quotes.
     */
    private fun setupObservers() {
        // Observer untuk Mood Harian
        viewModel.currentDayMood.observe(viewLifecycleOwner) { mood ->
            if (mood != null) {
                // Ada mood yang dicatat hari ini, set tampilan
                resetMoodSelection()
                val selectedViewId = when (mood.score) {
                    5 -> R.id.iv_mood_happy_extreme
                    4 -> R.id.iv_mood_happy
                    3 -> R.id.iv_mood_neutral
                    2 -> R.id.iv_mood_sad
                    else -> R.id.iv_mood_sad_extreme
                }
                val selectedView = view?.findViewById<ImageView>(selectedViewId)
                if (selectedView != null) {
                    selectedView.alpha = 1.0f
                    val moodColor = getMoodColor(mood.score)
                    ImageViewCompat.setImageTintList(selectedView, ContextCompat.getColorStateList(requireContext(), moodColor))
                }

                selectedMoodName = mood.moodName

                // Ganti prompt menjadi mood yang sudah dicatat
                binding.tvMoodPrompt.text = "Mood Anda hari ini: ${mood.moodName}"
            } else {
                // Belum ada mood, tampilkan prompt default
                binding.tvMoodPrompt.text = "How are you feeling today?"
                resetMoodSelection() // Pastikan semua di-reset
            }
        }

        // BARU: Observer untuk Quote
        viewModel.currentQuote.observe(viewLifecycleOwner) { quote ->
            binding.tvQuoteText.text = "\"${quote.text}\"" // Tampilkan teks quote
            binding.tvQuoteAuthor.text = "— ${quote.author}" // Tampilkan penulis
        }
    }

    // ... (fungsi onResume dan onDestroyView tetap sama)
    override fun onResume() {
        super.onResume()
        // Panggil ulang greeting saat fragment kembali (misal setelah Edit Profile)
        setupGreeting()
        // Panggil ulang loadRandomQuote untuk mendapatkan quote baru
        viewModel.loadRandomQuote()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}