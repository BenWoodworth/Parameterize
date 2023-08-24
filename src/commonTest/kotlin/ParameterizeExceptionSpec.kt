package com.benwoodworth.parameterize

import kotlin.properties.PropertyDelegateProvider
import kotlin.test.*

class ParameterizeExceptionSpec {
    @Test
    fun should_rethrow_and_not_continue_after_ParameterizeException() {
        var iterations = 0
        val exception = ParameterizeException("test")

        val actualException = assertFailsWith<ParameterizeException> {
            parameterize(
                throwHandler = {} // continue on (normal) throw
            ) {
                iterations++

                val parameter by parameterOf(1, 2)
                readProperty(parameter)

                throw exception
            }
        }

        assertSame(exception, actualException)
        assertEquals(1, iterations, "Should not continue after exception")
    }

    @Test
    fun parameter_delegate_used_with_the_wrong_property() {
        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                lateinit var interceptedDelegateFromA: ParameterDelegate<Int>

                val a by PropertyDelegateProvider { thisRef: Any?, property ->
                    parameterOf(1)
                        .provideDelegate(thisRef, property)
                        .also { interceptedDelegateFromA = it }
                }

                val b by interceptedDelegateFromA
                readProperty(b)
            }
        }

        assertEquals("Cannot use parameter delegate with `b`. Already declared for `a`.", exception.message)
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
                readProperty(b)

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
                readProperty(b)

                shouldDeclareA = true
            }
        }

        assertEquals("Expected to be declaring `b`, but got `a`", exception.message)
    }

    @Test
    fun parameter_creation_within_another_initialization() {
        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                val outer: String by parameter {
                    parameter(emptyList<Nothing>())
                    fail("Should have thrown")
                }

                readProperty(outer)
            }
        }

        assertEquals("Declaring a parameter within another is not supported", exception.message)
    }

    @Test
    fun parameter_delegate_creation_within_another_initialization() {
        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                val parameter = parameter(listOf(1))

                val outer: String by parameter {
                    val innerParameter by parameter
                    fail("Should have thrown")
                }

                readProperty(outer)
            }
        }

        assertEquals("Declaring a parameter within another is not supported", exception.message)
    }
}
