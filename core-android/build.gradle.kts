plugins {
    id("movie.android.library")
    id("movie.android.compose")
    id("movie.quality")
}

android {
    namespace = "com.codealphas.themovie.core.android"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
