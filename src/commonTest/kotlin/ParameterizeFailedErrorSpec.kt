package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterizeFailedErrorSpec {
    private val parameterA = "argumentA"
    private val parameterB = "argumentB"

    @Test
    fun message_with_no_arguments_should_say_failed_with_no_arguments() {
        val error = ParameterizeFailedError(listOf(), Throwable("I'm the cause"))

        assertEquals(
            "Failed with no arguments",
            error.message
        )
    }

    @Test
    fun message_with_one_argument_should_show_argument_inline() {
        val error = ParameterizeFailedError(
            listOf(::parameterA to parameterA),
            Throwable("I'm the cause")
        )

        assertEquals(
            "Failed with argument: ${::parameterA.name} = $parameterA",
            error.message
        )
    }

    @Test
    fun message_with_multiple_arguments_should_show_arguments_on_separate_lines() {
        val error = ParameterizeFailedError(
            listOf(::parameterA to parameterA, ::parameterB to parameterB),
            Throwable("I'm the cause")
        )

        val expectedMessage = """
            Failed with arguments:
            ${'\t'}${::parameterA.name} = $parameterA
            ${'\t'}${::parameterB.name} = $parameterB
        """.trimIndent()

        assertEquals(expectedMessage, error.message)
    }

    @Test
    @NativeIgnore // Currently not possible on native: https://youtrack.jetbrains.com/issue/KT-59017/
    fun stack_trace_should_only_include_calls_for_the_cause() {
        val cause = Throwable("Cause message")

        val error = ParameterizeFailedError(
            listOf(::parameterA to parameterA, ::parameterB to parameterB),
            cause
        )

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
