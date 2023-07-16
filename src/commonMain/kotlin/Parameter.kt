package com.benwoodworth.parameterize

public class Parameter<out T> internal constructor(
    private val context: ParameterizeContext,
    public val argumentCount: ULong,
    public val getArgument: (index: ULong) -> T
) {
    override fun toString(): String =
        context.getVariableParameterOrNull(this)
            ?.value?.toString()
            ?: "Parameter value not initialized yet."


    public companion object
}


public fun <T> ParameterizeScope.parameter(vararg arguments: T): Parameter<T> =
    Parameter(arguments.size.toULong()) { arguments[it.toInt()] }

public fun <T> ParameterizeScope.parameter(arguments: List<T>): Parameter<T> =
    Parameter(arguments.size.toULong()) { arguments[it.toInt()] }