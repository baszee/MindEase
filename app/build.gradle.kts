// baszee/mindease/MindEase-4e8b5bcc941bcf8b2f040d5689f753109d558dca/app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.ksp)
    // 🔥 PLUGINS TAMBAHAN (GRATIS)
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // FIX KRITIS: Menangani duplikasi file META-INF
    packaging {
        resources {

            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // ==================== FIREBASE BOM (STABIL VERSION) ====================
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))

    // ==================== CORE ANDROID ====================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Material Components
    implementation("com.google.android.material:material:1.11.0")

    implementation("androidx.preference:preference-ktx:1.2.1")

    // ==================== GRAFIK ====================
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")


    // ==================== LIFECYCLE & COROUTINES ====================
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ==================== FIREBASE (Semua di bawah ini GRATIS di Spark Plan) ====================
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-config-ktx")

    // 🔥 ANALYTICS, CRASHLYTICS, PERF MONITORING (GRATIS)
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // ==================== ROOM DATABASE ====================
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    ksp("androidx.room:room-compiler:$roomVersion") // FIX: Menggunakan KSP

    // ==================== IMAGE LOADING ====================
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0") // FIX: Menggunakan KSP

    // ==================== NETWORKING ====================
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ==================== TESTING ====================
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // 🔥 Tambahkan Library Image Compressor 🔥
    implementation("id.zelory:compressor:3.0.1")

    // Room testing
    testImplementation("androidx.room:room-testing:$roomVersion")

    // Coroutines testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}