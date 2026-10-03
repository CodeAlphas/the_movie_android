package com.codealphas.themovie

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // AGP 9 내장 Kotlin에도 Compose 컴파일러가 포함되지 않으므로, @Composable 변환을 위해 컴파일러 플러그인을 따로 적용
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.getByType<CommonExtension>().buildFeatures.compose = true

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies {
                val bom = platform(libs.findLibrary("androidx-compose-bom").get())
                add("implementation", bom)
                add("implementation", libs.findLibrary("androidx-compose-ui").get())
                add("implementation", libs.findLibrary("androidx-compose-material3").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("implementation", libs.findLibrary("androidx-activity-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
            }
        }
    }
}
