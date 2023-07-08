package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertContentEquals
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
            val letter by parameter(letters)

            iterations += letter
        }

        iterations.sort()
        assertContentEquals(letters, iterations)
    }

    @Test
    fun with_two_independent_parameters_it_should_iterate_once_per_permutation() {
        val iterations = mutableListOf<String>()
        parameterize {
            val letter by parameter('a', 'b', 'c', 'd')
            val number by parameter(0, 1, 2, 3)

            iterations += "$letter$number"
        }

        iterations.sort()
        val expectedIterations = listOf(
            "a0", "a1", "a2", "a3",
            "b0", "b1", "b2", "b3",
            "c0", "c1", "c2", "c3",
            "d0", "d1", "d2", "d3"
        )

        assertContentEquals(expectedIterations, iterations)
    }

    @Test
    fun with_two_dependent_parameters_it_should_iterate_once_per_permutation() {
        val iterations = mutableListOf<String>()
        parameterize {
            val numbers = (0..3).toList()

            val number by parameter(numbers)
            val differentNumber by parameter(numbers.filter { it != number })

            iterations += "$number$differentNumber"
        }

        iterations.sort()
        val expectedIterations = listOf(
            "01", "02", "03", "10",
            "12", "13", "20", "21",
            "23", "30", "31", "32"
        )

        assertContentEquals(expectedIterations, iterations)
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

        iterations.sort()
        val expectedIterations = listOf(
            "aaa", "aab", "aac", "aba", "abb", "abc", "aca", "acb", "acc",
            "baa", "bab", "bac", "bba", "bbb", "bbc", "bca", "bcb", "bcc",
            "caa", "cab", "cac", "cba", "cbb", "cbc", "cca", "ccb", "ccc",
        )

        assertContentEquals(expectedIterations, iterations)
    }
}
