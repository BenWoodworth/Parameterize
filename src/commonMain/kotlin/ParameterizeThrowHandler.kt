package com.benwoodworth.parameterize

public typealias ParameterizeThrowHandler = ParameterizeThrowHandlerScope.(cause: Throwable) -> Unit

public class ParameterizeThrowHandlerScope internal constructor(
    private val state: ParameterizeState,
    private val cause: Throwable
) {
    public val arguments: List<ParameterizeArgument<*>> by lazy {
        state.getUsedArguments()
    }

    public operator fun ParameterizeFailedError.Companion.invoke(): ParameterizeFailedError =
        ParameterizeFailedError(arguments, cause)
}
