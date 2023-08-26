package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterizeScopeSpec {
    @Test
    fun string_representation_should_show_iteration_number() = parameterize {
        val iteration by parameter(0..100)

        assertEquals("ParameterizeScope(iteration = $iteration)", this.toString())
    }
}
