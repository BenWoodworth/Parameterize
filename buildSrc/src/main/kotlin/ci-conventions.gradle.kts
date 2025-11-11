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

import org.jetbrains.kotlin.gradle.plugin.KotlinTargetWithTests

plugins {
    kotlin("multiplatform")
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

tasks.register("ciTest") {
    ciHostTargets
        .filterIsInstance<KotlinTargetWithTests<*, *>>()
        .map { target -> "${target.name}Test" }
        .forEach { targetTest ->
            dependsOn(targetTest)
        }
}

tasks.register("ciPublish") {
    ciHostTargets
        .map { it.name.replaceFirstChar(Char::uppercase) }
        .map { if (it == "Metadata") "KotlinMultiplatform" else it }
        .map { target -> "publish${target}PublicationToMavenRepository" }
        .forEach { publishTarget ->
            dependsOn(publishTarget)
        }
}
