# Parameterize

[![Maven Central](https://img.shields.io/maven-central/v/com.benwoodworth.parameterize/parameterize)](https://central.sonatype.com/search?namespace=com.benwoodworth.parameterize&sort=name)
[![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.benwoodworth.parameterize/parameterize?server=https%3A%2F%2Fs01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/benwoodworth/parameterize/)
[![KDoc](https://img.shields.io/badge/api-KDoc-blue)](https://benwoodworth.github.io/Parameterize/parameterize/com.benwoodworth.parameterize/parameterize.html)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
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
fun a_string_should_contain_its_own_substring() = parameterize {
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
		at ContainsSpec$a_string_should_contain_its_own_substring$1.invoke(ContainsSpec.kt:13)
		at ContainsSpec$a_string_should_contain_its_own_substring$1.invoke(ContainsSpec.kt:7)
		at com.benwoodworth.parameterize.ParameterizeKt.parameterize(Parameterize.kt:91)
	Suppressed: com.benwoodworth.parameterize.Failure: Failed with arguments:
		prefix = prefix-
		suffix = 
	Caused by: org.opentest4j.AssertionFailedError: "prefix-substring".contains("substring")
		at kotlin.test.AssertionsKt.assertTrue(Unknown Source)
		at ContainsSpec$a_string_should_contain_its_own_substring$1.invoke(ContainsSpec.kt:13)
		at ContainsSpec$a_string_should_contain_its_own_substring$1.invoke(ContainsSpec.kt:7)
		at com.benwoodworth.parameterize.ParameterizeKt.parameterize(Parameterize.kt:91)
```

Parameters are also designed to be flexible, depend on other parameters, be called conditionally, or even used a loop to
declare multiple parameters from the same property. Features which are especially useful for covering edge/corner cases:

```kotlin
@Test
fun an_int_should_not_equal_a_different_int() = parameterize {
    val int by parameterOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE)
    val differentInt by parameterOf(int + 1, int - 1)

    assertNotEquals(int, differentInt)
}
```

### Test Framework Examples

<details>
<summary><b>kotlin.test</b></summary>

Parameterize can be configured to trigger [kotlin.test](https://kotlinlang.org/api/latest/kotlin.test/)'s before/after
hooks for each of its iterations. To avoid re-configuring these hooks in each parameterize call, a pre-configured
ParameterizeContext can be used. In this example, `parameterize` calls in classes that extend `TestingContext` will pull
their default configuration from it. The options can still be overridden at the call site, but will default to the
options set in the ParameterizeContext.

In the future, this setup should be made easier with context receivers, only requiring a top level
`ParameterizeConfiguration` property and a `with val` bringing it into context.
([See here](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md#scope-properties)) 

```kotlin
abstract class TestingContext : ParameterizeContext {
    override val parameterizeConfiguration = ParameterizeConfiguration {
        // Insert before/after calls around each test case,
        // except when already invoked by kotlin.test
        decorator = { iteration ->
            if (!isFirstIteration) beforeTest()
            iteration()
            if (!isLastIteration) afterTest()
        }
        
        // ...other shared configuration
    }

    open fun beforeTest() {}
    open fun afterTest() {}

    
    // The annotations would be lost when overriding beforeTest/afterTest,
    // so hook in here instead of relying on the subclass to apply them.
    @BeforeTest
    fun beforeTestHook(): Unit = beforeTest()

    @AfterTest
    fun afterTestHook(): Unit = afterTest()
}
```
```kotlin
class KotlinTestSpec : TestingContext() {
    override fun beforeTest() {
        // ...
    }

    override fun afterTest() {
        // ...
    }
    
    @Test
    fun a_string_should_contain_its_own_substring() = parameterize {
        val substring = "substring"
        val prefix by parameterOf("prefix-", "")
        val suffix by parameterOf("-suffix", "")

        val string = "$prefix$substring$suffix"

        assertTrue(string.contains(substring), "\"$string\".contains(\"$substring\")")
    }
}
```

</details>
Outside of testing contexts, behaving the same as a normal loop and not handling failures should be the default behavior.

Having `parameterize` continue looping means pressing forward for *any* failure, even `Error`s which are not meant to be caught normally. Additionally, having the default `onComplete` behavior be to throw a `ParameterizeFailedError` is problematic for the same reason. With that and the additional overhead of recording failures and parameter arguments, the current default behavior in v0.2 isn't suitable for normal use in code.

Changing the default behavior to throw immediately from `onFailure` means the default `parameterize` acts identically to a nested for loop, with `Error`s not being caught, no additional failure recording overhead, and no errors thrown (since throwing from onFailure terminates `parameterize` without `onComplete` being executed).

For testing, this is fine since there are two main styles of usage I see. Annotation-based, like kotlin.test/JUnit, and DSL based like Kotest.
- For the annotation based frameworks, the suggestion is already to create a shared configuration to hook into the `BeforeTest/AfterTest` lifecycle. In these cases, it's easy enough to also add the explicit `onFailure` configured to record failures without breaking early. This also means the old "sane default" of recording only the first 10 failures can be offloaded to the developers, letting them choose what's best.
- And for DSL frameworks, having tests declared *within* `parameterize` means the loop-like default is also preferable here too. Failures are not meant to occurin the test DSL itself, so
<details>
<summary><b>Kotest</b></summary>

With test frameworks that declare tests in code like [Kotest](https://kotest.io/) can, it's possible to produce a suite
of *separate* tests by declaring the test within a `parameterize`d test group. For example, using Kotest's
[Fun Spec](https://kotest.io/docs/framework/testing-styles.html#fun-spec), four tests will be registered, and then
reported separately by the test runner when executed:

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

In the future, it will likely be possible for this to be written more nicely once Kotlin gets decorators, removing the
need for an extra level of nesting nesting inside the `context`.
([See here](https://youtrack.jetbrains.com/issue/KT-49904/Decorators#focus=Comments-27-8465650.0-0))

</details>

## Setup

### Gradle

```kotlin
// build.gradle.kts

plugins {
    kotlin("jvm") version "1.9.21" // or kotlin("multiplatform"), etc.
}

repositories {
    mavenCentral()
    //maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("com.benwoodworth.parameterize:parameterize:$parameterize_version") // or testImplementation(...)
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
