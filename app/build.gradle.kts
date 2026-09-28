plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val tmdbApiKey = providers.gradleProperty("TMDB_API_KEY").orElse("").get()

android {
    namespace = "com.example.streambox"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.streambox"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
        buildConfigField("String", "TMDB_API_KEY", "\"" + tmdbApiKey + "\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}
