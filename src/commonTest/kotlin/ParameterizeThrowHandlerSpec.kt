package com.benwoodworth.parameterize

import kotlin.test.*

class ParameterizeThrowHandlerSpec {
    @Test
    fun throw_handler_should_be_called_with_the_thrown_instance_on_throw() {
        val expectedThrownValue = object : Throwable("My Throwable!") {}

        var valueThrown: Throwable? = null
        parameterize(
            throwHandler = { valueThrown = expectedThrownValue }
        ) {
            throw expectedThrownValue
        }

        assertNotNull(valueThrown) { "Expected throwHandler to be executed" }
        assertSame(expectedThrownValue, valueThrown)
    }

    @Test
    fun throw_handler_that_doesnt_rethrow_should_continue_to_next_iteration() {
        var lastIteration: String? = null

        parameterize(throwHandler = {}) {
            val step by parameterOf("throw", "continued")
            lastIteration = step

            if (step == "throw") {
                throw object : Throwable("My Throwable!") {}
            }
        }

        assertEquals("continued", lastIteration)
    }

    @Test
    fun throw_handler_that_rethrows_another_throwable_should_cause_that_fail_with_that_throwable() {
        val thrown = object : Throwable("Thrown") {}
        val expectedReThrown = object : Throwable("Re-thrown") {}

        val actualReThrown = assertFails {
            parameterize(throwHandler = { throw expectedReThrown }) {
                throw thrown
            }
        }

        assertSame(expectedReThrown, actualReThrown)
    }

    @Test
    fun throw_handler_properties_should_be_correct() {
        class TestThrowable(val a: Char, val b: Char, val c: Char) : Throwable()

        parameterize(
            throwHandler = { thrown ->
                val expectedParameters = (thrown as TestThrowable).run {
                    listOf("a" to a, "b" to b, "c" to c)
                }

                val actualParameters = parameters
                    .map { it.parameter.name to it.argument }

                assertEquals(expectedParameters, actualParameters)
            }
        ) {
            val a by parameterOf('a', 'b', 'c')
            val b by parameterOf('1', '2', '3')
            val c by parameterOf('!', '@', '#')

            throw TestThrowable(a, b, c)
        }
    }

    @Test
    fun default_throw_handler_should_rethrow_the_same_instance() {
        parameterize(
            throwHandler = { thrown ->
                val rethrown = assertFails {
                    ParameterizeConfiguration.default.throwHandler(this, thrown)
                }

                assertSame(thrown, rethrown)
            }
        ) {
            throw object : Throwable("Test throwable") {}
        }
    }
}
