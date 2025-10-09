plugins {
    id("com.android.application")
    // kalau full Java, tidak perlu kotlin plugin
}

android {
    namespace = "com.example.indokicksprototype"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.indokicksprototype"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
}
