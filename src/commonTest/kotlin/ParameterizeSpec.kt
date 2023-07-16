package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals

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
            val letter by parameter('a', 'b', 'c', 'd')
            val number by parameter(0, 1, 2, 3)

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
                val letter by parameter('a', 'b', 'c')
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
}
