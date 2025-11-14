package com.mindease.mindeaseapp.ui.journal

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
// FIX: Hapus IndexAxisFormatter. Gunakan ValueFormatter saja.
import com.github.mikephil.charting.formatter.ValueFormatter
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.AppDatabase
import com.mindease.mindeaseapp.data.model.MoodEntry
import com.mindease.mindeaseapp.data.repository.MoodRepository
import com.mindease.mindeaseapp.databinding.ActivityMoodHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoodHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMoodHistoryBinding

    // FIX: Menggunakan delegasi viewModels yang benar (sekarang harus berhasil menemukan ViewModel dan Factory)
    private val viewModel: MoodHistoryViewModel by viewModels {
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
                binding.moodLineChart.setNoDataText("Belum ada data Mood yang tercatat.")
            }
        }
    }

    private fun setupChart(moods: List<MoodEntry>) {
        val chart = binding.moodLineChart
        val sortedMoods = moods.sortedBy { it.timestamp }

        // 1. Membuat Entry Data
        val entries: List<Entry> = sortedMoods.mapIndexed { index, moodEntry ->
            Entry(index.toFloat(), moodEntry.score.toFloat())
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
            valueFormatter = MoodValueFormatter()
        }

        // 3. Mengatur Data Grafik
        chart.data = LineData(dataSet)

        // 4. Mengatur Axis X (Tanggal)
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            // FIX: Menggunakan MoodDateFormatter (extends ValueFormatter)
            valueFormatter = MoodDateFormatter(sortedMoods)
            labelCount = 7
        }

        // 5. Mengatur Axis Y (Score Mood)
        chart.axisLeft.apply {
            axisMinimum = 1f
            axisMaximum = 5f
            granularity = 1f
            setDrawGridLines(true)
            valueFormatter = MoodValueFormatter()
        }
        chart.axisRight.isEnabled = false

        // 6. Pengaturan Tampilan Grafik
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.invalidate()
    }

    private fun updateStatistics(moods: List<MoodEntry>) {
        if (moods.isEmpty()) return

        // 1. Hitung Rata-Rata
        val averageScore = moods.map { it.score }.average()
        binding.tvAverageScore.text = getString(R.string.average_mood_score, String.format(Locale.getDefault(), "%.1f", averageScore))

        // 2. Hitung Mood Paling Umum
        val moodCounts = moods.groupingBy { it.moodName }.eachCount()
        val mostCommonMood = moodCounts.maxByOrNull { it.value }?.key ?: "Netral"
        binding.tvMostCommonMood.text = getString(R.string.most_common_mood, mostCommonMood)
    }

    /**
     * Formatter untuk menampilkan tanggal pada Axis X Grafik.
     * FIX: Menggunakan ValueFormatter (solusi yang lebih kompatibel)
     */
    private class MoodDateFormatter(private val moods: List<MoodEntry>) : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())

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
     * Formatter untuk Axis Y
     */
    private class MoodValueFormatter : ValueFormatter() {
        private val moodLabels = mapOf(
            1f to "Sangat Buruk",
            2f to "Buruk",
            3f to "Netral",
            4f to "Baik",
            5f to "Sangat Baik"
        )

        override fun getFormattedValue(value: Float): String {
            return moodLabels[value] ?: value.toString()
        }
    }
}