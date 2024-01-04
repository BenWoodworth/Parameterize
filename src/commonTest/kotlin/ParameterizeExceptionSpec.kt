package com.benwoodworth.parameterize

import kotlin.test.*

class ParameterizeExceptionSpec {
    /**
     * [ParameterizeException] is thrown when [parameterize] is misused, so should cause it to immediately fail since
     * its state and parameter tracking are invalid.
     */
    @Test
    fun should_cause_parameterize_to_immediately_fail_without_or_triggering_handlers() {
        lateinit var exception: ParameterizeException

        val actualException = assertFailsWith<ParameterizeException> {
            parameterize(
                onFailure = { fail("onFailure handler should not be invoked") },
                onComplete = { fail("onComplete handler should not be invoked") }
            ) {
                exception = ParameterizeException(parameterizeState, "test")
                throw exception
            }
        }

        assertSame(exception, actualException)
    }

    /**
     * When a different *inner* [parameterize] is misused, its should not cause other *outer* [parameterize] calls to
     * fail, as the *inner* [parameterize] being invalid does not make the *outer* one invalid.
     */
    @Test
    fun when_thrown_from_a_different_parameterize_call_it_should_be_handled_like_any_other_failure() {
        lateinit var exceptionFromDifferentParameterize: ParameterizeException

        var onFailureInvoked = false
        var onCompleteInvoked = false

        val result = runCatching {
            parameterize(
                onFailure = { failure ->
                    onFailureInvoked = true
                    assertSame(exceptionFromDifferentParameterize, failure, "onFailure handler should be invoked with the exception")
                },
                onComplete = {
                    onCompleteInvoked = true
                }
            ) {
                parameterize {
                    exceptionFromDifferentParameterize = ParameterizeException(parameterizeState, "from different parameterize")
                    throw exceptionFromDifferentParameterize
                }
            }
        }

        assertEquals(Result.success(Unit), result, "should have been handled and not thrown")
        assertTrue(onFailureInvoked, "onFailure handler should be invoked")
        assertTrue(onCompleteInvoked, "onComplete handler should be invoked")
    }

    @Test
    fun parameter_disappears_on_second_iteration_due_to_external_condition() {
        val exception = assertFailsWith<ParameterizeException> {
            var shouldDeclareA = true

            parameterize {
                if (shouldDeclareA) {
                    val a by parameterOf(1)
                }

                val b by parameterOf(1, 2)

                shouldDeclareA = false
            }
        }

        assertEquals("Expected to be declaring `a`, but got `b`", exception.message)
    }

    @Test
    fun parameter_appears_on_second_iteration_due_to_external_condition() {
        val exception = assertFailsWith<ParameterizeException> {
            var shouldDeclareA = false

            parameterize {
                if (shouldDeclareA) {
                    val a by parameterOf(2)
                }

                val b by parameterOf(1, 2)

                shouldDeclareA = true
            }
        }

        assertEquals("Expected to be declaring `b`, but got `a`", exception.message)
    }

    @Test
    fun nested_parameter_declaration_within_arguments_iterator_function() {
        fun ParameterizeScope.testArguments() = object : Sequence<Unit> {
            override fun iterator(): Iterator<Unit> {
                val inner by parameterOf(Unit)

                return listOf(Unit).iterator()
            }
        }

        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                val outer by parameter(testArguments())

                val end by parameterOf(Unit, Unit)
            }
        }

        assertEquals(
            "Nesting parameters is not currently supported: `inner` was declared within `outer`'s arguments",
            exception.message
        )
    }

    @Test
    fun nested_parameter_declaration_within_arguments_iterator_next_function() {
        fun ParameterizeScope.testArgumentsIterator() = object : Iterator<Unit> {
            private var index = 0

            override fun hasNext(): Boolean = index <= 1

            override fun next() {
                if (index == 0) {
                    val innerA by parameterOf(Unit)
                } else {
                    val innerB by parameterOf(Unit)
                }

                index++
            }
        }

        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                val outer by parameter(Sequence(::testArgumentsIterator))
                val end by parameterOf(Unit, Unit)
            }
        }

        assertEquals(
            "Nesting parameters is not currently supported: `innerA` was declared within `outer`'s arguments",
            exception.message
        )
    }

    @Test
    fun nested_parameter_declaration_with_another_valid_intermediate_parameter_usage() {
        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                val trackedNestingInterference by parameterOf(Unit)

                val outer by parameter {
                    val inner by parameterOf(Unit)
                    listOf(Unit)
                }

                val end by parameterOf(Unit, Unit)
            }
        }

        assertEquals(
            "Nesting parameters is not currently supported: `inner` was declared within `outer`'s arguments",
            exception.message
        )
    }

    @Test
    fun declaring_parameter_after_iteration_completed() {
        var declareParameter = {}

        parameterize {
            declareParameter = {
                val parameter by parameterOf(Unit)
            }
        }

        val failure = assertFailsWith<ParameterizeException> {
            declareParameter()
        }

        assertEquals("Cannot declare parameter `parameter` after its iteration has completed", failure.message)
    }

    @Test
    fun failing_earlier_than_the_previous_iteration() {
        val nondeterministicFailure = Throwable("Unexpected failure")

        val failure = assertFailsWith<ParameterizeException> {
            var shouldFail = false

            parameterize {
                if (shouldFail) throw nondeterministicFailure

                val iteration by parameter(1..2)

                shouldFail = true
            }
        }

        assertEquals(
            "Previous iteration executed to this point successfully, but now failed with the same arguments",
            failure.message,
            "message"
        )
        assertSame(nondeterministicFailure, failure.cause, "cause")
    }
}
