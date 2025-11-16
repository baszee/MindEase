package com.mindease.mindeaseapp.utils

import kotlinx.coroutines.delay
import android.util.Log

/**
 * Fungsi utilitas untuk mencoba kembali blok kode yang gagal (operasi jaringan)
 * dengan penundaan yang meningkat secara eksponensial (Exponential Backoff).
 *
 * Ini menghemat kuota Firebase dengan menghindari spamming requests yang gagal.
 */
suspend fun <T> retryWithExponentialBackoff(
    times: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    tag: String = "NetworkRetry",
    block: suspend (attempt: Int) -> T
): T {
    var currentDelay = initialDelay

    repeat(times - 1) { attempt ->
        try {
            return block(attempt)
        } catch (e: Exception) {
            Log.w(tag, "Gagal percobaan ke-${attempt + 1}. Menunggu ${currentDelay}ms. Error: ${e.message}")
        }

        delay(currentDelay)
        // Hitung penundaan berikutnya dan batasi (coerceAtMost)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }

    // Percobaan terakhir
    return block(times - 1)
}