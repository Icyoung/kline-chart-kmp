import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
    alias(libs.plugins.maven.publish)
}

group = "io.github.icyoung"
version = libs.versions.klineChart.get()

val hasSigningCredentials =
    providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent ||
        providers.environmentVariable("SIGNING_IN_MEMORY_KEY").isPresent

kotlin {
    androidLibrary {
        namespace = "io.github.icyoung"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "Icyoung/kline-chart-kmp"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    if (hasSigningCredentials) {
        signAllPublications()
    }

    coordinates(
        groupId = "io.github.icyoung",
        artifactId = "kline-chart-kmp",
        version = libs.versions.klineChart.get(),
    )

    pom {
        name.set("kline-chart-kmp")
        description.set("Kotlin Multiplatform OHLCV/K-line chart components and primitives.")
        inceptionYear.set("2026")
        url.set("https://github.com/Icyoung/kline-chart-kmp")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("Icyoung")
                name.set("Icy")
                url.set("https://github.com/Icyoung")
            }
        }
        scm {
            url.set("https://github.com/Icyoung/kline-chart-kmp")
            connection.set("scm:git:https://github.com/Icyoung/kline-chart-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com:Icyoung/kline-chart-kmp.git")
        }
    }
}
