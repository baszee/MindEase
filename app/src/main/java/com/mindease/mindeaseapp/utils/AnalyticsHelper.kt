@file:Suppress("DEPRECATION") // Digunakan untuk menekan warning KTX lama

package com.mindease.mindeaseapp.utils

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase

/**
 * Helper class untuk mencatat event ke Firebase Analytics (Prioritas 2).
 */
object AnalyticsHelper {
    private val analytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    // 🔥 FIX UTAMA: Menggunakan Bundle untuk logScreenView agar lebih stabil
    fun logScreenView(screenName: String, screenClass: String) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }

    // --- Auth Events ---

    fun logLogin(method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    fun logSignUp(method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SIGN_UP) {
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    // --- Mood Events ---

    fun logMoodTracked(moodName: String, score: Int) {
        analytics.logEvent("mood_tracked") {
            param("mood_name", moodName)
            param("mood_score", score.toLong())
        }
    }

    // --- Journal Events ---

    fun logJournalCreated(hasImage: Boolean) {
        analytics.logEvent("journal_created") {
            param("has_image", if (hasImage) "yes" else "no")
        }
    }

    fun logJournalDeleted() {
        analytics.logEvent("journal_deleted", null)
    }

    // --- Breathing Events ---

    fun logBreathingSessionCompleted(type: String, durationSeconds: Int) {
        analytics.logEvent("breathing_session_completed") {
            param("exercise_type", type)
            param("duration_seconds", durationSeconds.toLong())
        }
    }
}