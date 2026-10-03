plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hbatia.sleeptimer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hbatia.sleeptimer"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        resourceConfigurations += listOf("he", "en")
    }

    // חתימה קבועה: אותו מפתח בכל בנייה, כדי שעדכון יותקן מעל הגרסה הקיימת
    // בלי צורך להסיר את האפליקציה מהטלפון.
    signingConfigs {
        create("fixed") {
            storeFile = file("keystore/sleeptimer.jks")
            storePassword = "sleeptimer"
            keyAlias = "sleeptimer"
            keyPassword = "sleeptimer"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
