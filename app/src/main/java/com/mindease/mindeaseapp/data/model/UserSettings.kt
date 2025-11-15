package com.mindease.mindeaseapp.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Model untuk menyimpan preferensi pengguna (Settings) di Firestore.
 */
data class UserSettings(
    // ID Dokumen akan diset di repository
    @DocumentId
    val documentId: String? = null,
    val isSoundEnabled: Boolean = true,
    val isHapticEnabled: Boolean = false
) {
    // Konstruktor kosong diperlukan oleh Firestore
    constructor() : this(null, true, false)
}