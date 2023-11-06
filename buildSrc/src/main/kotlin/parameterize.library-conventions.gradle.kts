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
                    name = "GNU General Public License version 3"
                    url = "https://opensource.org/licenses/GPL-3.0"
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
