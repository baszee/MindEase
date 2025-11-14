// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Plugin Android & Kotlin yang sudah ada
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Plugin Google Services (Wajib untuk Firebase)
    id("com.google.gms.google-services") version "4.4.0" apply false

    // Plugin Kotlin Kapt untuk Room (Database Lokal)
    alias(libs.plugins.kotlin.kapt) apply false
}