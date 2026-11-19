plugins {
    id("com.android.application")
}

android {
    namespace = "net.micode.notes"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.micode.notes"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        // The legacy code base is full of deprecated-API warnings; keep them visible
        // without breaking command-line builds.
        abortOnError = false
    }

    // The Google Tasks sync client still uses the Apache HTTP client that was
    // removed from the default boot classpath in API 28.
    useLibrary("org.apache.http.legacy")
}

dependencies {
    // The app uses only framework APIs; no external libraries are required.
}
