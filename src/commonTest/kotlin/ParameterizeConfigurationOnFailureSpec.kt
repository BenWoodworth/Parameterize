package com.benwoodworth.parameterize

import kotlin.test.*

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

            if (iteration % 2 == 0 || iteration % 7 == 0) {
                expectedFailureCount++
                fail("iteration $iteration")
            }
        }
    }

    @Test
    fun failure_arguments_should_be_those_from_the_last_iteration() {
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

    @Test
    fun failure_arguments_should_only_include_used_parameters() = parameterize(
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

    @Test
    fun failure_arguments_should_include_lazily_used_parameters_that_were_only_used_in_previous_iterations() = parameterize(
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

    @Test
    fun failure_arguments_should_not_include_captured_parameters_from_previous_iterations() = parameterize(
        onFailure = {
            val parameters = arguments.map { it.parameter.name }

            assertFalse(
                "neverUsedDuringTheCurrentIteration" in parameters,
                "neverUsedDuringTheCurrentIteration in $parameters"
            )
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
