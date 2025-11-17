package com.mindease.mindeaseapp.ui.journal

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.databinding.ItemJournalEntryBinding
import com.mindease.mindeaseapp.utils.LocalizationHelper // <-- PENTING
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalAdapter : ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(JournalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JournalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry)
    }

    inner class JournalViewHolder(private val binding: ItemJournalEntryBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: JournalEntry) {
            // ✅ PERBAIKAN: Menggunakan LocalizationHelper untuk menerjemahkan moodName dari DB.
            binding.tvMoodName.text = LocalizationHelper.getLocalizedMoodName(binding.root.context, entry.moodName)
            binding.tvContentSummary.text = entry.content
            binding.tvDate.text = formatDate(entry.timestamp)

            // Pewarnaan Mood Icon
            binding.ivMoodIcon.setImageResource(getMoodDrawable(entry.moodName))
            val moodColor = getMoodColor(entry.moodScore)
            val colorStateList = ContextCompat.getColorStateList(binding.root.context, moodColor)
            ImageViewCompat.setImageTintList(binding.ivMoodIcon, colorStateList)

            // FIX: Kirim documentId (String), bukan id (Integer)
            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, DetailJournalActivity::class.java).apply {
                    putExtra(DetailJournalActivity.EXTRA_JOURNAL_ID, entry.documentId)
                }
                context.startActivity(intent)
            }
        }

        private fun formatDate(timestamp: Long): String {
            // ✅ PERBAIKAN TANGGAL: Menggunakan Locale default yang sudah diatur ThemeManager.
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
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

        private fun getMoodDrawable(moodName: String): Int {
            return when (moodName) {
                "Very Happy" -> R.drawable.ic_mood_happy_extreme
                "Happy" -> R.drawable.ic_mood_happy
                "Neutral" -> R.drawable.ic_mood_neutral
                "Sad" -> R.drawable.ic_mood_sad
                "Very Sad" -> R.drawable.ic_mood_sad_extreme
                else -> R.drawable.ic_mood_neutral
            }
        }
    }

    class JournalDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.documentId == newItem.documentId
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}