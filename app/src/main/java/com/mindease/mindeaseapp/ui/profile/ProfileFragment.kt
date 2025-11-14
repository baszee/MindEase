package com.mindease.mindeaseapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.repository.AuthRepository // Wajib
import com.mindease.mindeaseapp.databinding.FragmentProfileBinding // Wajib
import com.mindease.mindeaseapp.ui.auth.LoginActivity // Wajib untuk Redirect
import com.mindease.mindeaseapp.ui.common.SplashActivity // Wajib untuk Refresh

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Inisialisasi AuthRepository di Fragment
    private val authRepository: AuthRepository by lazy {
        AuthRepository(Firebase.auth)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayUserProfile()
        setupLogoutListener()

        // Listener dummy untuk opsi settings
        binding.tvThemes.setOnClickListener { Toast.makeText(context, "Themes (TODO)", Toast.LENGTH_SHORT).show() }
        binding.tvSettings.setOnClickListener { Toast.makeText(context, "Settings (TODO)", Toast.LENGTH_SHORT).show() }
        binding.tvAboutApp.setOnClickListener { Toast.makeText(context, "About App (TODO)", Toast.LENGTH_SHORT).show() }
    }

    /**
     * Mengambil data pengguna dari Firebase dan menampilkannya di UI.
     */
    private fun displayUserProfile() {
        val user = authRepository.currentUser

        if (user != null) {
            // Tampilkan nama atau email
            binding.tvUserName.text = user.displayName ?: "User"

            // Tampilkan email. Jika login Guest, tampilkan status Guest
            binding.tvUserEmail.text = if (user.isAnonymous) {
                "Guest Session"
            } else {
                user.email ?: "No Email Found"
            }

            // TODO: Tambahkan Glide untuk foto profil dari user.photoUrl jika ada.

        } else {
            binding.tvUserName.text = "Error"
            binding.tvUserEmail.text = "Not Authenticated"
        }
    }

    /**
     * Menyiapkan listener untuk tombol Logout.
     */
    private fun setupLogoutListener() {
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        // 1. Panggil fungsi logout dari Repository
        authRepository.logout()

        Toast.makeText(context, "Anda telah logout.", Toast.LENGTH_SHORT).show()

        // 2. Arahkan kembali ke Splash Activity (yang akan dialihkan ke Login Activity)
        val intent = Intent(activity, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}