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

val ciVersion: String? = System.getenv("CI_VERSION")

val ossrhUsername: String? = System.getenv("OSSRH_USERNAME")
val ossrhPassword: String? = System.getenv("OSSRH_PASSWORD")

val signingKeyId: String? = System.getenv("SIGNING_KEY_ID")
val signingKey: String? = System.getenv("SIGNING_KEY")
val signingPassword: String? = System.getenv("SIGNING_PASSWORD")

plugins {
    `maven-publish`
    signing
}

if (ciVersion != null) version = ciVersion
val isSnapshot = version.toString().contains("SNAPSHOT", true)

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier = "javadoc"
}

publishing {
    repositories {
        maven {
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            url = if (isSnapshot) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }

    publications.withType<MavenPublication> {
        artifact(javadocJar.get())

        pom {
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
