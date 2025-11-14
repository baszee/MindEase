package com.mindease.mindeaseapp.ui.journal

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisFormatter
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.AppDatabase
import com.mindease.mindeaseapp.data.model.MoodEntry
import com.mindease.mindeaseapp.data.repository.MoodRepository
import com.mindease.mindeaseapp.databinding.ActivityMoodHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MoodHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMoodHistoryBinding
    private val viewModel: MoodHistoryViewModel by viewModels {
        // Inisialisasi ViewModel dengan Factory
        val moodDao = AppDatabase.getDatabase(applicationContext).moodDao()
        val repository = MoodRepository(moodDao)
        MoodHistoryViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoodHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup Observer untuk data Mood
        viewModel.allMoods.observe(this) { moods ->
            if (moods.isNotEmpty()) {
                setupChart(moods)
                updateStatistics(moods)
            } else {
                // Tampilkan pesan jika tidak ada data
                binding.moodLineChart.setNoDataText("Belum ada data Mood yang tercatat.")
            }
        }

        // TODO: Tambahkan listener untuk TabLayout (Weeks/Months/Years) di sini
    }

    /**
     * Menginisialisasi dan menggambar grafik garis Mood.
     */
    private fun setupChart(moods: List<MoodEntry>) {
        val chart = binding.moodLineChart

        // Data harus dibalik agar diurutkan dari yang terlama ke yang terbaru (kiri ke kanan)
        val sortedMoods = moods.sortedBy { it.timestamp }

        // 1. Membuat Entry Data (Konversi Mood Score ke Y-value)
        val entries: List<Entry> = sortedMoods.mapIndexed { index, moodEntry ->
            // Index digunakan sebagai X-value, moodScore sebagai Y-value
            Entry(index.toFloat(), moodEntry.moodScore.toFloat())
        }

        // 2. Membuat Data Set
        val dataSet = LineDataSet(entries, "Mood Score Harian").apply {
            color = resources.getColor(R.color.design_default_color_primary, theme)
            valueTextColor = resources.getColor(R.color.design_default_color_primary, theme)
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(resources.getColor(R.color.design_default_color_primary, theme))
            circleRadius = 4f
            valueTextSize = 10f
            // Mengatur nilai mood dari 1 hingga 5
            valueFormatter = MoodValueFormatter()
        }

        // 3. Mengatur Data Grafik
        chart.data = LineData(dataSet)

        // 4. Mengatur Axis X (Tanggal)
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f // Hanya menampilkan satu label per hari
            setDrawGridLines(false)
            // Menggunakan MoodDateFormatter untuk menampilkan tanggal
            valueFormatter = MoodDateFormatter(sortedMoods)
            labelCount = 7 // Coba tampilkan 7 label
        }

        // 5. Mengatur Axis Y (Score Mood)
        chart.axisLeft.apply {
            axisMinimum = 1f // Minimum score 1
            axisMaximum = 5f // Maximum score 5
            granularity = 1f
            setDrawGridLines(true)
            valueFormatter = MoodValueFormatter()
        }
        chart.axisRight.isEnabled = false // Nonaktifkan axis kanan

        // 6. Pengaturan Tampilan Grafik
        chart.description.isEnabled = false // Hapus deskripsi
        chart.legend.isEnabled = false // Hapus legenda
        chart.invalidate() // Refresh grafik
    }

    /**
     * Mengupdate Card Statistik Mood.
     */
    private fun updateStatistics(moods: List<MoodEntry>) {
        if (moods.isEmpty()) return

        // 1. Hitung Rata-Rata
        val averageScore = moods.map { it.moodScore }.average()
        binding.tvAverageScore.text = getString(R.string.average_mood_score, String.format("%.1f", averageScore))

        // 2. Hitung Mood Paling Umum (Placeholder, butuh data tanggal yang lebih kompleks)
        val moodCounts = moods.groupingBy { it.moodName }.eachCount()
        val mostCommonMood = moodCounts.maxByOrNull { it.value }?.key ?: "Neutral"
        binding.tvMostCommonMood.text = getString(R.string.most_common_mood, mostCommonMood)

        // TODO: Logika untuk Good Day/Rough Day dan filter Weeks/Months/Years
    }

    /**
     * Formatter untuk menampilkan tanggal pada Axis X Grafik.
     */
    private class MoodDateFormatter(private val moods: List<MoodEntry>) : IndexAxisFormatter() {
        private val dateFormat = SimpleDateFormat("EEE", Locale.getDefault()) // Hari (Sen, Sel, Rab, dst)

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
     * Formatter untuk Axis Y agar menampilkan nilai 1-5 dengan label Mood (opsional).
     */
    private class MoodValueFormatter : IndexAxisFormatter() {
        private val moodLabels = mapOf(
            1f to "Very Sad",
            2f to "Sad",
            3f to "Neutral",
            4f to "Happy",
            5f to "Very Happy"
        )

        override fun getFormattedValue(value: Float): String {
            // Coba tampilkan nilai 1, 2, 3, 4, 5 di Y-Axis
            return moodLabels[value] ?: value.toString()
        }
    }
}