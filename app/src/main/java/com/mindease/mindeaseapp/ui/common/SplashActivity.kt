package com.mindease.mindeaseapp.ui.common

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.mindease.mindeaseapp.databinding.ActivitySplashBinding
import com.mindease.mindeaseapp.ui.auth.LoginActivity

class SplashActivity : AppCompatActivity() {

    // Durasi tampilan splash (2 detik)
    private val SPLASH_DELAY_MS: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Memastikan layout ActivitySplashBinding ada (di res/layout/activity_splash.xml)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pindah ke Activity berikutnya setelah penundaan
        Handler(Looper.getMainLooper()).postDelayed({

            // TODO: Nanti tambahkan logika pengecekan status login di sini

            // Mengarahkan ke LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // Tutup SplashActivity
            finish()
        }, SPLASH_DELAY_MS)
    }
}