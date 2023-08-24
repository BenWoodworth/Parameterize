package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterizeScopeSpec {
    @Test
    fun toString_should_show_iteration_number() {
        var iteration = 0

        parameterize {
            val a by parameter((1..100).toList())

            assertEquals("ParameterizeScope(iteration = $iteration)", this@parameterize.toString())

            iteration++
        }
    }
}
