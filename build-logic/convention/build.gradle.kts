plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "movie.android.application"
            implementationClass = "com.codealphas.themovie.AndroidApplicationConventionPlugin"
        }
        register("quality") {
            id = "movie.quality"
            implementationClass = "com.codealphas.themovie.QualityConventionPlugin"
        }
        register("androidHilt") {
            id = "movie.android.hilt"
            implementationClass = "com.codealphas.themovie.HiltConventionPlugin"
        }
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.ktlint.gradle.plugin)
    compileOnly(libs.detekt.gradle.plugin)
    // AGP 9가 맞춘 KSP 2.2.10-2.0.2는 생성 파일을 android.sourceSets에 넣지 않아 컴파일에서 빠지므로, KSP 2.3.12를 클래스패스에 고정
    compileOnly(libs.ksp.symbol.processing)
    compileOnly(libs.hilt.android.gradle.plugin)
}
