plugins {
    id("movie.android.application")
    id("movie.android.hilt")
    id("movie.quality")
    alias(libs.plugins.google.services)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.codealphas.themovie"

    defaultConfig {
        applicationId = "com.codealphas.themovie"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    lint {
        error += setOf("HardcodedText", "SetTextI18n")
    }
}

dependencies {
    implementation(project(":presentation"))
    implementation(project(":data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kakao.map)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
