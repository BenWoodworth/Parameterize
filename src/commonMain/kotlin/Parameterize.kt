package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

public fun parameterize(block: ParameterizeScope.() -> Unit) {
    var iteration = 0uL
    val context = ParameterizeContext()

    while (context.hasNextIteration) {
        ParameterizeScope(iteration, context).block()

        context.finishIteration()
        iteration++
    }
}

public class Parameter<out T> internal constructor(
    internal val context: ParameterizeContext,
    internal val index: Int
) {
    override fun toString(): String {
        val variable = context.getParameterVariableOrNull(this)
            ?: return "Parameter value not initialized yet."

        return context.getParameterArgument(variable, this).toString()
    }
}

public class ParameterizeScope internal constructor(
    private val iteration: ULong,
    private val context: ParameterizeContext,
) {
    override fun toString(): String =
        "ParameterizeScope(iteration = $iteration)"

    public operator fun <T> Parameter<T>.getValue(thisRef: Any?, variable: KProperty<*>): T =
        context.getParameterArgument(variable, this)


    public fun <T> parameter(arguments: () -> Iterable<T>): Parameter<T> =
        context.createParameter(arguments)
}

public fun <T> ParameterizeScope.parameter(vararg arguments: T): Parameter<T> =
    parameter { arguments.asIterable() }
