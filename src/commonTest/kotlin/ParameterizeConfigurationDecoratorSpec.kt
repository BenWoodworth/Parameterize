package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeConfiguration.DecoratorScope
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.test.*

class ParameterizeConfigurationDecoratorSpec : ParameterizeContext {
    override val parameterizeConfiguration = ParameterizeConfiguration {
        onFailure = {
            recordFailure = true
            breakEarly = true
        }
    }

    @Test
    fun should_be_invoked_once_per_iteration() {
        var iterationCount = 0
        var timesInvoked = 0

        parameterize(
            decorator = { iteration ->
                timesInvoked++
                iteration()
            }
        ) {
            val iteration by parameter(0..10)

            iterationCount++
            assertEquals(iterationCount, timesInvoked)
        }
    }

    @Test
    fun failures_within_decorator_should_propagate_out_uncaught() {
        class FailureWithinDecorator : Throwable()

        testAll<suspend DecoratorScope.(suspend DecoratorScope.() -> Unit) -> Unit>(
            "Before `iteration()`" to {
                throw FailureWithinDecorator()
            },
            "After `iteration()`" to { iteration ->
                iteration()
                throw FailureWithinDecorator()
            },
        ) { decorator ->
            assertFailsWith<FailureWithinDecorator> {
                parameterize(decorator = decorator) {}
            }
        }
    }

    @Test
    fun block_and_failure_handler_should_be_invoked_in_iteration_function() {
        val order = mutableListOf<String>()

        parameterize(
            decorator = { iteration ->
                order += "before iteration()"
                iteration()
                order += "after iteration()"
            },
            onFailure = {
                order += "onFailure invoked"
            },
            onComplete = {} // Don't fail
        ) {
            order += "block invoked"
            fail()
        }

        val expectedOrder = listOf(
            "before iteration()",
            "block invoked",
            "onFailure invoked",
            "after iteration()",
        )
        assertEquals(expectedOrder, order)
    }

    /**
     * The decorator scope [RestrictsSuspension], so it shouldn't be possible for code outside the library to suspend
     * without hacking around the type system like this. But a nice error should be provided just in case.
     */
    @Test
    fun suspending_unexpectedly_should_fail() {
        val suspendWithoutResuming: suspend Any.() -> Unit = {
            suspendCoroutineUninterceptedOrReturn { COROUTINE_SUSPENDED }
        }

        val suspendDecoratorWithoutResuming: suspend DecoratorScope.() -> Unit = suspendWithoutResuming

        testAll<suspend DecoratorScope.(suspend DecoratorScope.() -> Unit) -> Unit>(
            "before iteration" to { iteration ->
                suspendDecoratorWithoutResuming()
                iteration()
            },
            "after iteration" to { iteration ->
                iteration()
                suspendDecoratorWithoutResuming()
            },
        ) { decorator ->
            val failure = assertFailsWith<ParameterizeException> {
                parameterize(
                    decorator = decorator
                ) {}
            }

            assertEquals("Decorator suspended unexpectedly", failure.message, "message")
        }
    }

    @Test
    fun should_evaluate_in_correct_order() {
        val order = mutableListOf<String>()

        parameterize(
            decorator = { iteration ->
                order += "before"
                iteration()
                order += "after"
            }
        ) {
            order += "iteration"
        }

        assertEquals(listOf("before", "iteration", "after"), order)
    }

    @Test
    fun on_failure_should_be_invoked_in_iteration_function() {
        var onFailureInvoked = false

        parameterize(
            onFailure = {
                onFailureInvoked = true
            },
            onComplete = {}, // Don't throw because of the failure
            decorator = { iteration ->
                assertFalse(onFailureInvoked, "onFailureInvoked, before invoking iteration")
                iteration()
                assertTrue(onFailureInvoked, "onFailureInvoked, after invoking iteration")
            }
        ) {
            throw Throwable("Test failure")
        }
    }

    @Test
    fun should_throw_if_iteration_function_is_not_invoked() {
        val exception = assertFailsWith<ParameterizeException> {
            parameterize(
                decorator = {
                    // not invoked
                }
            ) {}
        }

        assertEquals(
            "Decorator must invoke the iteration function exactly once, but was not invoked",
            exception.message,
            "message"
        )
    }

    @Test
    fun should_throw_if_iteration_function_is_invoked_more_than_once() {
        val exception = assertFailsWith<ParameterizeException> {
            parameterize(
                decorator = { iteration ->
                    iteration()
                    iteration()
                }
            ) {}
        }

        assertEquals(
            "Decorator must invoke the iteration function exactly once, but was invoked twice",
            exception.message,
            "message"
        )
    }

    @Test
    fun is_first_iteration_should_be_correct() = testAll(
        (1..3)
            .flatMap { listOf(it to "before", it to "after") }
            .map { "in iteration ${it.first}, ${it.second}" to it }
    ){ (inIteration, beforeOrAfter) ->
        val before = beforeOrAfter == "before"
        val after = beforeOrAfter == "after"

        var iterationNumber = 1
        parameterize(
            decorator = { iteration ->
                val shouldCheck = inIteration == iterationNumber

                if (before && shouldCheck) assertEquals(iterationNumber == 1, isFirstIteration)
                iteration()
                if (after && shouldCheck) assertEquals(iterationNumber == 1, isFirstIteration)

                iterationNumber++
            }
        ) {
            val iteration by parameter(1..3)
            useParameter(iteration)
        }
    }

    @Test
    fun is_last_iteration_should_be_correct() = testAll(
        (1..3).map { "in iteration $it" to it }
    ){ inIteration ->
        var currentIteration = 1

        parameterize(
            decorator = { iteration ->
                iteration()

                val shouldCheck = inIteration == currentIteration
                if (shouldCheck) assertEquals(currentIteration == 3, isLastIteration)

                currentIteration++
            }
        ) {
            val iteration by parameter(1..3)
            useParameter(iteration)
        }
    }

    @Test
    fun is_last_iteration_when_accessed_before_invoking_iteration_should_throw() = testAll(
        (1..3).map { "in iteration $it" to it }
    ) { inIteration ->
        var iterationNumber = 1

        val exception = assertFailsWith<ParameterizeException> {
            parameterize(
                decorator = { iteration ->
                    if (inIteration == iterationNumber) {
                        isLastIteration
                    }

                    iteration()
                    iterationNumber++
                }
            ) {
                val iteration by parameter(1..3)
                useParameter(iteration)
            }
        }

        assertEquals(
            "Last iteration cannot be known until after the iteration function is invoked",
            exception.message,
            "message"
        )
    }
}
