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

val ossrhUsername: String? = System.getenv("OSSRH_USERNAME")
val ossrhPassword: String? = System.getenv("OSSRH_PASSWORD")

val signingKeyId: String? = System.getenv("SIGNING_KEY_ID")
val signingKey: String? = System.getenv("SIGNING_KEY")
val signingPassword: String? = System.getenv("SIGNING_PASSWORD")

plugins {
    `maven-publish`
    signing
}

val relocatedArtifactIds = listOf(
    "parameterize-core",
    "parameterize-core-androidnativearm32",
    "parameterize-core-androidnativearm64",
    "parameterize-core-androidnativex64",
    "parameterize-core-androidnativex86",
    "parameterize-core-iosarm64",
    "parameterize-core-iossimulatorarm64",
    "parameterize-core-iosx64",
    "parameterize-core-js",
    "parameterize-core-jvm",
    "parameterize-core-linuxarm64",
    "parameterize-core-linuxx64",
    "parameterize-core-macosarm64",
    "parameterize-core-macosx64",
    "parameterize-core-mingwx64",
    "parameterize-core-tvosarm64",
    "parameterize-core-tvossimulatorarm64",
    "parameterize-core-tvosx64",
    "parameterize-core-wasm-js",
    "parameterize-core-wasm-wasi",
    "parameterize-core-watchosarm32",
    "parameterize-core-watchosarm64",
    "parameterize-core-watchosdevicearm64",
    "parameterize-core-watchossimulatorarm64",
    "parameterize-core-watchosx64"
)

val relocatedVersion = "0.4.0"

publishing {
    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }

    publications {
        for (relocatedArtifactId in relocatedArtifactIds) {
            create<MavenPublication>("relocate-to-$relocatedArtifactId") {
                groupId = "com.benwoodworth.parameterize"
                artifactId = relocatedArtifactId.replace("-core", "")
                version = relocatedVersion

                pom {
                    distributionManagement {
                        relocation {
                            artifactId = relocatedArtifactId
                            message = "Moved to parameterize-core"
                        }
                    }

                    name = "Parameterize"
                    description = "Kotlin DSL for clean parameterized code"
                    url = "https://github.com/BenWoodworth/Parameterize"

                    licenses {
                        license {
                            name = "The Apache Software License, Version 2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    developers {
                        developer {
                            id = "BenWoodworth"
                            name = "Ben Woodworth"
                            email = "ben@benwoodworth.com"
                        }
                    }
                    scm {
                        url = "https://github.com/BenWoodworth/Parameterize"
                    }
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)

    gradle.taskGraph.whenReady {
        isRequired = allTasks.any { it is PublishToMavenRepository }
    }

    // https://github.com/gradle/gradle/issues/26091#issuecomment-1722947958
    tasks.withType<AbstractPublishToMaven>().configureEach {
        val signingTasks = tasks.withType<Sign>()
        mustRunAfter(signingTasks)
    }
}
