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
