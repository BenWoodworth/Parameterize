# Parameterize

[![Maven Central](https://img.shields.io/maven-central/v/com.benwoodworth.parameterize/parameterize-core)](https://central.sonatype.com/search?namespace=com.benwoodworth.parameterize&sort=name)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.benwoodworth.parameterize/parameterize?server=https%3A%2F%2Fs01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/benwoodworth/parameterize-core/)
[![KDoc](https://img.shields.io/badge/api-KDoc-blue)](https://benwoodworth.github.io/Parameterize/parameterize/com.benwoodworth.parameterize/parameterize.html)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Slack channel](https://img.shields.io/badge/chat-slack-blue.svg?logo=slack)](https://kotlinlang.slack.com/messages/parameterize/)

Parameterize is a multiplatform Kotlin library introducing a concise, idiomatic style of parameterizing code. Having
parameters be declared within the logic, potentially conditionally or with dynamic arguments, it's possible to model
complicated control flow scenarios much more cleanly.

```kotlin
parameterize {
    val letter by parameter('a'..'z')
    val primeUnder20 by parameterOf(2, 3, 5, 7, 11, 13, 17, 19)
    val computedValue by parameter { slowArgumentsComputation() }

    // ...
}
```

With its default behavior, `parameterize` is strictly an alternative syntax to nested `for` loops, with loop variables
defined within the body instead of up front, and without the indentation that's required for additional inner loops.

<table>
<tr>
<th>Example <code>parameterize</code> loop</th>
<th>Equivalent <code>for</code> loops</th>
</tr>

<tr>
<td>

```kotlin
val reddishYellows = sequence {
    parameterize {
        val red by parameter(128..255)
        val green by parameter(64..(red - 32))
        val blue by parameter(0..(green - 64))

        yield(Color(red, green, blue))
    }
}
```

</td>
<td>

```kotlin
val reddishYellows = sequence {
    for (red in 128..255) {
        for (green in 64..(red - 32)) {
            for (blue in 0..(green - 64)) {
                yield(Color(red, green, blue))
            }
        }
    }
}
```

</td>
</tr>
</table>

In addition to its default behavior, `parameterize` has a configuration with options to decorate its iterations, handle
and record failures, and summarize the overall loop execution. The flexibility `parameterize` offers makes it suitable
for many different specific use cases, including built in ways to access the named parameter arguments when a failure
occurs, recording failures while continuing to the next iteration, and throwing comprehensive multi-failures that list
recorded failures with parameter information.

## Parameterized Testing

With parameterized testing being the motivating use case for this library, `parameterize` can be used to cleanly and
more idiomatically cover edge cases while testing. As an example, here is a test that succinctly covers all the possible
ways a `substring` can be contained within a `string`, running the `parameterize` block once for each case:

```kotlin
val string = "prefix-substring-suffix"  // in the middle
val string = "substring-suffix"         // at the start
val string = "prefix-substring"         // at the end
val string = "substring"                // the entire string
```

```kotlin
// See full test suite examples below, with `parameterizeTest {...}` configured for testing
fun a_string_should_contain_its_own_substring() = parameterizeTest {
    val substring = "substring"
    val prefix by parameterOf("prefix-", "")
    val suffix by parameterOf("-suffix", "")

    val string = "$prefix$substring$suffix"

    assertTrue(string.contains(substring), "\"$string\".contains(\"$substring\")")
}
```

If any of the test cases don't pass, the failures will be wrapped into an `Error` detailing the parameters with their
arguments <ins>*and parameter names*</ins> for each, plus support for JVM tooling with
[expected/actual value comparison](http://ota4j-team.github.io/opentest4j/docs/current/api/org/opentest4j/AssertionFailedError.html)
and [multi-failures](http://ota4j-team.github.io/opentest4j/docs/current/api/org/opentest4j/MultipleFailuresError.html):

<details>
<summary><b>Multi-failure stack trace</b></summary>

```java
com.benwoodworth.parameterize.ParameterizeFailedError: Failed 2/4 cases
	AssertionFailedError: "prefix-substring-suffix".contains("substring")
	AssertionFailedError: "prefix-substring".contains("substring")
	Suppressed: com.benwoodworth.parameterize.Failure: Failed with arguments:
		prefix = prefix-
		suffix = -suffix
	Caused by: org.opentest4j.AssertionFailedError: "prefix-substring-suffix".contains("substring")
		at kotlin.test.AssertionsKt.assertTrue(Assertions.kt:44)
		at ContainsSpec.a_string_should_contain_its_own_substring(ContainsSpec.kt:18)
		at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:86)
	Suppressed: com.benwoodworth.parameterize.Failure: Failed with arguments:
		prefix = prefix-
		suffix = 
	Caused by: org.opentest4j.AssertionFailedError: "prefix-substring".contains("substring")
		at kotlin.test.AssertionsKt.assertTrue(Assertions.kt:44)
		at ContainsSpec.a_string_should_contain_its_own_substring(ContainsSpec.kt:18)
		at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:86)
```

</details>

The parameters are designed to be flexible, being able to depend on other parameters, be declared conditionally, or even
used in a loop to declare multiple parameters from the same property. Features which are especially useful for covering
edge/corner cases:

```kotlin
@Test
fun an_int_should_not_equal_a_different_int() = parameterizeTest {
    val int by parameterOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE)
    val differentInt by parameterOf(int + 1, int - 1)

    assertNotEquals(int, differentInt)
}
```

### Test Suite Examples

<details>
<summary><b>
    Annotation-based frameworks
    (<a href="https://kotlinlang.org/api/latest/kotlin.test/">kotlin.test</a>,
    <a href="https://junit.org/">JUnit</a>,
    <a href="https://testng.org/">TestNG</a>,
    ...)
</b></summary>

Using the `decorator` configuration option, `parameterize` can be configured to trigger a test framework's before/after
hooks for each of its iterations. Additionally the `onFailure` handler can be used to record failures and continue to
the next iteration, making `parameterize` report a comprehensive multi-failure instead of just throwing. In this
[kotlin.test](https://kotlinlang.org/api/latest/kotlin.test/) example, `parameterizeTest` wraps a pre-configured
`parameterize` call to avoid the boilerplate, and have it be easily accessible to any test suite by extending the
`TestingContext` class:

```kotlin
abstract class TestingContext {
    open fun beforeTest() {}
    open fun afterTest() {}

    // The annotations would be lost when overriding beforeTest/afterTest,
    // so hook in here instead of relying on the subclasses to apply them.
    @BeforeTest
    fun beforeTestHook(): Unit = beforeTest()

    @AfterTest
    fun afterTestHook(): Unit = afterTest()


    protected inline fun parameterizeTest(
        recordFailures: Long = someDefault, // Example of how `parameterize` could get wrapped,
        maxFailures: Long = Long.MAX_VALUE, // exposing options according to the testing needs.
        block: ParameterizeScope.() -> Unit
    ): Unit = parameterize(
        // Inserts before & after calls around each test case,
        // except where already invoked by the test framework.
        decorator = { testCase ->
            if (!isFirstIteration) beforeTest()
            testCase()
            if (!isLastIteration) afterTest()
        },

        onFailure = { failure ->
            recordFailure = failureCount <= recordFailures
            breakEarly = failureCount >= maxFailures
        }
    ) {
        block()
    }
}
```
```kotlin
class ContainsSpec : TestingContext() {
    override fun beforeTest() {
        // ...
    }

    override fun afterTest() {
        // ...
    }
    
    @Test
    fun a_string_should_contain_its_own_substring() = parameterizeTest {
        val substring = "substring"
        val prefix by parameterOf("prefix-", "")
        val suffix by parameterOf("-suffix", "")

        val string = "$prefix$substring$suffix"

        assertTrue(string.contains(substring), "\"$string\".contains(\"$substring\")")
    }
}
```

</details>

<details>
<summary><b>
    DSL-based frameworks
    (<a href="https://kotest.io/docs/framework/framework.html">Kotest</a>,
    <a href="https://www.spekframework.org/">Spek</a>,
    <a href="https://gitlab.com/opensavvy/prepared">Prepared</a>,
    ...)
</b></summary>

With test frameworks that declare tests dynamically, it's possible to produce a suite of *separate* tests by declaring
a single test within a parameterized group. With Kotest's [Fun Spec](https://kotest.io/docs/framework/testing-styles.html#fun-spec),
for example, this code will register four tests that will be reported separately by the test runner when executed, all
grouped together under the one test `context`:

```kotlin
└─ A string should contain its own substring
   ├─ "prefix-substring-suffix".contains("substring")
   ├─ "prefix-substring".contains("substring")
   ├─ "substring-suffix".contains("substring")
   └─ "substring".contains("substring")
```
```kotlin
context("A string should contain its own substring") {
    parameterize {
        val substring = "substring"
        val prefix by parameterOf("prefix-", "")
        val suffix by parameterOf("-suffix", "")

        val string = "$prefix$substring$suffix"

        test("\"$string\".contains(\"$substring\")") {
            string.contains(substring) shouldBe true
        }
    }
}
```

In the future, it will likely be possible for this to be written more nicely once Kotlin supports decorators, removing
the need for an extra level of nesting nesting inside the group of tests.
([See here](https://youtrack.jetbrains.com/issue/KT-49904/Decorators#focus=Comments-27-8465650.0-0))

</details>

## Setup

### Modules

| Artifact            | Description                                                                                       |
|---------------------|---------------------------------------------------------------------------------------------------|
| `parameterize-api`  | Library primitives, including the `ParameterizeScope` interface and `Parameter` functions.        |
| `parameterize-core` | The core functionality, with `parameterize {}` as the entry point for running parameterized code. |


### Gradle

```kotlin
// build.gradle.kts

plugins {
    kotlin("jvm") version "2.0.20" // or kotlin("multiplatform"), etc.
}

repositories {
    mavenCentral()
    //maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("com.benwoodworth.parameterize:parameterize-core:$parameterize_version") // or testImplementation(...)
    //api("com.benwoodworth.parameterize:parameterize-api:$parameterize_version") // for libraries that expose parameter DSLs
}
```

### A note about stability

While Parameterize is in beta, there may be source/binary/behavioral changes in new minor (v0.#.0) releases. Any
breaking changes will be documented on release, with
[automatic replacements](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-deprecated/replace-with.html)
for source-breaking changes provided where possible.

That said, the library is thoroughly tested, and the `parameter` DSL is unlikely to drastically change, with most of the
library's evolution expected to be isolated to configuration. So in scenarios where binary compatibility isn't a
concern, and changes to configuration are acceptable, I consider Parameterize ready to be used in the wild. Of course
exercise caution with these earlier releases, as they have not yet been battle tested. But as a strong believer of
dogfooding in software, I am already using it for testing in projects of my own. And in case of any major bugs, I will
make sure they are addressed in a timely manner.

I designed the library to address pain points I found in the rigidness of other parameterized/property-based testing
libraries, and have been very happy with some new patterns that have emerged from the flexible code that Parameterize
enables. I'm planning on documenting these at some point, and encourage discussion and code sharing in the Slack channel
linked at the top :)
