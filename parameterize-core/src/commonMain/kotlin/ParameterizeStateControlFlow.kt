package com.benwoodworth.parameterize

import kotlin.coroutines.Continuation

// KT-59625
internal abstract class ParameterizeStateControlFlow: ParameterizeControlFlow() {
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
