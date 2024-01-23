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

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterizeFailureSpec {
    private val argument = run {
        val container = object : Any() {
            val parameter = Any()
        }

        ParameterizeFailure.Argument(container::parameter, container.parameter)
    }

    @Test
    fun string_representation_should_list_properties_with_the_failure_matching_the_stdlib_result_representation() =
        testAll(
            "with message" to Throwable("failure"),
            "without message" to Throwable()
        ) { failure ->
            val parameterizeFailure = ParameterizeFailure(failure, listOf(argument))

            val failureString = run {
                val stdlibRepresentation = Result.failure<Unit>(parameterizeFailure.failure).toString()

                val prefix = "Failure("
                val suffix = ")"
                check(stdlibRepresentation.startsWith(prefix) && stdlibRepresentation.endsWith(suffix)) {
                    "Assumed Result string representation is $prefix...$suffix, but was $stdlibRepresentation"
                }

                stdlibRepresentation.removeSurrounding(prefix, suffix)
            }

            val expected = "${ParameterizeFailure::class.simpleName}(" +
                    "${ParameterizeFailure::failure.name}=$failureString, " +
                    "${ParameterizeFailure::arguments.name}=${parameterizeFailure.arguments}" +
                    ")"

            assertEquals(expected, parameterizeFailure.toString())
        }

    @Test
    fun argument_string_representation_should_be_parameter_name_equalling_the_argument() {
        val expected = with(argument) { "${parameter.name} = $argument" }
        assertEquals(expected, argument.toString())
    }

    @Test
    fun argument_component1_should_be_parameter() {
        val (component1, _) = argument
        assertEquals(argument.parameter, component1)
    }

    @Test
    fun argument_component2_should_be_argument() {
        val (_, component2) = argument
        assertEquals(argument.argument, component2)
    }
}
