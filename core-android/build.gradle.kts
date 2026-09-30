plugins {
    id("movie.android.library")
    id("movie.android.compose")
    id("movie.quality")
}

android {
    namespace = "com.codealphas.themovie.core.android"
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
}
