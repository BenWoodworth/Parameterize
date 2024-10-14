package com.benwoodworth.parameterize

internal actual class ParameterizeContinue private constructor(
    actual override val parameterizeState: ParameterizeState
) : ParameterizeStateControlFlow() {
    actual override val cause: Nothing?
        get() = null

    override fun fillInStackTrace(): Throwable = this

    actual companion object {
        actual operator fun invoke(
            state: ParameterizeState
        ): ParameterizeContinue {
            return ParameterizeContinue(state)
        }
    }
}

internal actual class ParameterizeBreak private constructor(
    actual override val parameterizeState: ParameterizeState,
    actual override val cause: ParameterizeException,
) : ParameterizeStateControlFlow() {
    override fun fillInStackTrace(): Throwable = this

    actual companion object {
        actual operator fun invoke(
            state: ParameterizeState,
            cause: ParameterizeException,
        ): ParameterizeBreak {
            return ParameterizeBreak(state, cause)
        }
    }
}


