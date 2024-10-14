package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.test.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ParameterizeStateControlFlowTest {
    private fun controlFlows(): List<Pair<String, ParameterizeStateControlFlow>> {
        lateinit var state: ParameterizeState
        parameterize { state = parameterizeState }

        return listOf(
            ParameterizeContinue(state),
            ParameterizeBreak(state, ParameterizeException("example")),
        ).map { it::class.simpleName!! to it }
    }

    @Test
    @[NativeIgnore WasmJsIgnore WasmWasiIgnore]
    fun stack_trace_should_not_be_populated() {
        testAll(controlFlows()) { controlFlow ->
            assertEquals(emptyList(), controlFlow.stackTraceLines)
        }
    }
}
