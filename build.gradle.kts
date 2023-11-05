import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests

plugins {
    kotlin("multiplatform") version "1.9.20"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
    id("parameterize.library-conventions")
}

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js {
        browser()
        nodejs()
    }

    linuxX64()
    linuxArm64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    iosArm64()
    watchosDeviceArm64()
    mingwX64()

    targets.configureEach {
        compilations.configureEach {
            compilerOptions.configure {
                if (name == "test") {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
        }

        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val ciHostTargets = run {
    val hostTargetPrefixes = mapOf(
        "linux" to listOf("metadata", "jvm", "js", "linux", "android"),
        "macos" to listOf("macos", "ios", "watchos", "tvos"),
        "windows" to listOf("mingw")
    )

    val hostTargets = hostTargetPrefixes.mapValues { (_, prefixes) ->
        kotlin.targets.filter { target ->
            prefixes.any { target.name.startsWith(it) }
        }
    }

    val envCiHost = System.getenv("CI_HOST")
    val osName = System.getProperty("os.name")
    val host = when {
        envCiHost != null -> envCiHost.also {
            require(envCiHost in hostTargets) { "Invalid CI_HOST: $envCiHost. Must be one of: ${hostTargets.keys}" }
        }
        osName == "Linux" -> "linux"
        osName == "Mac OS X" -> "macos"
        osName.startsWith("Windows") -> "windows"
        else -> error("Unable to determine CI Host for OS: $osName. CI_HOST env can be set instead.")
    }

    // Check for non-existent, unaccounted for, or double-counted targets
    val allTargets = kotlin.targets.map { it.name }.sorted()
    val groupedTargets = hostTargets.values.flatten().map { it.name }.sorted()
    check(groupedTargets == allTargets) {
        "Bad host target grouping.\n\tExpected: $allTargets\n\tActual:   $groupedTargets"
    }

    hostTargets[host]!!.asSequence()
}

tasks.create("ciTest") {
    ciHostTargets
        .filterIsInstance<KotlinTargetWithTests<*, *>>()
        .map { target -> "${target.name}Test" }
        .forEach { targetTest ->
            dependsOn(targetTest)
        }
}

tasks.create("ciPublish") {
    ciHostTargets
        .map { it.name.uppercaseFirstChar() }
        .filter { !it.startsWith("Android") }
        .map { if (it == "Metadata") "KotlinMultiplatform" else it }
        .map { target -> "publish${target}PublicationToMavenRepository" }
        .forEach { publishTarget ->
            dependsOn(publishTarget)
        }
}
