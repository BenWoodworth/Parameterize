package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterSpec {
    @Test
    @Suppress("UNUSED_VARIABLE")
    fun toString_before_variable_is_used_should_match_message_from_lazy() = parameterize {
        val parameterDelegate = parameter("unused")
        val lazyDelegate = lazy { "unused" }

        val parameterVariable by parameterDelegate
        val lazyVariable by lazyDelegate

        val expectedToString = lazyDelegate.toString().replace("Lazy", "Parameter")
        assertEquals(expectedToString, parameterDelegate.toString())
    }

    @Test
    fun toString_after_variable_is_used_should_match_the_value() = parameterize {
        val parameterDelegate = parameter(1, 2, 3, "a", "b", "c")
        val parameterVariable by parameterDelegate

        readVariable(parameterVariable)

        assertEquals(parameterVariable.toString(), parameterDelegate.toString())
    }
}
