/*
 * Copyright 2024 Ben Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests
import java.net.URL

plugins {
    kotlin("multiplatform") version "2.0.20"
    id("org.jetbrains.dokka") version "1.9.20"
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.16.3"
    id("parameterize.library-conventions")
}

repositories {
    mavenCentral()
}

@OptIn(ExperimentalWasmDsl::class)
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

    wasmJs {
        browser()
        nodejs()
    }
    wasmWasi {
        nodejs()
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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
        val jvmMain by getting {
            dependencies {
                implementation("org.opentest4j:opentest4j:1.3.0")
            }
        }
    }
}

val ciHostTargets = run {
    val hostTargetPrefixes = mapOf(
        "linux" to listOf("metadata", "jvm", "js", "linux", "android", "wasm"),
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
        .map { if (it == "Metadata") "KotlinMultiplatform" else it }
        .map { target -> "publish${target}PublicationToMavenRepository" }
        .forEach { publishTarget ->
            dependsOn(publishTarget)
        }
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        reportUndocumented = true
        failOnWarning = true

        val releaseVersionRef = version.toString()
            .takeIf { version -> version.matches(Regex("""\d+\.\d+\.\d+""")) }
            ?.let { version -> "v$version" }

        if (releaseVersionRef != null) {
            sourceLink {
                localDirectory = projectDir.resolve("src")
                remoteUrl.set(URL("https://github.com/BenWoodworth/Parameterize/tree/$releaseVersionRef/src"))
                remoteLineSuffix = "#L"
            }
        }
    }

    doLast {
        layout.buildDirectory.asFileTree.asSequence()
            .filter { it.isFile && it.extension == "html" }
            .forEach { file ->
                file.readText()
                    // Remove "ParameterizeConfiguration." prefix from link text for *Scope classes
                    .replace(Regex("""(?<=>)ParameterizeConfiguration\.(?=\w+Scope</a>)"""), "")
                    .let { file.writeText(it) }
            }
    }
}
