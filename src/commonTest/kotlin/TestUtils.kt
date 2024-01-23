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

private class TestAllSkip(
    message: String
) : Throwable(message)

object TestAllScope {
    fun skip(message: String): Nothing =
        throw TestAllSkip(message)
}

fun <T> testAll(
    testCases: Iterable<Pair<String, T>>,
    test: TestAllScope.(testCase: T) -> Unit
) {
    val results = testCases
        .map { (description, testCase) ->
            description to runCatching { TestAllScope.test(testCase) }
        }

    val passed = results.count { (_, result) -> result.isSuccess }
    val skipped = results.count { (_, result) -> result.exceptionOrNull() is TestAllSkip }
    val failed = results.count() - passed - skipped

    if (failed > 0) {
        val message = buildString {
            append("Test completed with failures ($failed/${passed + failed} failed")
            if (skipped > 0) append(", and $skipped skipped")
            append("):")

            results.forEach { (description, result) ->
                val label = when (result.exceptionOrNull()) {
                    is TestAllSkip -> "SKIP"
                    null -> "PASS"
                    else -> "FAIL"
                }

                append("\n[$label] $description")

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
    test: TestAllScope.(testCase: T) -> Unit
): Unit =
    testAll(testCases.toList(), test)

fun testAll(vararg testCases: Pair<String, TestAllScope.() -> Unit>): Unit =
    testAll(testCases.toList()) { testCase ->
        testCase()
    }
