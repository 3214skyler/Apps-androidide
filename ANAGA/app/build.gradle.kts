plugins {
    id("com.android.application")
}

android {
    namespace = "com.anagastudio.anaga"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.anagastudio.anaga"
        minSdk = 21
        targetSdk = 33

        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
