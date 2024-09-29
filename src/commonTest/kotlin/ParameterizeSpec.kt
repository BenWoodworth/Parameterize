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

import effekt.discardWithFast
import effekt.handle
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import runCC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Specifies how default non-configured [parameterize] usage should behave.
 */
class ParameterizeSpec {
    /**
     * Run parameterize with the given block, and assert that each iteration
     * completes correctly based on the value returned by the [block]. The
     * [expectedIterations] are the expected values returned each iteration, or
     * `null` for skipped iterations (when a parameter has no arguments).
     */
    private fun <T : Any> testParameterize(
        expectedIterations: Iterable<T?>,
        block: suspend ParameterizeScope.() -> T
    ) = runTestCC {
        val iterations = mutableListOf<T?>()

        parameterize(decorator = { iteration ->
            iterations += null
            iteration()
        }) {
            block().also { iterations[iterations.lastIndex] = it }
        }

        assertEquals(expectedIterations.toList(), iterations, "Incorrect iterations")
    }


    @Test
    fun with_zero_parameters_it_should_iterate_once() = testParameterize(
        listOf("only iteration")
    ) {
        "only iteration"
    }

    @Test
    fun parameter_arguments_iterator_should_be_computed_when_declared() = runTestCC {
        parameterize {
            var computed = false

            val parameter by parameter(Sequence {
                computed = true
                listOf(Unit).iterator()
            })

            assertTrue(computed, "computed")
        }
    }

    @Test
    fun second_parameter_argument_should_not_be_computed_until_the_next_iteration() = runTestCC {
        var finishedFirstIteration = false

        class AssertingIterator : Iterator<Unit> {
            var nextArgument = 1

            override fun hasNext(): Boolean =
                nextArgument <= 2

            override fun next() {
                if (nextArgument == 2) {
                    assertTrue(finishedFirstIteration, "finished first iteration before getting second argument")
                }
                nextArgument++
            }
        }

        parameterize {
            val parameter by parameter(Sequence(::AssertingIterator))

            finishedFirstIteration = true
        }
    }

    @Test
    fun parameter_should_restore_local_state_on_each_iteration() = runTestCC {
        parameterize {
            var state = "creating arguments"
            val iterationArguments = Sequence {
                object : Iterator<Int> {
                    var nextArgument = 0

                    override fun hasNext(): Boolean = nextArgument <= 5

                    override fun next(): Int {
                        return nextArgument++
                    }
                }
            }

            state = "declaring parameter"
            val iterationParameter = parameter(iterationArguments)

            val iteration by iterationParameter
            assertEquals("declaring parameter", state, "state (iteration $iteration)")

            state = "using parameter"
            useParameter(iteration)

            state = "between iterations"
        }
    }

    @Test
    fun with_one_parameter_it_should_iterate_once_per_argument() = testParameterize('a'..'z') {
        val letter by parameter('a'..'z')

        letter
    }

    @Test
    fun with_two_independent_parameters_it_should_iterate_the_same_as_nested_loops() = testParameterize(
        buildList {
            for (letter in listOf('a', 'b', 'c', 'd')) {
                for (number in listOf(0, 1, 2, 3)) {
                    add("$letter$number")
                }
            }
        }
    ) {
        val letter by parameterOf('a', 'b', 'c', 'd')
        val number by parameterOf(0, 1, 2, 3)

        "$letter$number"
    }

    @Test
    fun with_two_dependent_parameters_it_should_iterate_the_same_as_nested_loops() = testParameterize(
        buildList {
            for (number in 0..4) {
                for (differentNumber in (0..4).filter { it != number }) {
                    add("$number$differentNumber")
                }
            }
        }
    ) {
        val number by parameter(0..4)
        val differentNumber by parameter { (0..4).filter { it != number } }

        "$number$differentNumber"
    }

    @Test
    fun parameters_in_a_loop_should_be_independent() = testParameterize(
        buildList {
            for (letter1 in listOf('a', 'b', 'c')) {
                for (letter2 in listOf('a', 'b', 'c')) {
                    for (letter3 in listOf('a', 'b', 'c')) {
                        add("$letter1$letter2$letter3")
                    }
                }
            }
        }
    ) {
        var string = ""

        // repeat doesn't work on JS because JS broke its for-loop over IntRange
        // optimization. TODO find relevant YouTrack issue
        var i = 0
        while(i < 3) {
            val letter by parameterOf('a', 'b', 'c')
            string += letter
            i++
        }

        string
    }

    @Test
    fun parameter_with_no_arguments_should_finish_iteration_early() = testParameterize(
        listOf("123", "124", "125", "134", "135", "145", null, "234", "235", "245", null, "345", null, null, null)
    ) {
        // increasing digits
        val digit1 by parameter(1..5)
        val digit2 by parameter((1..5).filter { it > digit1 })
        val digit3 by parameter((1..5).filter { it > digit2 })

        "$digit1$digit2$digit3"
    }

    @Test
    fun unused_parameter_with_no_arguments_should_finish_iteration_early() = testParameterize(
        listOf(null)
    ) {
        val unused by parameterOf<Nothing>()
        unused
    }

    @Test
    fun parameters_that_are_used_out_of_order_should_iterate_in_declaration_order() = testParameterize(
        listOf("a1", "a2", "b1", "b2")
    ) {
        // Because they should iterate in the order their arguments are calculated, and that happens at declaration.

        // Parameters that (potentially) depend on another parameter should iterate first, since if it were the other
        // way around, and the depended on parameter iterates first, the dependent parameter's argument would be based
        // on a now changed value and wouldn't be valid.

        val first by parameterOf("a", "b")
        val second by parameterOf(1, 2)

        // Used in a different order
        useParameter(second)
        useParameter(first)

        "$first$second"
    }

    @Test
    fun custom_lazy_arguments_implementation() = testParameterize(
        listOf("a1", "a2", "a3", "b1", "b2", "b3", "c1", "c2", "c3")
    ) {
        suspend fun <T> ParameterizeScope.customLazyParameter(
            lazyArguments: () -> Iterable<T>
        ): ParameterizeScope.ParameterDelegate<T> {
            val arguments by lazy(lazyArguments)

            class CustomLazyArguments : Iterable<T> {
                override fun iterator(): Iterator<T> = arguments.iterator()
            }

            return parameter(CustomLazyArguments())
        }

        val letter by parameter('a'..'c')
        val number by parameter(1..3)

        val letterNumber by customLazyParameter {
            listOf("$letter$number")
        }

        letterNumber
    }

    @Test
    fun captured_parameters_should_be_usable_after_the_iteration_completes() = runTestCC {
        val capturedParameters = mutableListOf<() -> Int>()

        parameterize {
            val iteration by parameter(0..10)

            capturedParameters += { iteration }
        }

        testAll(
            (0..10).map { "iteration $it" to it }
        ) { iteration ->
            assertEquals(iteration, capturedParameters[iteration]())
        }
    }

    @Test
    fun should_be_able_to_discard_to_an_outer_function_from_within_the_block() = runTestCC {
        handle {
            parameterize {
                discardWithFast(Result.success(Unit))
            }
        }
    }

    /**
     * The motivating use case here is decorating a Kotest test group, in which the test declarations suspend.
     */
    @Test
    fun should_be_able_to_decorate_a_suspend_block() = runTest {
        // This works as well with a normal flow, but this could
        // change in future versions of kontinuity (because
        // currently, we wrap the `coroutineContext` to add
        // extra data, but that data could simply be added to the context.
        // if we do that though, `flow` complains that the context,
        // and hence the coroutine, changed)
        val coordinates = channelFlow {
            runCC {
                parameterize {
                    val letter by parameter('a'..'c')
                    val number by parameter(1..3)

                    send("$letter$number")
                }
            }
        }

        assertEquals(
            listOf("a1", "a2", "a3", "b1", "b2", "b3", "c1", "c2", "c3"),
            coordinates.toList()
        )
    }
}
