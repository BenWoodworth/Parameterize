package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class ParameterizeConfigurationOnFailureSpec : ParameterizeContext {
    override val parameterizeConfiguration = ParameterizeConfiguration {
        onComplete = {} // Don't throw because of the tested failures
    }

    @Test
    fun should_be_invoked_once_per_failure() {
        val failureIterations = listOf(1, 3, 4, 7, 9, 10)

        var currentIteration = -1
        val iterationsInvoked = mutableListOf<Int>()

        parameterize(
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
    fun should_be_invoked_with_the_failure() {
        val failures = List(10) { Throwable(it.toString()) }

        val invokedWithFailures = mutableListOf<Throwable>()

        parameterize(
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
    fun should_not_continue_if_should_break_is_true() {
        val failureIterations = listOf(1, 3, 4, 7)
        val breakIteration = failureIterations.last()

        var lastIteration = -1

        parameterize(
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
    fun failures_within_on_failure_should_propagate_out_uncaught() {
        class FailureWithinOnFailure : Throwable()

        assertFailsWith<FailureWithinOnFailure> {
            parameterize(
                onFailure = {
                    throw FailureWithinOnFailure()
                }
            ) {
                fail()
            }
        }
    }

    @Test
    fun iteration_count_should_be_correct() {
        var expectedIterationCount = 0L

        parameterize(
            onFailure = {
                assertEquals(expectedIterationCount, iterationCount)
            }
        ) {
            val iteration by parameter(0..100)
            useParameter(iteration)

            expectedIterationCount++
            fail(iteration.toString())
        }
    }

    @Test
    fun failure_count_should_be_correct() {
        var expectedFailureCount = 0L

        parameterize(
            onFailure = {
                assertEquals(expectedFailureCount, failureCount)
            }
        ) {
            val iteration by parameter(0..100)
            useParameter(iteration)

            if (iteration % 2 == 0 || iteration % 7 == 0) {
                expectedFailureCount++
                fail("iteration $iteration")
            }
        }
    }

    @Test
    fun parameter_arguments_should_be_those_from_the_last_iteration() {
        val lastParameterArguments = mutableListOf<Pair<String, *>>()

        parameterize(
            onFailure = {
                val actualParameterArguments = arguments
                    .map { (parameter, argument) -> parameter.name to argument }

                assertEquals(lastParameterArguments, actualParameterArguments)
            }
        ) {
            lastParameterArguments.clear()

            val iteration by parameter(0..10)
            lastParameterArguments += "iteration" to iteration

            if (iteration % 2 == 0) {
                val evenIteration by parameterOf(iteration)
                lastParameterArguments += "evenIteration" to evenIteration
            }

            if (iteration % 3 == 0) {
                val threevenIteration by parameterOf(iteration)
                lastParameterArguments += "threevenIteration" to threevenIteration
            }

            fail()
        }
    }
}
