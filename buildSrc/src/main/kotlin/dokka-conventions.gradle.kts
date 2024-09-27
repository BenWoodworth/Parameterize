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

import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        reportUndocumented = true
        failOnWarning = true

        // TODO Suppress friend api annotations?

        val releaseVersionRef = version.toString()
            .takeIf { version -> version.matches(Regex("""\d+\.\d+\.\d+""")) }
            ?.let { version -> "v$version" }

        if (releaseVersionRef != null) {
            sourceLink {
                localDirectory = rootDir
                remoteUrl = URL("https://github.com/BenWoodworth/Parameterize/tree/$releaseVersionRef")
                remoteLineSuffix = "#L"
            }
        }
    }
}
