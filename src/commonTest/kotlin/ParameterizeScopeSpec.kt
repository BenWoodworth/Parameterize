@file:Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")

package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterizeScopeSpec {
    @Test
    fun string_representation_should_show_used_parameter_arguments_in_declaration_order() = parameterize {
        val a by parameterOf(1)
        val unused1 by parameterOf<Nothing>()
        val b by parameterOf(2)
        val unused2 by parameterOf<Nothing>()
        val c by parameterOf(3)

        // used in a different order
        useParameter(c)
        useParameter(b)
        useParameter(a)

        assertEquals("${ParameterizeScope::class.simpleName}(a = $a, b = $b, c = $c)", this.toString())
    }
}
