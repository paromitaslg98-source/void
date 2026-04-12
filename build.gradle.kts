plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.knownassurajit.app.launcher.voidlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.knownassurajit.app.launcher.void"
        minSdk = 26
        targetSdk = 36

        // Deterministic versioning formula
        val major = 0
        val minor = 0
        val patch = 0
        val build = 8
        
        versionCode = major * 1_000_000 + minor * 10_000 + patch * 100 + build
        versionName = "$major.$minor.$patch.$build"

        resourceConfigurations += listOf("en", "ar", "de", "es-rES", "es-rUS", "fr", "he", "hr", "hu", "in", "it", "ja", "nl", "pl", "pt-rBR", "ru-rRU", "sv", "tr", "uk", "zh")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.kotlin.stdlib)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.process)
    implementation(libs.work.runtime.ktx)
    implementation(libs.material)

    // Compose BOM 
    implementation(platform("androidx.compose:compose-bom:2025.05.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.01"))
    
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.10.6")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // ML Kit GenAI
    implementation("com.google.mlkit:genai-summarization:1.0.0-beta1")
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation(libs.junit)
}
