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

    lint {
        error += setOf("HardcodedText", "SetTextI18n")
    }

    testOptions {
        // 로컬 단위 테스트는 기본으로 Android 리소스를 담지 않아 Robolectric 화면 테스트에서 stringResource와 테마를 읽지 못하므로,
        // 화면 테스트가 실제 문자열과 테마로 그리도록 리소스 포함
        unitTests.isIncludeAndroidResources = true
        // Robolectric은 테스트 시작 때 JDK 내부의 jdk.internal.access를 리플렉션으로 부르는데 JDK가 이 패키지를 공개하지 않아
        // 화면 테스트가 시작 전에 실패하므로, 단위 테스트 JVM에 이 패키지를 공개하도록 추가
        unitTests.all { it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core-android"))
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kakao.map)
    implementation(libs.play.services.location)
    implementation(libs.android.youtube.player)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.espresso.core)
    // createComposeRule이 띄우는 빈 ComponentActivity가 테스트 매니페스트에 등록되도록 ui-test-manifest 추가
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
