package com.mindease.mindeaseapp.ui.common

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.databinding.ActivitySplashBinding
import com.mindease.mindeaseapp.ui.auth.LoginActivity
import com.mindease.mindeaseapp.ui.home.MainActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY_MS: Long = 1500 // Kurangi jadi 1.5 detik

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndRedirect() // Panggil fungsi cek auth
        }, SPLASH_DELAY_MS)
    }

    /**
     * Mengecek status login pengguna saat ini dan mengarahkan ke Activity yang sesuai.
     * Ini akan segera berjalan setelah splash.
     */
    private fun checkAuthAndRedirect() {
        val authRepository = AuthRepository(Firebase.auth)

        val intent: Intent
        if (authRepository.currentUser != null) {
            // Pengguna sudah login, arahkan ke MainActivity
            intent = Intent(this, MainActivity::class.java)
        } else {
            // Pengguna belum login, arahkan ke LoginActivity
            intent = Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}