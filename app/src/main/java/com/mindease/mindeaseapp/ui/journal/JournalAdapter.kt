package com.mindease.mindeaseapp.ui.journal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.databinding.ItemJournalEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter untuk menampilkan daftar JournalEntry di RecyclerView.
 */
class JournalAdapter(private val onItemClicked: (JournalEntry) -> Unit) :
    ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(JournalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JournalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
        holder.itemView.setOnClickListener {
            onItemClicked(currentItem)
        }
    }

    inner class JournalViewHolder(private val binding: ItemJournalEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(journal: JournalEntry) {
            binding.apply {
                // 1. Tanggal
                tvDate.text = formatDate(journal.timestamp)

                // 2. Mood Name
                tvMoodName.text = journal.moodName

                // 3. Ringkasan Konten
                tvContentSummary.text = journal.content

                // 4. Ikon Mood
                ivMoodIcon.setImageResource(getMoodIconResource(journal.moodScore))

                // 5. Gambar Preview (Jika ada)
                if (journal.imagePath != null && journal.imagePath.isNotEmpty()) {
                    // TODO: Gunakan library seperti Glide atau Coil untuk memuat gambar
                    ivJournalImagePreview.visibility = View.VISIBLE
                } else {
                    ivJournalImagePreview.visibility = View.GONE
                }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
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

    /** Digunakan untuk menentukan perubahan item secara efisien saat memperbarui daftar. */
    class JournalDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}