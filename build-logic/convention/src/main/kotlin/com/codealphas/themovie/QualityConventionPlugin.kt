package com.codealphas.themovie

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
            pluginManager.apply("dev.detekt")
            extensions.configure<KtlintExtension> {
                android.set(true)
            }
            extensions.configure<DetektExtension> {
                buildUponDefaultConfig.set(true)
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                parallel.set(true)
                baseline.set(layout.projectDirectory.file("detekt-baseline.xml"))
            }
        }
    }
}
