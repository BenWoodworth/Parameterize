@file:Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")

package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ParameterizeSpec {
    /**
     * Run parameterize with the given block, and assert that each iteration
     * completes correctly based on the value returned by the [block]. The
     * [expectedIterations] are the expected values returned each iteration, or
     * `null` for skipped iterations (when a parameter has no arguments).
     */
    private fun <T : Any> testParameterize(
        expectedIterations: Iterable<T?>,
        block: ParameterizeScope.() -> T
    ) {
        val iterations = mutableListOf<T?>()

        parameterize {
            try {
                iterations += block()
            } catch (caught: Throwable) {
                iterations += null
                throw caught
            }
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
    fun parameter_with_lazy_arguments_should_create_parameter_correctly() = parameterize {
        val lazyParameter = parameter { 'a'..'z' }

        assertEquals(('a'..'z').toList(), lazyParameter.arguments.toList())
    }

    @Test
    fun parameter_with_lazy_arguments_should_not_be_evaluated_before_read() = parameterize {
        var evaluated = false

        parameter<Nothing> {
            evaluated = true
            emptyList()
        }

        assertFalse(evaluated)
    }

    @Test
    fun parameter_with_lazy_arguments_should_only_be_evaluated_once() = parameterize {
        var evaluationCount = 0

        val lazyParameter = parameter {
            evaluationCount++
            1..10
        }

        repeat(5) { i ->
            val arguments = lazyParameter.arguments.toList()
            assertEquals((1..10).toList(), arguments, "Iteration #$i")
        }

        assertEquals(1, evaluationCount)
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

        repeat(3) {
            val letter by parameterOf('a', 'b', 'c')
            string += letter
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
    fun unused_parameter_with_no_arguments_should_not_finish_iteration_early() = testParameterize(
        listOf("finished")
    ) {
        val unused by parameterOf<Nothing>()

        "finished"
    }

    @Test
    fun parameter_that_depends_on_a_later_one() = testParameterize(
        listOf("a1", "a2", "a3", "b1", "b2", "b3")
    ) {
        lateinit var laterValue: String

        val earlier by parameter {
            listOf("${laterValue}1", "${laterValue}2", "${laterValue}3")
        }

        val later by parameterOf("a", "b")
        laterValue = later

        earlier
    }

    @Test
    fun parameters_that_are_read_out_of_order() = testParameterize(
        listOf("a1", "a2", "b1", "b2")
    ) {
        val first by parameterOf(1, 2)
        val second by parameterOf("a", "b")

        "$second$first"
    }

    @Test
    fun swapping_unused_parameters() = testParameterize(
        listOf("a1", "a2", "b1", "b2")
    ) {
        val firstReadParameter by parameterOf("a", "b")
        if (firstReadParameter == "a") {
            val unread1A by parameterOf<Nothing>()
        } else {
            val unread1B by parameterOf<Nothing>()
        }

        val lastReadParameter by parameterOf("1", "2")
        if (lastReadParameter == "1") {
            val unread2A by parameterOf<Nothing>()
        } else {
            val unread2B by parameterOf<Nothing>()
        }

        "$firstReadParameter$lastReadParameter"
    }

    @Test
    fun parameter_only_read_from_another_lazy_initialization() = testParameterize(
        listOf("a1", "a2", "a3", "b1", "b2", "b3", "c1", "c2", "c3")
    ) {
        val letter by parameter('a'..'c')

        val letterNumber by parameter {
            (1..3).map { "$letter$it" }
        }

        letterNumber
    }

    @Test
    fun declaring_parameters_within_another_initialization() = testParameterize(
        listOf("a1", "a2", "a3", "b1", "b2", "b3", "c1", "c2", "c3")
    ) {
        val letterNumber by parameter {
            val letter by parameter('a'..'c')
            val number by parameter(1..3)

            listOf("$letter$number")
        }

        letterNumber
    }

    @Test
    fun custom_lazy_arguments_implementation() = testParameterize(
        listOf("a1", "a2", "a3", "b1", "b2", "b3", "c1", "c2", "c3")
    ) {
        fun <T> ParameterizeScope.customLazyParameter(
            lazyArguments: () -> Iterable<T>
        ): Parameter<T> {
            val arguments by lazy(lazyArguments)

            class CustomLazyArguments : Iterable<T> {
                override fun iterator(): Iterator<T> = arguments.iterator()
            }

            return parameter(CustomLazyArguments())
        }

        val letterNumber by customLazyParameter {
            val letter by parameter('a'..'c')
            val number by parameter(1..3)

            listOf("$letter$number")
        }

        letterNumber
    }
}
