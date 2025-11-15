package com.mindease.mindeaseapp.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Model data untuk Kutipan Motivasi yang akan diambil dari Firestore.
 */
data class Quote(
    @DocumentId
    val documentId: String? = null,
    val text: String = "",
    val author: String = ""
) {
    // Konstruktor kosong diperlukan oleh Firestore
    constructor() : this(null, "", "")
}