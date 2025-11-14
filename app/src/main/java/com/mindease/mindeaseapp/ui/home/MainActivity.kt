package com.mindease.mindeaseapp.ui.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.mindease.mindeaseapp.R
import com.mindease.mindeaseapp.databinding.ActivityMainBinding
import com.mindease.mindeaseapp.ui.breathing.BreathingFragment
import com.mindease.mindeaseapp.ui.journal.JournalFragment
import com.mindease.mindeaseapp.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Deklarasi Fragments
    private val dashboardFragment = DashboardFragment()
    private val journalFragment = JournalFragment()
    private val breathingFragment = BreathingFragment()
    private val profileFragment = ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Muat Fragment default (Home/Dashboard) saat aplikasi pertama kali dibuka
        replaceFragment(dashboardFragment)

        // Siapkan listener untuk Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(dashboardFragment)
                R.id.nav_journal -> replaceFragment(journalFragment)
                R.id.nav_breathing -> replaceFragment(breathingFragment)
                R.id.nav_profile -> replaceFragment(profileFragment)
            }
            true
        }
    }

    /**
     * Mengganti Fragment yang ditampilkan di FrameLayout container.
     */
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            commit()
        }
    }
}