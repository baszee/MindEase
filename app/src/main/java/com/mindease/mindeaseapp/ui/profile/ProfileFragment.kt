package com.mindease.mindeaseapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.data.repository.AuthRepository
import com.mindease.mindeaseapp.databinding.FragmentProfileBinding
import com.mindease.mindeaseapp.ui.auth.LoginActivity
import com.mindease.mindeaseapp.ui.common.SplashActivity
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Inisialisasi AuthRepository dengan Firestore
    private val authRepository: AuthRepository by lazy {
        //
        AuthRepository(Firebase.auth, FirebaseFirestore.getInstance())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayUserProfile()
        setupLogoutListener()
        setupNavigationListeners()
    }

    override fun onResume() {
        super.onResume()
        displayUserProfile()
    }


    /**
     * Mengambil data pengguna dari Firebase Auth & Firestore dan menampilkannya di UI.
     */
    private fun displayUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            authRepository.reloadCurrentUser()

            val user = authRepository.currentUser
            val profile = try {
                authRepository.getUserProfile()
            } catch (e: Exception) {
                null
            }

            if (user != null) {
                // Teks
                binding.tvUserName.text = profile?.name ?: user.displayName ?: "User MindEase"
                binding.tvUserBio.text = profile?.bio ?: "Always Be Happy"
                binding.tvUserEmail.text = if (user.isAnonymous) {
                    "Guest Session"
                } else {
                    user.email ?: "No Email Found"
                }

                // Ikon/Gambar
                val imageUrl = profile?.profileImageUrl ?: user.photoUrl?.toString()

                if (!imageUrl.isNullOrBlank() && imageUrl != "null") {
                    // Ada foto profil
                    Glide.with(this@ProfileFragment)
                        .load(imageUrl)
                        .circleCrop()
                        .into(binding.ivProfilePicture)
                } else {
                    // Pakai icon placeholder (warna sudah otomatis dari XML)
                    binding.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                }

            } else {
                // Sesi Guest/Error
                binding.tvUserName.text = "Sesi Berakhir"
                binding.tvUserBio.text = "Silakan login ulang"
                binding.tvUserEmail.text = ""
                binding.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    private fun setupNavigationListeners() {
        binding.tvEditProfileMenu.setOnClickListener {
            navigateToEditProfile()
        }

        binding.tvSettingsMenu.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.tvAboutAppMenu.setOnClickListener {
            val intent = Intent(requireContext(), AboutAppActivity::class.java)
            startActivity(intent)
        }
    }

    private fun navigateToEditProfile() {
        val intent = Intent(requireContext(), EditProfileActivity::class.java)
        startActivity(intent)
    }

    private fun setupLogoutListener() {
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        //
        authRepository.logout()

        Toast.makeText(context, "Anda telah keluar.", Toast.LENGTH_SHORT).show()
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