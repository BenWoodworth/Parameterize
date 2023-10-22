package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.DefaultParameterizeContext.parameterizeConfiguration
import kotlin.jvm.JvmInline
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
 *     assertTrue(string.contains(substring))
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
 *           ***Note:** Should not be used as an extension function, as it will be changed to use a context receiver in the future.*
 * @throws ParameterizeException when [block] executes nondeterministically, with different control flow for the same parameter arguments.
 * @throws ParameterizeFailedError when [block] throws. (Configurable with [throwHandler])
 */
//context(ParameterizeContext) // TODO
public fun ParameterizeContext.parameterize(
    throwHandler: ParameterizeThrowHandler = parameterizeConfiguration.throwHandler,
    block: ParameterizeScope.() -> Unit
) {
    val state = ParameterizeState()
    val scope = ParameterizeScope(state)

    while (state.startNextIteration()) {
        try {
            scope.block()
        } catch (_: ParameterizeContinue) {
        } catch (exception: ParameterizeException) {
            throw exception
        } catch (cause: Throwable) {
            ParameterizeThrowHandlerScope(state, cause).throwHandler(cause)
        }
    }
}

/**
 * Calls [parameterize] with the default [ParameterizeContext].
 *
 * @see parameterize
 */
public fun parameterize(
    throwHandler: ParameterizeThrowHandler = parameterizeConfiguration.throwHandler,
    block: ParameterizeScope.() -> Unit
) {
    with(DefaultParameterizeContext) {
        parameterize(
            throwHandler,
            block
        )
    }
}

internal data object ParameterizeContinue : Throwable()

internal class ParameterizeException(override val message: String) : Exception(message)

@JvmInline
public value class Parameter<out T> internal constructor(
    internal val arguments: Iterable<T>
)

public class ParameterizeScope internal constructor(
    private val state: ParameterizeState,
) {
    override fun toString(): String =
        state.getUsedParameters().joinToString(
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

    public operator fun <T> Parameter<T>.provideDelegate(thisRef: Any?, property: KProperty<*>): ParameterDelegate<T> =
        @Suppress("UNCHECKED_CAST")
        state.declareParameter(property as KProperty<T>, arguments)

    public operator fun <T> ParameterDelegate<T>.getValue(thisRef: Any?, property: KProperty<*>): T =
        @Suppress("UNCHECKED_CAST")
        state.getParameterArgument(this, property as KProperty<T>)
}

/**
 * Declare a parameter with the given [arguments].
 *
 * ```
 * val primeUnder20 by parameterOf(2, 3, 5, 7, 11, 13, 17, 19)
 * ```
 */
public fun <T> ParameterizeScope.parameterOf(vararg arguments: T): Parameter<T> =
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
