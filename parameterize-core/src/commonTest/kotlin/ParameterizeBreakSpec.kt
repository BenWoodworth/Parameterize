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

import com.benwoodworth.parameterize.test.parameterizeState
import com.benwoodworth.parameterize.test.probeThrow
import kotlin.test.*

class ParameterizeBreakSpec {
    /**
     * [ParameterizeBreak] is thrown when [parameterize] is misused, so should cause it to immediately fail since
     * its state and parameter tracking are invalid.
     */
    @Test
    fun should_cause_parameterize_to_immediately_break_without_continuing_or_triggering_handlers() {
        val failedAssertions = mutableListOf<String>()

        runCatching {
            parameterize(
                onFailure = { failedAssertions += "onFailure handler should not be invoked" },
                onComplete = { failedAssertions += "onComplete handler should not be invoked" }
            ) {
                val iteration by parameter(1..2)
                if (iteration == 2) failedAssertions += "Should not continue to iteration 2"

                throw ParameterizeBreak(parameterizeState, ParameterizeException("Stub"))
            }
        }

        assertEquals(emptyList(), failedAssertions, "Failed assertions")
    }

    @Test
    fun should_cause_parameterize_to_fail_with_the_break_exception() {
        val breakException = ParameterizeException("Stub")

        val actualException = assertFailsWith<ParameterizeException> {
            parameterize {
                throw ParameterizeBreak(parameterizeState, breakException)
            }
        }

        assertSame(breakException, actualException, "Should fail with the break's exception")
    }

    // TODO Valid? Test outer declaring param within inner?
    /**
     * When a different *inner* [parameterize] is misused, its should not cause other *outer* [parameterize] calls to
     * fail, as the *inner* [parameterize] being invalid does not make the *outer* one invalid.
     */
    @Test
    fun when_thrown_from_a_different_parameterize_call_it_should_be_ignored() {
        val probedThrows = mutableListOf<Throwable?>()

        runCatching {
            parameterize {
                val outerScope = this

                probeThrow(probedThrows) {
                    parameterize {
                        throw ParameterizeBreak(outerScope.parameterizeState, ParameterizeException("Stub"))
                    }
                }
            }
        }

        assertIs<ParameterizeBreak>(probedThrows[0], "Inner parameterize should not catch break for the outer parameterize")
    }

    @Test
    fun parameter_disappears_on_second_iteration_due_to_external_condition() {
        val probedThrows = mutableListOf<Throwable?>()

        runCatching {
            var shouldDeclareA = true

            parameterize {
                if (shouldDeclareA) {
                    val a by parameterOf(1)
                }

                probeThrow(probedThrows) {
                    val b by parameterOf(1, 2)
                }

                shouldDeclareA = false
            }
        }

        val probedBreak = assertIs<ParameterizeBreak>(probedThrows[0])
        assertEquals("Expected to be declaring `a`, but got `b`", probedBreak.cause.message)
    }

    @Test
    fun parameter_appears_on_second_iteration_due_to_external_condition() {
        var shouldDeclareA = false
        val probedThrows = mutableListOf<Throwable?>()

        runCatching {
            parameterize {
                if (shouldDeclareA) {
                    probeThrow(probedThrows) {
                        val a by parameterOf(2)
                    }
                }

                val b by parameterOf(1, 2)

                shouldDeclareA = true
            }
        }

        val probedBreak = assertIs<ParameterizeBreak>(probedThrows[0])
        assertEquals("Expected to be declaring `b`, but got `a`", probedBreak.cause.message)
    }

    @Test
    fun nested_parameter_declaration_within_arguments_iterator_function() {
        val probedThrows = mutableListOf<Throwable?>()

        fun ParameterizeScope.testArguments() = object : Sequence<Unit> {
            override fun iterator(): Iterator<Unit> {
                probeThrow(probedThrows) {
                    val inner by parameterOf(Unit)
                }

                return listOf(Unit).iterator()
            }
        }

        runCatching {
            parameterize {
                val outer by parameter(testArguments())
            }
        }

        val thrownBreak = assertIs<ParameterizeBreak>(probedThrows[0])
        assertEquals(
            "Nesting parameters is not currently supported: `inner` was declared within `outer`'s arguments",
            thrownBreak.cause.message
        )
    }

    @Test
    fun nested_parameter_declaration_within_arguments_iterator_next_function() {
        val probedThrows = mutableListOf<Throwable?>()

        fun ParameterizeScope.testArgumentsIterator() = object : Iterator<Unit> {
            override fun hasNext(): Boolean = true

            override fun next() {
                probeThrow(probedThrows) {
                    val inner by parameterOf(Unit)
                }
            }
        }

        runCatching {
            parameterize {
                val outer by parameter(Sequence(::testArgumentsIterator))
            }
        }

        val probedThrow = assertIs<ParameterizeBreak>(probedThrows[0])
        assertEquals(
            "Nesting parameters is not currently supported: `inner` was declared within `outer`'s arguments",
            probedThrow.cause.message
        )
    }

    @Test
    fun nested_parameter_declaration_with_another_valid_intermediate_parameter_usage() { // TODO Remove in usage special treatment commit
        val probedThrows = mutableListOf<Throwable?>()

        runCatching {
            parameterize {
                val trackedNestingInterference by parameterOf(Unit)

                val outer by parameter {
                    with(this@parameterize) { // TODO Fix in DslMarker commit
                        probeThrow(probedThrows) {
                            val inner by parameterOf(Unit)
                        }
                    }

                    listOf(Unit)
                }
            }
        }

        val probedBreak = assertIs<ParameterizeBreak>(probedThrows[0])
        assertEquals(
            "Nesting parameters is not currently supported: `inner` was declared within `outer`'s arguments",
            probedBreak.cause.message
        )
    }

    @Test
    fun nested_parameter_declaration_with_another_valid_intermediate_parameter_usage_with_lazy_parameter_scope() { // TODO Remove in usage special treatment commit
        val probedThrows = mutableListOf<Throwable?>()

        runCatching {
            parameterize {
                val trackedNestingInterference by parameterOf(Unit)

                val outer by parameter {
                    probeThrow(probedThrows) {
                        @Suppress("DEPRECATION_ERROR")
                        val inner by parameterOf(Unit)
                    }

                    listOf(Unit)
                }

                val end by parameterOf(Unit, Unit)
            }
        }

        val probedBreak = assertIs<ParameterizeBreak>(probedThrows[0])
        assertEquals(
            "Nesting parameters is not currently supported: `inner` was declared within `outer`'s arguments",
            probedBreak.cause.message
        )
    }

    @Test
    fun declaring_parameter_after_iteration_completed() { // TODO Not break-related
        var declareParameter = {}

        parameterize {
            declareParameter = {
                val parameter by parameterOf(Unit)
            }
        }

        val failure = assertFailsWith<ParameterizeException> { // TODO
            declareParameter()
        }

        assertEquals("Cannot declare parameter `parameter` after its iteration has completed", failure.message)
    }

    @Test
    fun failing_earlier_than_the_previous_iteration() { // TODO PException, no break
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
