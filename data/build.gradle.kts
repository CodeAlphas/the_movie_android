plugins {
    id("movie.android.library")
    // app의 @HiltAndroidApp 코드 생성은 Hilt 컴파일러가 각 모듈에 남긴 메타데이터 클래스로만 @InstallIn 모듈을 찾으므로,
    // data의 Repository, 네트워크, DB 바인딩이 앱 의존성 그래프에 들어가도록 data에서도 Hilt 컴파일러를 실행하는 movie.android.hilt 적용
    id("movie.android.hilt")
    id("movie.quality")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    // KSP 인자 room.schemaLocation은 경로 문자열만 넘겨 Gradle이 스키마 파일이 바뀌거나 지워져도 KSP를 다시 실행하지 않으므로,
    // 스키마 파일이 빠지거나 예전 내용으로 남지 않도록 Room Gradle 플러그인 적용
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.codealphas.themovie.data"

    testOptions {
        // Robolectric이 테스트를 시작할 때 쓰는 JDK 내부 패키지 jdk.internal.access를 JDK가 외부 코드에 막아 두어 테스트가 시작 전에 실패하므로,
        // 단위 테스트 JVM에 이 패키지를 여는 실행 옵션 추가
        unitTests.all { it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
}

// AppDatabase는 exportSchema가 기본값 true라 스키마 JSON을 내보낼 위치가 필요하므로,
// DB 버전을 올릴 때 이전 스키마와 비교해 마이그레이션을 작성할 수 있도록 schemas/<Database 전체 클래스 이름>/<버전>.json에 저장
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.androidx.annotation.experimental)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    // @HiltWorker는 Dagger Hilt 컴파일러가 처리하지 않으므로, HiltWorkerFactory가 Worker를 만들 수 있도록 AndroidX Hilt 컴파일러 추가
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    // Firebase Auth, Realtime Database, Storage는 결과를 Task 콜백으로 주므로, 코루틴에서 결과를 기다리도록 await 확장 추가
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
}
