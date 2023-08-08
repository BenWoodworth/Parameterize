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
        } catch (exception: ParameterizeException) {
            throw exception
        } catch (thrown: Throwable) {
            throwHandler.invoke(ParameterizeThrowHandlerScope(context), thrown)
        }

        context.finishIteration()
        iteration++
    }
}

internal data object ParameterizeContinue : Throwable()

internal class ParameterizeException(override val message: String) : Exception(message)

public class ParameterizeScope internal constructor(
    private val iteration: ULong,
    private val context: ParameterizeContext,
) {
    private var readingParameters = 0

    override fun toString(): String =
        "ParameterizeScope(iteration = $iteration)"

    public operator fun <T> Parameter<T>.getValue(thisRef: Any?, property: KProperty<*>): T =
        try {
            readingParameters++

            if (context != this@ParameterizeScope.context) {
                throw ParameterizeException("Cannot initialize `a` with parameter from another scope")
            }

            @Suppress("UNCHECKED_CAST")
            readArgument(property as KProperty<T>)
        } finally {
            readingParameters--
        }

    public fun <T> parameter(arguments: Iterable<T>): Parameter<T> {
        if (readingParameters != 0) {
            throw ParameterizeException("Declaring a parameter within another is not supported")
        }

        return context.declareParameter(arguments)
    }
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
