// baszee/mindease/MindEase-4e8b5bcc941bcf8b2f040d5689f753109d558dca/build.gradle.kts
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Plugin Android & Kotlin yang sudah ada
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Plugin Google Services (Wajib untuk Firebase)
    id("com.google.gms.google-services") version "4.4.0" apply false

    // Plugin Kotlin Kapt untuk Room (Database Lokal)
    alias(libs.plugins.kotlin.ksp) apply false

    // 🔥 FIX: Firebase Crashlytics Plugin dengan versi
    id("com.google.firebase.crashlytics") version "2.9.9" apply false

    // 🔥 FIX: Firebase Performance Monitoring Plugin dengan versi
    id("com.google.firebase.firebase-perf") version "1.4.2" apply false
}