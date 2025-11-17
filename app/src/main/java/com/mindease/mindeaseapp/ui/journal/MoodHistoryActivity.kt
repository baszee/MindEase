package com.mindease.mindeaseapp.ui.journal

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.MoodEntry
import com.mindease.mindeaseapp.databinding.ActivityMoodHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth // Tambahkan import
import com.google.firebase.ktx.Firebase // Tambahkan import
import com.google.firebase.firestore.FirebaseFirestore // Tambahkan import
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository // Tambahkan import
import com.mindease.mindeaseapp.utils.ThemeManager
import android.content.Context
class MoodHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMoodHistoryBinding

    // FIX: Mengubah inisialisasi ViewModel untuk menggunakan MoodCloudRepository
    private val viewModel: MoodHistoryViewModel by viewModels {
        val firestore = FirebaseFirestore.getInstance()
        val auth = Firebase.auth
        val repository = MoodCloudRepository(firestore, auth) // Menggunakan Cloud Repository
        MoodHistoryViewModelFactory(repository) // Menggunakan Factory yang sudah diperbarui
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityMoodHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        viewModel.filteredMoods.observe(this) { moods ->
            // FIX: Hanya ambil satu mood per hari untuk ditampilkan di grafik
            // Data dari Firestore sudah memiliki satu entri per hari berkat fix di ViewModel.
            if (moods.isNotEmpty()) {
                setupChart(moods)
                updateStatistics(moods)
            } else {
                binding.moodLineChart.setNoDataText("Belum ada data Mood yang tercatat pada periode ini.")
                binding.moodLineChart.invalidate()
                updateStatistics(emptyList())
            }
        }

        setupTabLayout()

        // PENTING: Panggil setFilter untuk memicu pemuatan data awal
        binding.tabFilter.selectTab(binding.tabFilter.getTabAt(0))
    }

    private fun setupTabLayout() {
        // ... (Tidak ada perubahan, fungsi ini tetap sama)
        binding.tabFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val filter = when (tab?.position) {
                    0 -> MoodFilter.WEEK
                    1 -> MoodFilter.MONTH
                    2 -> MoodFilter.YEAR
                    3 -> MoodFilter.ALL
                    else -> MoodFilter.WEEK
                }
                viewModel.setFilter(filter)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }


    private fun setupChart(moods: List<MoodEntry>) {
        val chart = binding.moodLineChart
        // Data dari Cloud Repository sudah terurut DESCENDING,
        // tapi untuk chart kita butuh urutan ASCENDING (dari kiri ke kanan)
        val sortedMoods = moods.sortedBy { it.timestamp }

        val entries: List<Entry> = sortedMoods.mapIndexed { index, moodEntry ->
            Entry(index.toFloat(), moodEntry.score.toFloat())
        }

        val primaryColor = ContextCompat.getColor(this, R.color.mindease_primary)

        val dataSet = LineDataSet(entries, "Mood Score Harian").apply {
            color = primaryColor
            valueTextColor = primaryColor
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(primaryColor)
            circleRadius = 4f
            valueTextSize = 10f
            valueFormatter = MoodValueFormatter()
        }

        chart.data = LineData(dataSet)

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            valueFormatter = MoodDateFormatter(sortedMoods)
            // Tampilkan maks 7 label hari di bawah grafik
            labelCount = sortedMoods.size.coerceAtMost(7)
            isGranularityEnabled = true
        }

        chart.axisLeft.apply {
            axisMinimum = 1f
            axisMaximum = 5f
            granularity = 1f
            setDrawGridLines(true)
            valueFormatter = MoodValueFormatter()
        }
        chart.axisRight.isEnabled = false

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.invalidate()
    }

    private fun updateStatistics(moods: List<MoodEntry>) {
        if (moods.isEmpty()) {
            binding.tvAverageScore.text = getString(R.string.average_mood_score, "N/A")
            binding.tvMostCommonMood.text = getString(R.string.most_common_mood, "N/A")
            binding.tvGoodDay.visibility = View.GONE
            binding.tvRoughDay.visibility = View.GONE
            return
        }

        val averageScore = moods.map { it.score }.average()
        binding.tvAverageScore.text = getString(R.string.average_mood_score, String.format(Locale.getDefault(), "%.1f", averageScore))

        val moodCounts = moods.groupingBy { it.moodName }.eachCount()
        // Menggunakan maxByOrNull dengan perbandingan nilai untuk menemukan yang paling sering
        val mostCommonMood = moodCounts.maxByOrNull { it.value }?.key ?: "Netral"
        binding.tvMostCommonMood.text = getString(R.string.most_common_mood, mostCommonMood)

        // 🔥 FIX: Sembunyikan/hilangkan teks hardcoded tentang hari baik/buruk
        // Teks ini TIDAK dapat dihitung secara akurat tanpa logic bisnis yang kompleks.
        binding.tvGoodDay.visibility = View.GONE
        binding.tvRoughDay.visibility = View.GONE
    }

    /**
     * Formatter untuk menampilkan tanggal pada Axis X Grafik.
     */
    private class MoodDateFormatter(private val moods: List<MoodEntry>) : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("EEE", Locale.Builder().setLanguage("en").build())

        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return if (index >= 0 && index < moods.size) {
                dateFormat.format(Date(moods[index].timestamp))
            } else {
                ""
            }
        }
    }

    /**
     * Formatter untuk Axis Y dan nilai data point.
     */
    private class MoodValueFormatter : ValueFormatter() {
        private val moodLabels = mapOf(
            1f to "Very Sad",
            2f to "Sad",
            3f to "Neutral",
            4f to "Happy",
            5f to "Very Happy"
        )

        override fun getFormattedValue(value: Float): String {
            return moodLabels[value] ?: value.toString()
        }
    }
}