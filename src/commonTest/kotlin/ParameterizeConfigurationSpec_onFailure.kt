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

import com.benwoodworth.parameterize.ParameterizeConfiguration.OnFailureScope
import kotlin.test.*

@Suppress("ClassName")
class ParameterizeConfigurationSpec_onFailure {
    private suspend inline fun testParameterize(
        noinline onFailure: OnFailureScope.(failure: Throwable) -> Unit,
        noinline block: suspend ParameterizeScope.() -> Unit
    ): Unit =
        parameterize(
            onFailure = onFailure,
            onComplete = {}, // Don't throw because of the tested failures
            block = block
        )

    @Test
    fun should_be_invoked_once_per_failure() = runTestCC {
        val failureIterations = listOf(1, 3, 4, 7, 9, 10)

        var currentIteration = -1
        val iterationsInvoked = mutableListOf<Int>()

        testParameterize(
            onFailure = {
                iterationsInvoked += currentIteration
            }
        ) {
            val iteration by parameter(0..10)
            currentIteration = iteration

            if (iteration in failureIterations) {
                fail()
            }
        }

        assertEquals(failureIterations, iterationsInvoked)
    }

    @Test
    fun should_be_invoked_with_the_failure() = runTestCC {
        val failures = List(10) { Throwable(it.toString()) }

        val invokedWithFailures = mutableListOf<Throwable>()

        testParameterize(
            onFailure = { failure ->
                invokedWithFailures += failure
            }
        ) {
            val failure by parameter(failures)

            throw failure
        }

        assertEquals(failures, invokedWithFailures)
    }

    @Test
    fun should_not_continue_if_should_break_is_true() = runTestCC {
        val failureIterations = listOf(1, 3, 4, 7)
        val breakIteration = failureIterations.last()

        var lastIteration = -1

        testParameterize(
            onFailure = {
                breakEarly = (lastIteration == breakIteration)
            }
        ) {
            val iteration by parameter(0..10)
            lastIteration = iteration

            if (iteration in failureIterations) {
                fail()
            }
        }

        assertEquals(breakIteration, lastIteration)
    }

    @Test
    fun failures_within_on_failure_should_propagate_out_uncaught() = runTestCC {
        class FailureWithinOnFailure : Throwable()

        assertFailsWith<FailureWithinOnFailure> {
            testParameterize(
                onFailure = {
                    throw FailureWithinOnFailure()
                }
            ) {
                fail()
            }
        }
    }

    @Test
    fun iteration_count_should_be_correct() = runTestCC {
        var expectedIterationCount = 0L

        testParameterize(
            onFailure = {
                assertEquals(expectedIterationCount, iterationCount)
            }
        ) {
            val iteration by parameter(0..100)

            expectedIterationCount++
            fail(iteration.toString())
        }
    }

    @Test
    fun failure_count_should_be_correct() = runTestCC {
        var expectedFailureCount = 0L

        testParameterize(
            onFailure = {
                assertEquals(expectedFailureCount, failureCount)
            }
        ) {
            val iteration by parameter(0..100)

            if (iteration % 2 == 0 || iteration % 7 == 0) {
                expectedFailureCount++
                fail("iteration $iteration")
            }
        }
    }

    data class FailureParameterArgumentsException(val parameterArguments: List<Pair<String, *>>): Exception()

    @Test
    fun failure_arguments_should_be_those_from_the_last_iteration() = runTestCC {
        testParameterize(
            onFailure = {
                assertIs<FailureParameterArgumentsException>(it)
                val actualParameterArguments = arguments
                    .map { (parameter, argument) -> parameter.name to argument }

                assertEquals(it.parameterArguments, actualParameterArguments)
            }
        ) {
            val iteration by parameter(0..10)
            val iterationPair = "iteration" to iteration

            val evenIterationPair = if (iteration % 2 == 0) {
                val evenIteration by parameterOf(iteration)
                "evenIteration" to evenIteration
            } else null

            val threevenIterationPair = if (iteration % 3 == 0) {
                val threevenIteration by parameterOf(iteration)
                "threevenIteration" to threevenIteration
            } else null

            throw FailureParameterArgumentsException(listOfNotNull(iterationPair, evenIterationPair, threevenIterationPair))
        }
    }

    @Test
    fun failure_arguments_should_only_include_used_parameters() = runTestCC {
        testParameterize(
            onFailure = {
                val actualUsedParameters = arguments.map { it.parameter.name }
                assertEquals(listOf("used1", "used2"), actualUsedParameters)
            }
        ) {
            val used1 by parameterOf(Unit)
            val unused1 by parameterOf(Unit)
            val used2 by parameterOf(Unit)
            val unused2 by parameterOf(Unit)

            useParameter(used1)
            useParameter(used2)

            fail()
        }
    }

    @Test
    fun failure_arguments_should_include_lazily_used_parameters_that_were_unused_this_iteration() = runTestCC {
        testParameterize(
            onFailure = {
                val actualUsedParameters = arguments.map { it.parameter.name }
                assertContains(actualUsedParameters, "letter")
            }
        ) {
            val letter by parameterOf('a', 'b')

            var letterUsedThisIteration = false

            val letterNumber by parameter {
                letterUsedThisIteration = true
                (1..2).map { "$letter$it" }
            }

            // Letter contributes to the failure, even though it wasn't used this iteration
            if (letterNumber == "b2") {
                check(!letterUsedThisIteration) { "Letter was actually used this iteration, so test is invalid" }
                fail()
            }
        }
    }

    @Test
    fun failure_arguments_should_include_captured_parameters_from_previous_iterations() = runTestCC {
        var isFirstIteration = true
        testParameterize(
            onFailure = {
                val parameters = arguments.map { it.parameter.name }

                assertTrue(
                    "neverUsedDuringTheCurrentIteration" !in parameters == isFirstIteration,
                    "neverUsedDuringTheCurrentIteration !in $parameters != $isFirstIteration"
                )
                isFirstIteration = false
            }
        ) {
            val neverUsedDuringTheCurrentIteration by parameterOf(Unit)

            @Suppress("UNUSED_EXPRESSION")
            val usePreviousIterationParameter by parameterOf(
                { }, // Don't use it the first iteration
                { neverUsedDuringTheCurrentIteration }
            )

            // On the 2nd iteration, use the parameter captured from the 1st iteration
            usePreviousIterationParameter()

            fail()
        }
    }

    @Test
    fun failure_arguments_should_not_include_parameters_only_used_in_previous_iterations() = runTestCC {
        var isFirstIteration = true
        testParameterize(
            onFailure = {
                val parameters = arguments.map { it.parameter.name }

                assertTrue(
                    "neverUsedDuringTheCurrentIteration" in parameters == isFirstIteration,
                    "neverUsedDuringTheCurrentIteration in $parameters != $isFirstIteration"
                )
                isFirstIteration = false
            }
        ) {
            val neverUsedDuringTheCurrentIteration by parameterOf(Unit)

            @Suppress("UNUSED_EXPRESSION")
            val useParameter by parameterOf(
                { neverUsedDuringTheCurrentIteration },
                { }, // Don't use it the second iteration
            )

            useParameter()

            fail()
        }
    }
}
