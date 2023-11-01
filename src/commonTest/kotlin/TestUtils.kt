package com.benwoodworth.parameterize

import kotlin.test.Ignore

/**
 * [Ignore] on native targets.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class NativeIgnore()

expect val Throwable.stackTraceLines: List<String>

@Suppress("UNUSED_PARAMETER")
fun <T> useParameter(parameter: T) {
}

fun <T> testAll(
    testCases: Iterable<Pair<String, T>>,
    test: (testCase: T) -> Unit
) {
    val results = testCases
        .map { (description, testCase) ->
            description to runCatching { test(testCase) }
        }

    val passed = results.count { (_, result) -> result.isSuccess }
    val total = results.count()

    if (passed < total) {
        val message = buildString {
            append("Test cases failed ($passed/$total passed):")

            results.forEach { (description, result) ->
                append("\n[")
                append(if (result.isSuccess) "PASS" else "FAIL")
                append("] ")
                append(description)

                val failure = result.exceptionOrNull()
                if (failure != null) {
                    append(": ")
                    append(failure.message?.lines()?.first())
                }
            }
        }

        throw AssertionError(message).apply {
            results.forEach { (_, result) ->
                result.exceptionOrNull()?.let { addSuppressed(it) }
            }
        }
    }
}

fun <T> testAll(
    vararg testCases: Pair<String, T>,
    test: (testCase: T) -> Unit
): Unit =
    testAll(testCases.toList(), test)

fun testAll(vararg testCases: Pair<String, () -> Unit>): Unit =
    testAll(testCases.toList()) { testCase ->
        testCase()
    }
