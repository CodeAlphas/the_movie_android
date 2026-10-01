plugins {
    id("movie.android.library")
    id("movie.android.compose")
    // app의 @HiltAndroidApp 코드 생성은 Hilt 컴파일러가 각 모듈에 남긴 메타데이터 클래스로만 @AndroidEntryPoint와 @InstallIn 모듈을 찾으므로,
    // 화면, ViewModel, LocationModule이 앱 의존성 그래프에 들어가도록 presentation에서도 Hilt 컴파일러를 실행하는 movie.android.hilt 적용
    id("movie.android.hilt")
    id("movie.quality")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.codealphas.themovie.presentation"

    buildFeatures {
        viewBinding = true
    }
    lint {
        error += setOf("HardcodedText", "SetTextI18n")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core-android"))
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kakao.map)
    implementation(libs.play.services.location)
    implementation(libs.android.youtube.player)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
