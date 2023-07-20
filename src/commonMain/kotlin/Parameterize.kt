package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

public fun parameterize(
    configuration: ParameterizeConfiguration = ParameterizeConfiguration.default,
    throwHandler: ParameterizeThrowHandler = configuration.throwHandler,
    block: ParameterizeScope.() -> Unit
) {
    var iteration = 0uL
    val context = ParameterizeContext()

    while (context.hasNextIteration) {
        try {
            ParameterizeScope(iteration, context).block()
        } catch (_: ParameterizeContinue) {
        } catch (thrown: Throwable) {
            throwHandler.invoke(ParameterizeThrowHandlerScope(context), thrown)
        }

        context.finishIteration()
        iteration++
    }
}

internal data object ParameterizeContinue : Throwable()

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


    public fun <T> parameter(arguments: Iterable<T>): Parameter<T> =
        context.createParameter(arguments)
}

public fun <T> ParameterizeScope.parameterOf(vararg arguments: T): Parameter<T> =
    parameter(arguments.asIterable())

public fun <T> ParameterizeScope.parameter(lazyArguments: () -> Iterable<T>): Parameter<T> =
    parameter(
        object : Iterable<T> {
            val arguments by lazy(lazyArguments)

            override fun iterator(): Iterator<T> = arguments.iterator()
        }
    )
