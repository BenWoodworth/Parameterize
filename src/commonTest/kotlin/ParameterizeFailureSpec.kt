package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterizeFailureSpec {
    private val arguments = run {
        val parameter = object : Any() {
            val parameter = "argument"
        }

        listOf(ParameterizeArgument(parameter::parameter, parameter.parameter))
    }

    @Test
    fun string_representation_should_list_properties_with_the_failure_matching_the_stdlib_result_representation() =
        testAll(
            "with message" to Throwable("failure"),
            "without message" to Throwable()
        ) { failure ->
            val parameterizeFailure = ParameterizeFailure(failure, arguments)

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
}
