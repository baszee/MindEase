package com.mindease.mindeaseapp.utils

import android.content.Context
import androidx.preference.PreferenceManager // <-- IMPORT YANG DIPERLUKAN

object NotificationPreference {

    private const val PREF_KEY_NOTIFICATION_ENABLED = "notification_enabled"
    private const val DEFAULT_NOTIFICATION_STATE = true

    /**
     * Menyimpan status notifikasi ke SharedPreferences.
     * @param context Context aplikasi.
     * @param isEnabled Status notifikasi (true/false).
     */
    fun saveNotificationStatus(context: Context, isEnabled: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(PREF_KEY_NOTIFICATION_ENABLED, isEnabled).apply()
    }

    /**
     * Mengambil status notifikasi dari SharedPreferences.
     * @param context Context aplikasi.
     * @return Status notifikasi yang tersimpan, defaultnya adalah true.
     */
    fun getNotificationStatus(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREF_KEY_NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_STATE)
    }
}