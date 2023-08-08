package com.benwoodworth.parameterize

import kotlin.test.*

class ParameterizeExceptionSpec {
    private var failureProbeWasUsed: Boolean = false
    private var failedWithinFailureProbe: Boolean = false

    private fun <T> probeFailure(block: () -> T): T =
        try {
            failureProbeWasUsed = true
            block()
        } catch (e: ParameterizeException) {
            failedWithinFailureProbe = true
            throw e
        }

    @BeforeTest
    fun beforeTest() {
        failureProbeWasUsed = false
        failedWithinFailureProbe = false
    }

    @AfterTest
    fun afterTest() {
        if (failureProbeWasUsed) {
            assertTrue(failedWithinFailureProbe, "Should have failed within probe")
        }
    }


    @Test
    fun should_rethrow_and_not_continue_after_ParameterizeException() {
        var iterations = 0
        val exception = ParameterizeException("test")

        val actualException = assertFailsWith<ParameterizeException> {
            parameterize(
                throwHandler = {} // continue on (normal) throw
            ) {
                iterations++

                val parameter by parameter { 1..10 }
                readProperty(parameter)

                throw exception
            }
        }

        assertSame(exception, actualException)
        assertEquals(1, iterations, "Should not continue after exception")
    }

    @Test
    fun reusing_a_parameter_for_multiple_properties() {
        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                val parameter = parameterOf(1)

                val a by parameter
                readProperty(a)

                val b by parameter
                probeFailure {
                    readProperty(b)
                }
            }
        }

        assertEquals("Cannot use property with `b`. Already initialized with `a`.", exception.message)
    }

    @Test
    fun parameter_disappears_on_second_iteration_due_to_external_condition() {
        val exception = assertFailsWith<ParameterizeException> {
            var shouldDeclareA = true

            parameterize {
                if (shouldDeclareA) {
                    val a by parameterOf(1, 2)
                    readProperty(a)
                }

                val b by parameterOf('a', 'b')
                probeFailure {
                    readProperty(b)
                }

                shouldDeclareA = false
            }
        }

        assertEquals("Expected to be initializing `a`, but got `b`", exception.message)
    }

    @Test
    fun parameter_appears_on_second_iteration_due_to_external_condition() {
        val exception = assertFailsWith<ParameterizeException> {
            var shouldDeclareA = false

            parameterize {
                if (shouldDeclareA) {
                    val a by parameterOf(1, 2)
                    probeFailure {
                        readProperty(a)
                    }
                }

                val b by parameterOf('a', 'b')
                readProperty(b)

                shouldDeclareA = true
            }
        }

        assertEquals("Expected to be initializing `b`, but got `a`", exception.message)
    }

    @Test
    fun parameter_declared_within_another_parameter_initialization() {
        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                val outer by parameter {
                    val inner by probeFailure { parameterOf(1, 2) }

                    listOf(inner)
                }
                readProperty(outer)
            }
        }

        assertEquals("Declaring a parameter within another is not supported", exception.message)
    }

    @Test
    fun parameter_declared_within_another_parameter_initialization_dependent() {
        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                val dependency by parameterOf(1, 2)

                val outer by parameter {
                    readProperty(dependency)

                    val inner by probeFailure { parameterOf(1, 2) }

                    listOf(inner)
                }
                readProperty(outer)
            }
        }

        assertEquals("Declaring a parameter within another is not supported", exception.message)
    }

    @Test
    fun parameter_from_a_different_parameterize_scope() {
        lateinit var parameter: Parameter<Char>

        parameterize {
            parameter = parameterOf('a')
        }

        val exception = assertFailsWith<ParameterizeException> {
            parameterize {
                val a by parameter

                probeFailure {
                    readProperty(a)
                }
            }
        }

        assertEquals("Cannot initialize `a` with parameter from another scope", exception.message)
    }
}
