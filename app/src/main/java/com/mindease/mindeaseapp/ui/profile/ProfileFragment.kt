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
import com.mindease.mindeaseapp.utils.AuthResult
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleSignInClient: GoogleSignInClient

    private val authRepository: AuthRepository by lazy {
        AuthRepository(Firebase.auth, FirebaseFirestore.getInstance())
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

        setupGoogleSignInClient()
        displayUserProfile()
        setupLogoutListener()
        setupNavigationListeners()

        // 🔥 BARU: Setup verification banner
        setupVerificationBanner()
    }

    override fun onResume() {
        super.onResume()
        displayUserProfile()
        setupVerificationBanner() // Update banner setiap resume
    }

    private fun setupGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    // 🔥 BARU: Tampilkan banner jika email belum diverifikasi
    private fun setupVerificationBanner() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Cek apakah user Email/Password dan belum verifikasi
            if (authRepository.isEmailPasswordUser() && !authRepository.isEmailVerified()) {
                // Tampilkan banner
                binding.verificationBanner.visibility = View.VISIBLE

                // Button "Verifikasi Sekarang"
                binding.btnVerifyNow.setOnClickListener {
                    sendVerificationEmail()
                }

                // Button "Nanti Saja" (dismiss banner sementara)
                binding.btnVerifyLater.setOnClickListener {
                    binding.verificationBanner.visibility = View.GONE
                }
            } else {
                // Sembunyikan banner jika sudah verifikasi atau bukan email/pass user
                binding.verificationBanner.visibility = View.GONE
            }
        }
    }

    // 🔥 BARU: Kirim email verifikasi
    private fun sendVerificationEmail() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.btnVerifyNow.isEnabled = false
            binding.btnVerifyNow.text = "Mengirim..."

            val result = authRepository.sendEmailVerification()

            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "✅ Email verifikasi telah dikirim! Cek inbox Anda.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.verificationBanner.visibility = View.GONE
                }
                is AuthResult.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "❌ Gagal mengirim email: ${result.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.btnVerifyNow.isEnabled = true
                    binding.btnVerifyNow.text = "Verifikasi Sekarang"
                }
                else -> {}
            }
        }
    }

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
                val isGuest = user.isAnonymous

                binding.tvUserName.text = profile?.name ?: user.displayName ?: "User MindEase"
                binding.tvUserBio.text = profile?.bio ?: "Always Be Happy"
                binding.tvUserEmail.text = if (isGuest) {
                    "Guest Session"
                } else {
                    user.email ?: "No Email Found"
                }

                // Hide Edit Profile untuk Guest
                if (isGuest) {
                    binding.tvEditProfileMenu.visibility = View.GONE
                } else {
                    binding.tvEditProfileMenu.visibility = View.VISIBLE
                }

                val imageUrl = profile?.profileImageUrl ?: user.photoUrl?.toString()

                if (!imageUrl.isNullOrBlank() && imageUrl != "null") {
                    Glide.with(this@ProfileFragment)
                        .load(imageUrl)
                        .circleCrop()
                        .into(binding.ivProfilePicture)
                } else {
                    binding.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                }

            } else {
                binding.tvUserName.text = "Sesi Berakhir"
                binding.tvUserBio.text = "Silakan login ulang"
                binding.tvUserEmail.text = ""
                binding.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                binding.tvEditProfileMenu.visibility = View.GONE
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
        googleSignInClient.signOut().addOnCompleteListener {
            authRepository.logout()

            Toast.makeText(context, "Anda telah keluar.", Toast.LENGTH_SHORT).show()
            val intent = Intent(activity, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}