package com.mindease.mindeaseapp.utils

import android.content.Context
import com.mindease.mindeaseapp.R

/**
 * Utility class untuk membantu melokalisasi string yang disimpan dalam format statis
 * di database (misalnya, nama mood).
 */
object LocalizationHelper {

    /**
     * Mengambil nama mood yang telah dilokalisasi berdasarkan string yang tersimpan di database.
     * Nama mood di database (e.g., "Very Happy") digunakan sebagai kunci.
     * @param context Context aplikasi
     * @param moodName String nama mood dari database (e.g., "Very Happy").
     * @return String nama mood yang telah diterjemahkan sesuai bahasa aplikasi.
     */
    fun getLocalizedMoodName(context: Context, moodName: String): String {
        return when (moodName) {
            "Very Happy" -> context.getString(R.string.mood_very_happy_name)
            "Happy" -> context.getString(R.string.mood_happy_name)
            "Neutral" -> context.getString(R.string.mood_neutral_name)
            "Sad" -> context.getString(R.string.mood_sad_name)
            "Very Sad" -> context.getString(R.string.mood_very_sad_name)
            else -> moodName // Fallback ke string asli jika tidak ditemukan
        }
    }
}