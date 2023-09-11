package com.benwoodworth.parameterize

import kotlin.reflect.KProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ParameterizeThrowHandlerSpec {
    private object TestCause : Throwable("Test cause")

    @Test
    fun should_be_called_once_on_throw() {
        var callCount = 0

        parameterize(
            throwHandler = {
                callCount++
            }
        ) {
            throw TestCause
        }

        assertEquals(1, callCount)
    }

    @Test
    fun returning_should_continue_to_next_iteration() {
        var lastIteration = 0
        val throwIterations = mutableListOf<Int>()

        parameterize(
            throwHandler = {
                throwIterations += lastIteration
                // return, without re-throwing
            }
        ) {
            val iteration by parameter(0..5)
            lastIteration = iteration

            throw TestCause
        }

        assertEquals((0..5).toList(), throwIterations)
    }

    @Test
    fun throwing_should_end_parameterize_execution_and_propagate_out_to_the_caller() {
        var lastIteration = 0

        class CustomRethrow : Throwable()

        assertFailsWith<CustomRethrow> {
            parameterize(
                throwHandler = {
                    throw CustomRethrow()
                }
            ) {
                val iteration by parameter(0..5)
                lastIteration = iteration

                if (iteration == 3) {
                    throw TestCause
                }
            }
        }

        assertEquals(3, lastIteration)
    }

    @Test
    fun should_be_called_with_the_same_cause() {
        lateinit var actualCause: Throwable

        parameterize(
            throwHandler = { cause ->
                actualCause = cause
            }
        ) {
            throw TestCause
        }

        assertSame(TestCause, actualCause)
    }

    @Test
    fun should_be_called_with_used_arguments_in_declaration_order() {
        lateinit var actualArguments: List<Pair<KProperty<*>, *>>

        parameterize(
            throwHandler = {
                actualArguments = arguments
            }
        ) {
            val a by parameterOf(1)
            val unused1: Int by parameterOf()
            val b by parameterOf(2)
            val unused2: Int by parameterOf()
            val c by parameterOf(3)

            // used in a different order
            readProperty(c)
            readProperty(b)
            readProperty(a)

            throw TestCause
        }

        val actualArgumentsNamed = actualArguments
            .map { (parameter, argument) -> parameter.name to argument }

        assertEquals(listOf("a" to 1, "b" to 2, "c" to 3), actualArgumentsNamed)
    }

    @Test
    fun constructed_parameterize_failed_error_should_have_the_same_cause() {
        lateinit var error: ParameterizeFailedError

        parameterize(
            throwHandler = {
                error = ParameterizeFailedError()
            }
        ) {
            throw TestCause
        }

        assertSame(TestCause, error.cause)
    }

    @Test
    fun constructed_parameterize_failed_error_should_have_the_same_arguments() {
        lateinit var expectedArguments: List<Pair<KProperty<*>, *>>
        lateinit var error: ParameterizeFailedError

        parameterize(
            throwHandler = {
                expectedArguments = arguments
                error = ParameterizeFailedError()
            }
        ) {
            val a by parameterOf(1)
            val b by parameterOf(2)
            val c by parameterOf(3)

            readProperty(a)
            readProperty(b)
            readProperty(c)

            throw TestCause
        }

        assertSame(expectedArguments, error.arguments)
    }

    @Test
    fun default_throw_handler_should_throw_a_parameterize_failed_error() {
        assertFailsWith<ParameterizeFailedError> {
            parameterize {
                throw TestCause
            }
        }
    }
}
