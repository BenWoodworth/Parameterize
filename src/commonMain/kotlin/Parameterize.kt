@file:JvmMultifileClass
@file:JvmName("ParameterizeKt")

package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.DefaultParameterizeContext.parameterizeConfiguration
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
 *     val lazyValue by parameter { slowArgumentsComputation() }
 *
 *     // ...
 * }
 * ```
 *
 * For example, [parameterize] can be used to succinctly cover edge cases in a test. Here the test will be run
 * once for each of the possible ways `substring` can be contained within `string`:
 * ```
 *  • val string = "prefix-substring-suffix"  // in the middle
 *  • val string = "substring-suffix"         // at the start
 *  • val string = "prefix-substring"         // at the end
 *  • val string = "substring"                // the entire string
 * ```
 *
 * ```
 * @Test
 * fun contains_with_the_substring_present_should_be_true() = parameterize {
 *     val substring = "substring"
 *     val prefix by parameterOf("prefix-", "")
 *     val suffix by parameterOf("-suffix", "")
 *
 *     val string = "$prefix$substring$suffix"
 *     assertTrue(string.contains(substring), "\"$string\".contains(\"$substring\")")
 * }
 * ```
 *
 * Parameters may also depend on other parameters, which is especially useful be useful for testing edge/corner cases:
 * ```
 * @Test
 * fun int_should_not_equal_a_different_int() = parameterize {
 *     val int by parameterOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE)
 *     val differentInt by parameterOf(int + 1, int - 1)
 *
 *     assertNotEquals(int, differentInt)
 * }
 * ```
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
 * ***Note:** Should not be used as an extension function, as it will be changed to use a context receiver in the future.*
 *
 * @param onFailure See [ParameterizeConfiguration.Builder.onFailure]
 * @param onComplete See [ParameterizeConfiguration.Builder.onComplete]
 * @param decorator See [ParameterizeConfiguration.Builder.decorator]
 *
 * @throws ParameterizeException when [block] executes nondeterministically, with different control flow for the same parameter arguments.
 * @throws ParameterizeFailedError when [block] throws. (Configurable with [onComplete])
 */
//context(ParameterizeContext) // TODO
public fun ParameterizeContext.parameterize(
    onFailure: ParameterizeConfiguration.OnFailureScope.(failure: Throwable) -> Unit = parameterizeConfiguration.onFailure,
    onComplete: ParameterizeConfiguration.OnCompleteScope.() -> Unit = parameterizeConfiguration.onComplete,
    decorator: ParameterizeConfiguration.DecoratorScope.(iteration: () -> Unit) -> Unit = parameterizeConfiguration.decorator,
    block: ParameterizeScope.() -> Unit
) {
    contract {
        callsInPlace(onComplete, InvocationKind.EXACTLY_ONCE)
    }

    val parameterizeState = ParameterizeState()
    var breakEarly = false

    while (!breakEarly && parameterizeState.hasNextArgumentCombination) {
        parameterizeState.startNextIteration()

        parameterizeState.runIterationWithDecorator(decorator) {
            val scope = ParameterizeScope(parameterizeState)

            try {
                scope.block()
            } catch (failure: Throwable) {
                when {
                    failure is ParameterizeContinue -> {} // continue
                    failure is ParameterizeException && failure.parameterizeState === parameterizeState -> throw failure
                    else -> {
                        val result = parameterizeState.handleFailure(onFailure, failure)
                        breakEarly = result.breakEarly
                    }
                }
            } finally {
                scope.iterationCompleted = true
            }
        }
    }

    parameterizeState.handleComplete(onComplete)
}

/**
 * Calls [parameterize] with the default [ParameterizeContext].
 *
 * @see parameterize
 */
public fun parameterize(
    onFailure: ParameterizeConfiguration.OnFailureScope.(failure: Throwable) -> Unit = parameterizeConfiguration.onFailure,
    onComplete: ParameterizeConfiguration.OnCompleteScope.() -> Unit = parameterizeConfiguration.onComplete,
    decorator: ParameterizeConfiguration.DecoratorScope.(iteration: () -> Unit) -> Unit = parameterizeConfiguration.decorator,
    block: ParameterizeScope.() -> Unit
) {
    contract {
        callsInPlace(onComplete, InvocationKind.EXACTLY_ONCE)
    }

    with(DefaultParameterizeContext) {
        // Leave arguments unnamed so this call errors when new options are added
        parameterize(
            onFailure,
            onComplete,
            decorator,
            block
        )
    }
}

private inline fun ParameterizeState.runIterationWithDecorator(
    decorator: ParameterizeConfiguration.DecoratorScope.(iteration: () -> Unit) -> Unit,
    crossinline iteration: () -> Unit
) = with (ParameterizeConfiguration.DecoratorScope(this)) {
    var iterationInvoked = false

    decorator {
        if (iterationInvoked) {
            throw ParameterizeException(
                this@runIterationWithDecorator,
                "Decorator must invoke the iteration function exactly once, but was invoked twice"
            )
        }

        iteration()

        isLastIteration = !hasNextArgumentCombination
        iterationInvoked = true
    }

    if (!iterationInvoked) {
        throw ParameterizeException(
            this@runIterationWithDecorator,
            "Decorator must invoke the iteration function exactly once, but was not invoked"
        )
    }
}

internal data object ParameterizeContinue : Throwable()

internal class ParameterizeException(
    internal val parameterizeState: ParameterizeState,
    override val message: String,
    override val cause: Throwable? = null
) : Exception()

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
     *
     * Parameters may also depend on other parameters, which is especially useful be useful for testing edge/corner cases:
     * ```
     * @Test
     * fun int_should_not_equal_a_different_int() = parameterize {
     *     val int by parameterOf(0, 1, -1, Int.MAX_VALUE, Int.MIN_VALUE)
     *     val differentInt by parameterOf(int + 1, int - 1)
     *
     *     assertNotEquals(int, differentInt)
     * }
     * ```
     */
    public fun <T> parameter(arguments: Iterable<T>): Parameter<T> =
        Parameter(arguments)

    /** @suppress */
    public operator fun <T> Parameter<T>.provideDelegate(thisRef: Any?, property: KProperty<*>): ParameterDelegate<T> {
        if (iterationCompleted) {
            throw ParameterizeException(
                parameterizeState,
                "Cannot declare parameter `${property.name}` after its iteration has completed"
            )
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
