package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterizeArgumentSpec {
    private val parameterizeArgument = run {
        val container = object : Any() {
            val parameter = Any()
        }

        ParameterizeArgument(container::parameter, container.parameter)
    }

    @Test
    fun string_representation_should_be_parameter_name_equalling_the_argument() {
        val expected = with(parameterizeArgument) { "${parameter.name} = $argument" }
        assertEquals(expected, parameterizeArgument.toString())
    }

    @Test
    fun component1_should_be_parameter() {
        val (component1, _) = parameterizeArgument
        assertEquals(parameterizeArgument.parameter, component1)
    }

    @Test
    fun component2_should_be_argument() {
        val (_, component2) = parameterizeArgument
        assertEquals(parameterizeArgument.argument, component2)
    }
}
