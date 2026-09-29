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
        versionCode = 9
        versionName = "1.7.0"
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
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.8.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.8.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
