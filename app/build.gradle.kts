plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Terapkan plugin Google Services di sini
    //id("com.google.gms.google-services")

    // Terapkan plugin Kotlin Kapt di sini (untuk Room)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.mindease.mindeaseapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mindease.mindeaseapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // Dianjurkan menggunakan 17/18
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // AKTIFKAN VIEW BINDING
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // --- Dependencies yang sudah ada ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Kebutuhan Grafik (MPAndroidChart)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- Kebutuhan Arsitektur (Fragment, ViewModel, LiveData) ---
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // --- Kebutuhan Firebase & Google Sign-In ---
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation("com.google.firebase:firebase-auth-ktx") // 1. Firebase Auth
    implementation("com.google.android.gms:play-services-auth:20.7.0") // 2. Google Sign-In
    implementation("com.google.firebase:firebase-firestore-ktx") // 3. Firestore
    implementation("com.google.firebase:firebase-analytics-ktx") // 4. Analytics

    // --- Kebutuhan Room Database (Penyimpanan Lokal) ---
    val roomVersion = "2.6.0" // Room Version Terbaru

    // Library Room untuk Kotlin
    implementation("androidx.room:room-ktx:$roomVersion")

    // Kapt (Kotlin Annotation Processing Tool) untuk memproses Room
    kapt("androidx.room:room-compiler:$roomVersion")
}