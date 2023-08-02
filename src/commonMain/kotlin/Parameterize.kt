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

public class ParameterizeScope internal constructor(
    private val iteration: ULong,
    private val context: ParameterizeContext,
) {
    override fun toString(): String =
        "ParameterizeScope(iteration = $iteration)"

    public operator fun <T> Parameter<T>.getValue(thisRef: Any?, property: KProperty<*>): T =
        @Suppress("UNCHECKED_CAST")
        readArgument(property as KProperty<T>)


    public fun <T> parameter(arguments: Iterable<T>): Parameter<T> =
        context.declareParameter(arguments)
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
