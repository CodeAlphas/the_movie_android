// AGP 9는 더 낮은 KSP를 2.2.10-2.0.2로 올리므로, android.sourceSets를 쓰는 버전을 직접 고정한다.
buildscript {
    dependencies {
        classpath(libs.ksp.symbol.processing)
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.secrets) apply false
}
