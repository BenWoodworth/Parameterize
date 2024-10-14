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

// KT-59625
internal abstract class ParameterizeStateControlFlow : ParameterizeControlFlow() {
    abstract val parameterizeState: ParameterizeState
}

internal expect class ParameterizeContinue : ParameterizeStateControlFlow {
    override val parameterizeState: ParameterizeState
    override val cause: Nothing?

    companion object {
        operator fun invoke(state: ParameterizeState): ParameterizeContinue
    }
}

internal expect class ParameterizeBreak : ParameterizeStateControlFlow {
    override val parameterizeState: ParameterizeState
    override val cause: ParameterizeException

    companion object {
        operator fun invoke(state: ParameterizeState, cause: ParameterizeException): ParameterizeBreak
    }
}
