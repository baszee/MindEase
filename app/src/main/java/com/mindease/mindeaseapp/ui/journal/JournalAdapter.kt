package com.mindease.mindeaseapp.ui.journal

import android.net.Uri // Import untuk URI gambar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // FIX: Import Glide
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.model.JournalEntry
import com.mindease.mindeaseapp.databinding.ItemJournalEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalAdapter(private val onItemClicked: (JournalEntry) -> Unit) :
    ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(JournalDiffCallback()) {
    // ... (onCreateViewHolder dan onBindViewHolder tetap sama)

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
                // ... (Kode Jurnal lainnya)
                tvDate.text = formatDate(journal.timestamp)
                tvMoodName.text = journal.moodName
                tvContentSummary.text = journal.content
                ivMoodIcon.setImageResource(getMoodIconResource(journal.moodScore))

                // FIX: Logika untuk memuat gambar (Thumbnail)
                if (journal.imagePath != null && journal.imagePath.isNotEmpty()) {
                    ivJournalImagePreview.visibility = View.VISIBLE
                    Glide.with(itemView.context)
                        .load(Uri.parse(journal.imagePath)) // Muat dari URI yang tersimpan
                        .centerCrop()
                        .placeholder(R.drawable.ic_add_image) // Placeholder saat memuat
                        .into(ivJournalImagePreview)
                } else {
                    ivJournalImagePreview.visibility = View.GONE
                }
            }
        }

        private fun formatDate(timestamp: Long): String {
            // Menggunakan Locale Builder yang sudah diperbaiki
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

    class JournalDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}