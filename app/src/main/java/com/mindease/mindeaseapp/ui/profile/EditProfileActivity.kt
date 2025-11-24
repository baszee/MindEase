package com.mindease.mindeaseapp.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.databinding.ActivityEditProfileBinding
import com.mindease.mindeaseapp.ui.auth.AuthViewModel
import com.mindease.mindeaseapp.ui.auth.AuthViewModelFactory
import com.mindease.mindeaseapp.utils.AuthResult
import com.mindease.mindeaseapp.utils.ThemeManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide
import android.content.Context

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemeManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeManager.getThemeStyleResId(this))
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = Firebase.auth.currentUser

        // FIX: Blokir akses Edit Profile jika user adalah Guest
        if (user == null || user.isAnonymous) {
            Toast.makeText(this, getString(R.string.guest_cannot_edit_profile), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Setup Repository dan ViewModel
        authRepository = AuthRepository(Firebase.auth, FirebaseFirestore.getInstance())
        val factory = AuthViewModelFactory(authRepository)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadCurrentUserProfile()
        setupSaveButton()
        observeViewModel()
    }

    private fun loadCurrentUserProfile() {
        val user = Firebase.auth.currentUser

        // Memuat data dari Firebase Auth (untuk info Email)
        binding.tvUserEmail.text = user?.email ?: getString(R.string.no_email_found)

        // Memuat data dari Firestore (Nama, Bio, Image URL)
        lifecycleScope.launch {
            val profile = authRepository.getUserProfile()

            // Nama dari Firebase Auth (jika ada) atau dari Firestore
            binding.etUsername.setText(user?.displayName ?: profile.name ?: "")
            // Bio dari Firestore
            binding.etBio.setText(profile.bio ?: getString(R.string.bio_short_status)) // Default dari model adalah "Always Be Happy"

            // 🔥 FIX LOGIKA FOTO PROFIL
            val imageUrl = profile.profileImageUrl ?: user?.photoUrl?.toString()

            // Kunci 1: Clear state Glide untuk menghindari konflik rendering lama
            Glide.with(this@EditProfileActivity).clear(binding.ivProfileImage)

            // KUNCI SINKRONISASI: Menggunakan logika yang sama persis dengan ProfileFragment
            if (!imageUrl.isNullOrBlank() && imageUrl != "null") {
                Glide.with(this@EditProfileActivity)
                    .load(imageUrl)
                    .circleCrop() // 🔥 Tambahkan circleCrop agar konsisten dengan ProfileFragment
                    .into(binding.ivProfileImage)
            } else {
                // Kunci 2: Secara eksplisit menggunakan setImageResource
                binding.ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val newName = binding.etUsername.text.toString().trim()
            val newBio = binding.etBio.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, getString(R.string.name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (Firebase.auth.currentUser == null) {
                Toast.makeText(this, getString(R.string.session_invalid_relogin), Toast.LENGTH_LONG).show()
                finish()
                return@setOnClickListener
            }

            // Saat ini, tidak ada fitur upload gambar, jadi URL gambar disetel null/lama
            // NOTE: Jika Anda ingin mempertahankan URL gambar lama yang sudah ada di Firestore,
            // Anda perlu mengambil profile.profileImageUrl di sini, bukan null.
            // Namun, karena di ProfileFragment sudah mengambil dari Auth juga, kita biarkan null
            // agar logika sinkronisasi di Repository mengambil URL terbaik yang tersedia.
            authViewModel.updateUserProfile(newName, newBio, null)
        }
    }

    private fun observeViewModel() {
        authViewModel.loginResult.observe(this) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    binding.btnSave.isEnabled = false
                    binding.btnSave.text = getString(R.string.loading)
                }
                is AuthResult.Success -> {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.save)
                    Toast.makeText(this, getString(R.string.profile_updated_successfully), Toast.LENGTH_SHORT).show()
                    finish()
                }
                is AuthResult.Error -> {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.save)
                    Toast.makeText(this, getString(R.string.generic_failure_message, result.exception.message), Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }
}