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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.mindease.mindeaseapp.data.repository.MoodCloudRepository
import com.mindease.mindeaseapp.utils.ThemeManager
import android.content.Context
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.text.DateFormatSymbols // 🔥 FIX: Tambahkan import ini

class MoodHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMoodHistoryBinding

    private val viewModel: MoodHistoryViewModel by viewModels {
        val firestore = FirebaseFirestore.getInstance()
        val auth = Firebase.auth
        val repository = MoodCloudRepository(firestore, auth)
        MoodHistoryViewModelFactory(repository)
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
            val currentFilter = viewModel.filter.value
            if (moods.isNotEmpty()) {
                setupChart(moods, currentFilter)
                updateStatistics(moods)
            } else {
                setupChart(emptyList(), currentFilter)
                updateStatistics(emptyList())
            }
        }

        setupTabLayout()

        binding.tabFilter.selectTab(binding.tabFilter.getTabAt(0))
    }

    private fun setupTabLayout() {
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

    // ==========================================================
    // UTILITY KHUSUS UNTUK FILTER WEEK (SUN-SAT FIXED INDEX)
    // ==========================================================

    private fun getWeekEntriesFixed(moods: List<MoodEntry>): List<Entry> {
        val entries = mutableListOf<Entry>()
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.US)

        // De-duplikasi berdasarkan hari
        val uniqueMoodsByDay = moods
            .groupBy { dateFormatter.format(Date(it.timestamp)) }
            .map { it.value.first() }

        for (entry in uniqueMoodsByDay) {
            val tempCalendar = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
            val dayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)
            val xIndex = dayOfWeek - 1

            entries.add(Entry(xIndex.toFloat(), entry.score.toFloat()))
        }

        return entries.sortedBy { it.x }
    }

    // ==========================================================
    // UTILITY UNTUK FILTER MONTH (5 WEEKS FIXED INDEX)
    // ==========================================================

    private fun getMonthEntriesFixed(moods: List<MoodEntry>): List<Entry> {
        val entries = mutableListOf<Entry>()
        val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.US)

        val uniqueMoodsByDay = moods
            .groupBy { dateFormatter.format(Date(it.timestamp)) }
            .map { it.value.first() }
            .sortedBy { it.timestamp }

        if (uniqueMoodsByDay.isEmpty()) return emptyList()

        val startTime = uniqueMoodsByDay.first().timestamp

        val weeklyMoods = mutableMapOf<Int, MutableList<Float>>()

        for (entry in uniqueMoodsByDay) {
            val diffMs = entry.timestamp - startTime
            val diffDays = TimeUnit.DAYS.convert(diffMs, TimeUnit.MILLISECONDS)

            // Bagi 30 hari ke dalam 5 bucket @ 6 hari
            val weekIndex = (diffDays / 6).toInt().coerceAtMost(4)

            weeklyMoods.getOrPut(weekIndex) { mutableListOf() }.add(entry.score.toFloat())
        }

        // Hitung rata-rata mood per week bucket
        weeklyMoods.forEach { (index, scores) ->
            val averageScore = scores.average().toFloat()
            entries.add(Entry(index.toFloat(), averageScore))
        }

        return entries.sortedBy { it.x }
    }

    // ==========================================================
    // UTILITY UNTUK FILTER YEAR (12 MONTHS FIXED INDEX)
    // ==========================================================

    private fun getYearEntriesFixed(moods: List<MoodEntry>): List<Entry> {
        val dateFormatter = SimpleDateFormat("yyyyMM", Locale.US)

        // Ambil data unik per bulan (kita akan rata-rata)
        val monthlyMoods = moods
            .groupBy { dateFormatter.format(Date(it.timestamp)) }
            .mapValues { entryList ->
                entryList.value.map { it.score.toFloat() }
            }

        val entries = mutableListOf<Entry>()
        val calendar = Calendar.getInstance()

        // Iterate 12 bulan (0=Jan, 11=Dec)
        for (monthIndex in 0 until 12) {
            // Set tahun ke tahun saat ini (asumsi data di filterYear adalah 365 hari terakhir)
            // Kita harus menentukan tahun yang benar, yang paling aman adalah tahun dari hari ini
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.MONTH, monthIndex)

            // Buat key YYYYMM untuk bulan ini
            val yearMonthKey = SimpleDateFormat("yyyyMM", Locale.US).format(calendar.time)

            val scores = monthlyMoods[yearMonthKey]

            if (scores != null && scores.isNotEmpty()) {
                val averageScore = scores.average().toFloat()
                // X-Value adalah index bulan (0-11)
                entries.add(Entry(monthIndex.toFloat(), averageScore))
            }
        }

        return entries.sortedBy { it.x }
    }


    /**
     * FIX: Menerima MoodFilter untuk menyesuaikan tampilan Axis X (formatter dan label count).
     */
    private fun setupChart(moods: List<MoodEntry>, filter: MoodFilter) {
        val sortedMoods = moods.sortedBy { it.timestamp }
        val chart = binding.moodLineChart

        val entries: List<Entry> = when (filter) {
            MoodFilter.WEEK -> getWeekEntriesFixed(sortedMoods)
            MoodFilter.MONTH -> getMonthEntriesFixed(sortedMoods)
            MoodFilter.YEAR -> getYearEntriesFixed(sortedMoods)
            MoodFilter.ALL -> sortedMoods.mapIndexed { index, moodEntry ->
                Entry(index.toFloat(), moodEntry.score.toFloat())
            }
        }

        if (entries.isEmpty() && filter != MoodFilter.WEEK && filter != MoodFilter.YEAR && filter != MoodFilter.MONTH) {
            binding.moodLineChart.setNoDataText(getString(R.string.no_mood_data_period))
            chart.data = null
            chart.invalidate()
            return
        } else if (entries.isEmpty() && (filter == MoodFilter.WEEK || filter == MoodFilter.MONTH || filter == MoodFilter.YEAR)) {
            binding.moodLineChart.setNoDataText("Belum ada data Mood yang tercatat pada periode ini.")
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

            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        }

        chart.data = LineData(dataSet)

        // ==========================================================
        // LOGIKA X-AXIS FIXED LABEL
        // ==========================================================
        val dateFormatter = MoodDateFormatter(sortedMoods, filter)

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            valueFormatter = dateFormatter
            setDrawLabels(true)
            labelRotationAngle = 0f

            when (filter) {
                MoodFilter.WEEK -> {
                    labelCount = 7
                    axisMinimum = -0.5f
                    axisMaximum = 6.0f // Sun=0 hingga Sat=6
                    setLabelCount(7, false)
                }
                MoodFilter.MONTH -> {
                    labelCount = 5
                    axisMinimum = -0.5f
                    axisMaximum = 4.0f // Index 0 hingga 4
                    setLabelCount(5, false)
                    labelRotationAngle = -45f
                }
                MoodFilter.YEAR -> {
                    labelCount = 12
                    axisMinimum = -0.5f
                    axisMaximum = 11.0f // Index 0 hingga 11
                    setLabelCount(12, false)
                    labelRotationAngle = -45f
                }
                MoodFilter.ALL -> {
                    labelCount = entries.size.coerceAtMost(12)
                    axisMinimum = -0.5f
                    axisMaximum = entries.size.toFloat() - 0.5f
                    labelRotationAngle = -45f
                }
            }

            isGranularityEnabled = true
        }
        // ==========================================================

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
        val mostCommonMood = moodCounts.maxByOrNull { it.value }?.key ?: "Netral"
        binding.tvMostCommonMood.text = getString(R.string.most_common_mood, mostCommonMood)

        binding.tvGoodDay.visibility = View.GONE
        binding.tvRoughDay.visibility = View.GONE
    }

    /**
     * FIX: Formatter sekarang menggunakan DateFormatSymbols untuk mendapatkan label bulan yang benar
     * dan mengatasi error kompilasi.
     */
    private class MoodDateFormatter(
        private val moods: List<MoodEntry>,
        private val filter: MoodFilter
    ) : ValueFormatter() {

        // Label Statis
        private val weekDayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        private val monthWeekLabels = listOf("Week 1", "Week 2", "Week 3", "Week 4", "Week 5")

        // 🔥 FIX: Menggunakan DateFormatSymbols untuk mengambil 12 nama bulan singkat.
        private val yearMonthLabels: List<String> =
            DateFormatSymbols.getInstance(Locale.getDefault()).getShortMonths().take(12).toList()

        // Format untuk Tahun (MMM yy: Jan 24)
        private val monthYearFormat = SimpleDateFormat("MMM yy", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()

            // Filter dengan Fixed Index
            return when (filter) {
                MoodFilter.WEEK -> weekDayLabels.getOrNull(index) ?: ""
                MoodFilter.MONTH -> monthWeekLabels.getOrNull(index) ?: ""
                MoodFilter.YEAR -> yearMonthLabels.getOrNull(index) ?: ""
                MoodFilter.ALL -> {
                    // Logika ALL tetap berdasarkan data kronologis
                    if (index < 0 || index >= moods.size) return ""
                    val timestamp = moods[index].timestamp
                    // Tampilkan nama bulan dan tahun (Jan 24, Feb 24, ...) saat bulan berubah
                    if (index == 0 || isMonthChanged(index)) {
                        monthYearFormat.format(Date(timestamp))
                    } else {
                        ""
                    }
                }
            }
        }

        // Helper function untuk cek perubahan bulan (HANYA dipakai di ALL TIME)
        private fun isMonthChanged(index: Int): Boolean {
            if (index <= 0 || index >= moods.size) return false

            val currentMonth = Calendar.getInstance().apply { timeInMillis = moods[index].timestamp }.get(Calendar.MONTH)
            val previousMonth = Calendar.getInstance().apply { timeInMillis = moods[index - 1].timestamp }.get(Calendar.MONTH)

            return currentMonth != previousMonth
        }
    }

    /**
     * Formatter untuk Axis Y dan nilai data point (TETAP SAMA).
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