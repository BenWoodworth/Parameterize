# Parameterize

[![Maven Central](https://img.shields.io/maven-central/v/com.benwoodworth.parameterize/parameterize)](https://search.maven.org/search?q=g:com.benwoodworth.parameterize)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.benwoodworth.parameterize/parameterize?server=https%3A%2F%2Fs01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/benwoodworth/parameterize/)
[![KDoc](https://img.shields.io/badge/api-KDoc-blue)](https://benwoodworth.github.io/Parameterize/parameterize/com.benwoodworth.parameterize/parameterize.html)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Slack channel](https://img.shields.io/badge/chat-slack-blue.svg?logo=slack)](https://kotlinlang.slack.com/messages/parameterize/)

Parameterize is a multiplatform Kotlin library that brings a concise syntax for parameterizing code.
With the parameters defined inline, the `parameterize` block will be run for each combination of `parameter` arguments,
simplifying the exhaustive exploration of code paths.

```kotlin
parameterize {
    val letter by parameter('a'..'z')
    val primeUnder20 by parameterOf(2, 3, 5, 7, 11, 13, 17, 19)
    val lazyValue by parameter { slowArgumentsComputation() }

    // ...
}
```

With parameterized testing being the motivating use case, `parameterize` can be used to cleanly and more idiomatically
cover edge cases in tests. For example, succinctly covering all the possible ways a `substring` can be contained within
a `string`, running the `parameterize` block once for each case:

```kotlin
val string = "prefix-substring-suffix"  // in the middle
val string = "substring-suffix"         // at the start
val string = "prefix-substring"         // at the end
val string = "substring"                // the entire string
```

```kotlin
@Test
fun contains_with_the_substring_present_should_be_true() = parameterize {
    val substring = "substring"
    val prefix by parameterOf("prefix-", "")
    val suffix by parameterOf("-suffix", "")

    val string = "$prefix$substring$suffix"
    assertTrue(string.contains(substring), "\"$string\".contains(\"$substring\")")
}
```

If the test fails, the cause will be wrapped into an `Error` detailing the <ins>*used*</ins> parameters with their
arguments <ins>*and parameter names*</ins>:

```java
com.benwoodworth.parameterize.ParameterizeFailedError: Failed 2/4 cases
	AssertionFailedError: "prefix-substring-suffix".contains("substring")
	AssertionFailedError: "prefix-substring".contains("substring")
	Suppressed: com.benwoodworth.parameterize.Failure: Failed with arguments:
		prefix = prefix-
		suffix = -suffix
	Caused by: org.opentest4j.AssertionFailedError: "prefix-substring-suffix".contains("substring")
		at kotlin.test.AssertionsKt.assertTrue(Unknown Source)
		at ContainsSpec$contains_with_the_substring_present_should_be_true$1.invoke(ContainsSpec.kt:13)
		at ContainsSpec$contains_with_the_substring_present_should_be_true$1.invoke(ContainsSpec.kt:7)
		at com.benwoodworth.parameterize.ParameterizeKt.parameterize(Parameterize.kt:91)
	Suppressed: com.benwoodworth.parameterize.Failure: Failed with arguments:
		prefix = prefix-
		suffix = 
	Caused by: org.opentest4j.AssertionFailedError: "prefix-substring".contains("substring")
		at kotlin.test.AssertionsKt.assertTrue(Unknown Source)
		at ContainsSpec$contains_with_the_substring_present_should_be_true$1.invoke(ContainsSpec.kt:13)
		at ContainsSpec$contains_with_the_substring_present_should_be_true$1.invoke(ContainsSpec.kt:7)
		at com.benwoodworth.parameterize.ParameterizeKt.parameterize(Parameterize.kt:91)
```

Parameters are also designed to be flexible, depend on other parameters, be called conditionally, or even used a loop to
declare multiple parameters from the same property. Features which are especially useful for covering edge/corner cases:

```kotlin
@Test
fun int_should_not_equal_a_different_int() = parameterize {
    val int by parameterOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE)
    val differentInt by parameterOf(int + 1, int - 1)

    assertNotEquals(int, differentInt)
}
```

## Setup

### Gradle

```kotlin
// build.gradle.kts

plugins {
    kotlin("jvm") version "1.9.20" // or kotlin("multiplatform"), etc.
}

repositories {
    mavenCentral()
    //maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("com.benwoodworth.parameterize:parameterize:$parameterize_version") // or testImplementation(...)
}
```
