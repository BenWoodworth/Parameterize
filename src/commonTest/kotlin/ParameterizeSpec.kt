package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParameterizeSpec {
    @Test
    fun with_zero_parameters_it_should_iterate_once() {
        var count = 0

        parameterize {
            count++
        }

        assertEquals(1, count)
    }

    @Test
    fun parameter_with_lazy_arguments_should_create_parameter_correctly() = parameterize {
        val lazyParameter = parameter { 'a'..'z' }

        assertEquals(('a'..'z').toList(), lazyParameter.arguments.toList())
    }

    @Test
    fun parameter_with_lazy_arguments_should_not_be_evaluated_before_read() = parameterize {
        var evaluated = false

        parameter {
            evaluated = true
            emptyList<String>()
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
    fun with_one_parameter_it_should_iterate_once_per_argument() {
        val letters = ('a'..'z').toList()

        val iterations = mutableListOf<Char>()
        parameterize {
            val letter by parameter { letters }

            iterations += letter
        }

        assertEquals(letters, iterations)
    }

    @Test
    fun with_two_independent_parameters_it_should_iterate_the_same_as_nested_loops() {
        val iterations = mutableListOf<String>()
        parameterize {
            val letter by parameterOf('a', 'b', 'c', 'd')
            val number by parameterOf(0, 1, 2, 3)

            iterations += "$letter$number"
        }

        val expectedIterations = mutableListOf<String>()
        for (letter in listOf('a', 'b', 'c', 'd')) {
            for (number in listOf(0, 1, 2, 3)) {
                expectedIterations += "$letter$number"
            }
        }

        assertEquals(expectedIterations, iterations)
    }

    @Test
    fun with_two_dependent_parameters_it_should_iterate_the_same_as_nested_loops() {
        val numbers = (0..4).toList()

        val iterations = mutableListOf<String>()
        parameterize {
            val number by parameter { numbers }
            val differentNumber by parameter { numbers.filter { it != number } }

            iterations += "$number$differentNumber"
        }

        val expectedIterations = mutableListOf<String>()
        for (number in numbers) {
            for (differentNumber in numbers.filter { it != number }) {
                expectedIterations += "$number$differentNumber"
            }
        }

        assertEquals(expectedIterations, iterations)
    }

    @Test
    fun parameters_in_a_loop_should_be_independent() {
        val iterations = mutableListOf<String>()
        parameterize {
            var string = ""

            repeat(3) {
                val letter by parameterOf('a', 'b', 'c')
                string += letter
            }

            iterations += string
        }

        val expectedIterations = mutableListOf<String>()
        for (letter1 in listOf('a', 'b', 'c')) {
            for (letter2 in listOf('a', 'b', 'c')) {
                for (letter3 in listOf('a', 'b', 'c')) {
                    expectedIterations += "$letter1$letter2$letter3"
                }
            }
        }

        assertEquals(expectedIterations, iterations)
    }

    @Test
    fun parameter_with_no_arguments_should_finish_iteration_early() {
        val iterations = mutableListOf<String>()

        parameterize {
            val digit1 by parameter(1..5)
            val digit2 by parameter((1..5).filter { it > digit1 })
            val digit3 by parameter((1..5).filter { it > digit2 })

            val increasingDigits = "$digit1$digit2$digit3"
            iterations += increasingDigits
        }

        val expectedIterations = listOf("123", "124", "125", "134", "135", "145", "234", "235", "245", "345")
        assertEquals(expectedIterations, iterations)
    }

    @Test
    fun unused_parameter_with_no_arguments_should_not_finish_iteration_early() {
        var finishedIteration = false

        parameterize {
            val unused: String by parameter(emptyList())

            finishedIteration = true
        }

        assertTrue(finishedIteration)
    }

    @Test
    fun parameter_that_depends_on_a_later_one() {
        val iterations = mutableListOf<String>()

        parameterize {
            lateinit var laterValue: String

            val earlier by parameter {
                listOf("${laterValue}1", "${laterValue}2", "${laterValue}3")
            }

            val later by parameterOf("a", "b")
            laterValue = later

            iterations += earlier
        }

        val expectedIterations = listOf("a1", "a2", "a3", "b1", "b2", "b3")
        assertEquals(expectedIterations, iterations)
    }

    @Test
    fun parameters_that_are_read_out_of_order() {
        val iterations = mutableListOf<String>()

        parameterize {
            val first by parameterOf(1, 2)
            val second by parameterOf("a", "b")

            iterations += "$second$first"
        }

        val expectedIterations = listOf("a1", "a2", "b1", "b2")
        assertEquals(expectedIterations, iterations)
    }

    @Test
    fun swapping_unused_parameters() {
        val iterations = mutableListOf<String>()

        parameterize {
            val firstReadParameter by parameterOf("a", "b")
            if (firstReadParameter == "a") {
                val unread1A: String by parameterOf()
            } else {
                val unread1B: String by parameterOf()
            }

            val lastReadParameter by parameterOf("1", "2")
            if (lastReadParameter == "1") {
                val unread2A: String by parameterOf()
            } else {
                val unread2B: String by parameterOf()
            }

            iterations += "$firstReadParameter$lastReadParameter"
        }

        val expectedIterations = listOf("a1", "a2", "b1", "b2")
        assertEquals(expectedIterations, iterations)
    }

    @Test
    fun parameter_only_read_from_another_lazy_initialization() {
        val iterations = mutableListOf<String>()

        parameterize {
            val letter by parameter('a'..'c')

            val letterNumber by parameter {
                (1..3).map { "$letter$it" }
            }

            iterations += letterNumber
        }

        val expectedIterations = listOf("a1", "a2", "a3", "b1", "b2", "b3", "c1", "c2", "c3")
        assertEquals(expectedIterations, iterations)
    }

    @Test
    fun declaring_parameters_within_another_initialization() {
        val iterations = mutableListOf<String>()

        parameterize {
            val letterNumber by parameter {
                val letter by parameter('a'..'c')
                val number by parameter(1..3)

                listOf("$letter$number")
            }

            iterations += letterNumber
        }

        val expectedIterations = listOf("a1", "a2", "a3", "b1", "b2", "b3", "c1", "c2", "c3")
        assertEquals(expectedIterations, iterations)
    }

    @Test
    fun custom_lazy_arguments_implementation() {
        fun <T> ParameterizeScope.customLazyParameter(
            lazyArguments: () -> Iterable<T>
        ): Parameter<T> {
            val arguments by lazy(lazyArguments)

            class CustomLazyArguments : Iterable<T> {
                override fun iterator(): Iterator<T> = arguments.iterator()
            }

            return parameter(CustomLazyArguments())
        }

        val iterations = mutableListOf<String>()

        parameterize {
            val letterNumber by customLazyParameter {
                val letter by parameter('a'..'c')
                val number by parameter(1..3)

                listOf("$letter$number")
            }

            iterations += letterNumber
        }

        val expectedIterations = listOf("a1", "a2", "a3", "b1", "b2", "b3", "c1", "c2", "c3")
        assertEquals(expectedIterations, iterations)
    }
}
