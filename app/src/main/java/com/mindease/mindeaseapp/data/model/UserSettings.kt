package com.mindease.mindeaseapp.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Model untuk menyimpan preferensi pengguna (Settings) di Firestore.
 * 🔥 FIX: Menggunakan @field:JvmField untuk mapping Firestore yang benar
 */
data class UserSettings(
    @DocumentId
    val documentId: String? = null,

    // 🔥 FIX: ANOTASI KRITIS UNTUK MAPPING FIRESTORE
    @field:JvmField
    val isSoundEnabled: Boolean = false,

    // 🔥 FIX: ANOTASI KRITIS UNTUK MAPPING FIRESTORE
    @field:JvmField
    val isHapticEnabled: Boolean = false,

    // 🔥 BARU: Pengaturan Tambahan
    @field:JvmField
    val language: String = "ID", // Default: ID

    @field:JvmField
    val notificationEnabled: Boolean = false
) {
    // Konstruktor kosong diperlukan oleh Firestore
    constructor() : this(null, false, false, "ID", false)
}