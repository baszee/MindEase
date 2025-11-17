package com.mindease.mindeaseapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Model untuk menyimpan data profil pengguna yang lebih detail di Firestore (bukan hanya Auth).
 */
data class UserProfile(
    @DocumentId
    val documentId: String? = null,
    val userId: String = "",

    // Data yang mungkin berbeda dengan Firebase Auth (untuk sinkronisasi di masa depan)
    val name: String? = null,
    val email: String? = null,

    // 🔥 BARU: Field Profile Kustom
    val bio: String? = "Always Be Happy", // Bio singkat/status
    val profileImageUrl: String? = null, // URL gambar profil (dari Storage)
) {
    // Konstruktor kosong diperlukan oleh Firestore
    constructor() : this(null, "", null, null, "Always Be Happy", null)
}