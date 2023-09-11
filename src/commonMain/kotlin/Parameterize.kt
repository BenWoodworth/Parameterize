package com.benwoodworth.parameterize

import kotlin.jvm.JvmInline
import kotlin.reflect.KProperty

/**
 * @throws ParameterizeException when [block] executes non-deterministically, with different control flow for the same parameter arguments.
 * @throws ParameterizeFailedError when [block] throws. (Configurable with [throwHandler])
 */
public fun parameterize(
    configuration: ParameterizeConfiguration = ParameterizeConfiguration.default,
    throwHandler: ParameterizeThrowHandler = configuration.throwHandler,
    block: ParameterizeScope.() -> Unit
) {
    val context = ParameterizeContext()
    val scope = ParameterizeScope(context)

    while (context.hasNextIteration) {
        try {
            scope.block()
        } catch (_: ParameterizeContinue) {
        } catch (exception: ParameterizeException) {
            throw exception
        } catch (cause: Throwable) {
            ParameterizeThrowHandlerScope(context, cause).throwHandler(cause)
        }

        context.finishIteration()
    }
}

internal data object ParameterizeContinue : Throwable()

internal class ParameterizeException(override val message: String) : Exception(message)

@JvmInline
public value class Parameter<T> internal constructor(
    internal val arguments: Iterable<T>
)

public class ParameterizeScope internal constructor(
    private val context: ParameterizeContext,
) {
    override fun toString(): String =
        context.getReadParameters().joinToString(
            prefix = "ParameterizeScope(",
            separator = ", ",
            postfix = ")"
        ) { (parameter, argument) ->
            "${parameter.name} = $argument"
        }

    public fun <T> parameter(arguments: Iterable<T>): Parameter<T> =
        Parameter(arguments)

    public operator fun <T> Parameter<T>.provideDelegate(thisRef: Any?, property: KProperty<*>): ParameterDelegate<T> =
        @Suppress("UNCHECKED_CAST")
        context.declareParameter(property as KProperty<T>, arguments)

    public operator fun <T> ParameterDelegate<T>.getValue(thisRef: Any?, property: KProperty<*>): T =
        @Suppress("UNCHECKED_CAST")
        context.readParameter(this, property as KProperty<T>)
}

public fun <T> ParameterizeScope.parameterOf(vararg arguments: T): Parameter<T> =
    parameter(arguments.asIterable())

public inline fun <T> ParameterizeScope.parameter(crossinline lazyArguments: () -> Iterable<T>): Parameter<T> =
    parameter(object : Iterable<T> {
        private var arguments: Iterable<T>? = null

        override fun iterator(): Iterator<T> {
            var arguments = this.arguments

            if (arguments == null) {
                arguments = lazyArguments()
                this.arguments = arguments
            }

            return arguments.iterator()
        }
    })
