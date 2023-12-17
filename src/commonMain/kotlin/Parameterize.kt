@file:JvmMultifileClass
@file:JvmName("ParameterizeKt")

package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.DefaultParameterizeContext.parameterizeConfiguration
import com.benwoodworth.parameterize.ParameterizeConfiguration.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

/**
 * With configuration defaults pulled in from the [ParameterizeContext], the parameterized [block] will be run for each
 * combination of arguments as declared by [parameter][ParameterizeScope.parameter] functions:
 * ```
 * parameterize {
 *     val letter by parameter('a'..'z')
 *     val primeUnder20 by parameterOf(2, 3, 5, 7, 11, 13, 17, 19)
 *     val computedValue by parameter { slowArgumentsComputation() }
 *
 *     // ...
 * }
 * ```
 *
 * With its default behavior, [parameterize] is strictly an alternative syntax to nested `for` loops, with loop
 * variables defined within the body instead of up front, and without the indentation that's required for additional
 * inner loops. For example, this `sequence` is equivalent to nested `red`/`green`/`blue` `for` loops with the same
 * dependent ranges:
 *
 * ```kotlin
 * val reddishYellows = sequence {
 *     parameterize {
 *         val red by parameter(128..255)
 *         val green by parameter(64..(red - 32))
 *         val blue by parameter(0..(green - 64))
 *
 *         yield(Color(red, green, blue))
 *     }
 * }
 * ```
 *
 * In addition to its default behavior, [parameterize] is also configurable with options to decorate its iterations,
 * handle and record failures, and summarize the overall loop execution. The flexibility [parameterize] offers makes it
 * suitable for many different specific use cases. Supported use cases include built in ways to access the named
 * parameter arguments when a failure occurs, recording failures while continuing to the next iteration, and throwing a
 * comprehensive multi-failure with the recorded failures and the arguments when each occurred.
 *
 * ### Restrictions
 *
 * - The parameterized [block] must be deterministic, executing the same way for the same parameter arguments.
 *
 * - Parameter arguments must not be mutated, as they are re-used between iterations, and mutations from previous
 *   iterations could result in different execution, breaking the determinism assumption.
 *
 * - Parameters cannot be declared within another parameter's arguments, such as nesting within a lazy `parameter {}`.
 *
 * - Care should be taken with any asynchronous code, since the order that parameters are used must be the same between
 *   iterations, and all async code must be awaited before the [block] completes.
 *
 * @receiver [ParameterizeContext] provides the configuration for this function's default arguments.
 *
 * **Note:** Should not be used as an extension function, as it will be changed to use a context receiver in the future.
 *
 * @throws ParameterizeException if the DSL is used incorrectly. (See restrictions)
 *
 * @param decorator See [ParameterizeConfiguration.Builder.decorator]
 * @param onFailure See [ParameterizeConfiguration.Builder.onFailure]
 * @param onComplete See [ParameterizeConfiguration.Builder.onComplete]
 */
//context(ParameterizeContext) // TODO
public inline fun ParameterizeContext.parameterize(
    noinline decorator: suspend DecoratorScope.(iteration: suspend DecoratorScope.() -> Unit) -> Unit = parameterizeConfiguration.decorator,
    noinline onFailure: OnFailureScope.(failure: Throwable) -> Unit = parameterizeConfiguration.onFailure,
    noinline onComplete: OnCompleteScope.() -> Unit = parameterizeConfiguration.onComplete,
    block: ParameterizeScope.() -> Unit
) {
    contract {
        callsInPlace(onComplete, InvocationKind.EXACTLY_ONCE)
    }

    val configuration = ParameterizeConfiguration(parameterizeConfiguration) {
        this.decorator = decorator
        this.onFailure = onFailure
        this.onComplete = onComplete
    }

    parameterize(configuration, block)
}

/**
 * Calls [parameterize] with the default [ParameterizeContext].
 *
 * @see parameterize
 */
public inline fun parameterize(
    noinline decorator: suspend DecoratorScope.(iteration: suspend DecoratorScope.() -> Unit) -> Unit = parameterizeConfiguration.decorator,
    noinline onFailure: OnFailureScope.(failure: Throwable) -> Unit = parameterizeConfiguration.onFailure,
    noinline onComplete: OnCompleteScope.() -> Unit = parameterizeConfiguration.onComplete,
    block: ParameterizeScope.() -> Unit
) {
    contract {
        callsInPlace(onComplete, InvocationKind.EXACTLY_ONCE)
    }

    val configuration = ParameterizeConfiguration {
        this.decorator = decorator
        this.onFailure = onFailure
        this.onComplete = onComplete
    }

    parameterize(configuration, block)
}

/**
 * Calls [parameterize] with the given [configuration]'s options.
 *
 * @see parameterize
 */
public inline fun parameterize(
    configuration: ParameterizeConfiguration,
    block: ParameterizeScope.() -> Unit
) {
    // Exercise extreme caution modifying this code, since the iterator is sensitive to the behavior of this function.
    // Code inlined from a previous version could have subtly different semantics when interacting with the runtime
    // iterator of a later release, and would be major breaking change that's difficult to detect.

    val iterator = ParameterizeIterator(configuration)

    while (true) {
        val scope = iterator.nextIteration() ?: break

        try {
            scope.block()
        } catch (failure: Throwable) {
            iterator.handleFailure(failure)
        }
    }
}

/** @see parameterize */
public class ParameterizeScope internal constructor(
    internal val parameterizeState: ParameterizeState,
) {
    internal var iterationCompleted: Boolean = false

    /** @suppress */
    override fun toString(): String =
        parameterizeState.getFailureArguments().joinToString(
            prefix = "ParameterizeScope(",
            separator = ", ",
            postfix = ")"
        ) { (parameter, argument) ->
            "${parameter.name} = $argument"
        }

    /**
     * Declare a parameter with the given [arguments].
     *
     * ```
     * val letter by parameter('a'..'z')
     * ```
     */
    public fun <T> parameter(arguments: Iterable<T>): Parameter<T> =
        Parameter(arguments)

    /** @suppress */
    public operator fun <T> Parameter<T>.provideDelegate(thisRef: Any?, property: KProperty<*>): ParameterDelegate<T> {
        parameterizeState.checkState(!iterationCompleted) {
            "Cannot declare parameter `${property.name}` after its iteration has completed"
        }

        @Suppress("UNCHECKED_CAST")
        return parameterizeState.declareParameter(property as KProperty<T>, arguments)
    }

    /** @suppress */
    public operator fun <T> ParameterDelegate<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!iterationCompleted) parameterState.useArgument()
        return argument
    }


    /** @suppress */
    @JvmInline
    public value class Parameter<out T> internal constructor(
        internal val arguments: Iterable<T>
    )

    /** @suppress */
    public class ParameterDelegate<out T> internal constructor(
        internal val parameterState: ParameterState,
        internal val argument: T
    ) {
        /**
         * Returns a string representation of the current argument.
         *
         * Useful while debugging, e.g. inline hints that show property values:
         * ```
         * val letter by parameter { 'a'..'z' }  //letter$delegate: b
         * ```
         */
        override fun toString(): String =
            argument.toString()
    }
}

/**
 * Declare a parameter with the given [arguments].
 *
 * ```
 * val primeUnder20 by parameterOf(2, 3, 5, 7, 11, 13, 17, 19)
 * ```
 */
public fun <T> ParameterizeScope.parameterOf(vararg arguments: T): ParameterizeScope.Parameter<T> =
    parameter(arguments.asIterable())

/**
 * Declares a parameter with the given [lazyArguments].
 * The arguments are only computed the first time the parameter is used, and not at all if used.
 *
 * This `parameter` function is useful to avoid computing the arguments every iteration.
 * Instead, these arguments will only be computed the first time the parameter is used.
 *
 * ```
 * val evenNumberSquared by parameter {
 *     numbers
 *         .filter { it % 2 == 0 }
 *         .map { it * it }
 * }
 * ```
 *
 * ### Restrictions
 *
 * - The [lazyArguments] block should not have side effects. Since it's not run every iteration, side effects could make
 *   the execution different from future iterations, breaking [parameterize]'s determinism assumption.
 */
public inline fun <T> ParameterizeScope.parameter(
    crossinline lazyArguments: () -> Iterable<T>
): ParameterizeScope.Parameter<T> =
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
