package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterizeFailedErrorSpec {
    private val arguments = run {
        val parameters = object : Any() {
            val parameterA = "argumentA"
            val parameterB = "argumentB"
        }

        listOf(
            ParameterizeArgument(parameters::parameterA, parameters.parameterA),
            ParameterizeArgument(parameters::parameterB, parameters.parameterB)
        )
    }

    private val argumentA = arguments[0]
    private val argumentB = arguments[1]

    private fun createError(vararg arguments: ParameterizeArgument<*>) =
        ParameterizeFailedError(
            listOf(ParameterizeFailure(Throwable("I'm the cause"), arguments.toList())),
            1, 1
        )

    @Test
    fun message_with_no_arguments_should_say_failed_with_no_arguments() {
        val error = createError()

        assertEquals(
            "Failed with no arguments",
            error.message
        )
    }

    @Test
    fun message_with_one_argument_should_show_argument_inline() {
        val error = createError(argumentA)

        assertEquals(
            "Failed with argument: $argumentA",
            error.message
        )
    }

    @Test
    fun message_with_multiple_arguments_should_show_arguments_on_separate_lines() {
        val error = createError(argumentA, argumentB)

        val expectedMessage = """
            Failed with arguments:
            ${'\t'}$argumentA
            ${'\t'}$argumentB
        """.trimIndent()

        assertEquals(expectedMessage, error.message)
    }

    @Test
    @NativeIgnore // Currently not possible on native: https://youtrack.jetbrains.com/issue/KT-59017/
    fun stack_trace_should_only_include_calls_for_the_cause() {
        val cause = Throwable("Cause message")

        val error = createError(argumentA, argumentB)
        val actualStackTrace = error.stackTraceToString()

        val expectedStackTrace = run {
            val errorStackTraceWithoutCalls = actualStackTrace
                .replaceAfter(error.message, "")

            val causeStackTraceWithCalls = actualStackTrace.lines()
                .dropWhile { !it.contains(cause.message!!) }
                .joinToString("\n")

            "$errorStackTraceWithoutCalls\n$causeStackTraceWithCalls"
        }

        assertEquals(expectedStackTrace, actualStackTrace)
    }
}
