package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ParameterizeExceptionSpec {
    @Test
    fun should_rethrow_and_not_continue_after_ParameterizeException() {
        var iterations = 0
        val exception = ParameterizeException("test")

        val actualException = assertFailsWith<ParameterizeException> {
            parameterize {
                iterations++

                val parameter by parameterOf(1, 2)

                throw exception
            }
        }

        assertSame(exception, actualException)
        assertEquals(1, iterations, "Should not continue after exception")
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
        fun ParameterizeScope.testArguments() = object : Iterable<Unit> {
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
                val outer by parameter(Iterable(::testArgumentsIterator))
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
}
