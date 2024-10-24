/*
 * Copyright 2024 Ben Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeScope.Parameter
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

/**
 * The scope for parameterized code blocks, exposing [parameter] builders and enabling them to be declared.
 *
 * Delegating to this interface makes it possible to observe parameter declaration, introspect arguments, or even
 * transform/augment the parameter that's passed to the underlying scope in order to modify their behavior.
 *
 * As an example, a testing framework could take advantage of this to run a specific set of arguments and track each of
 * their values:
 *
 * ```
 * private class PickingParameterizeScope(
 *     private val parameterizeScope: ParameterizeScope,
 *     private val pickArgumentIndices: List<Int>
 * ) : ParameterizeScope by parameterizeScope {
 *     private var parameterIndex = 0
 *
 *     // Intercept the parameter, and declare it with just the one picked argument
 *     override fun <T> Parameter<T>.provideDelegate(thisRef: Nothing?, property: KProperty<*>): DeclaredParameter<T> {
 *         val pickArgumentIndex = pickArgumentIndices.getOrNull(parameterIndex++)
 *             ?: throw IllegalArgumentException("Argument index not specified for ${property.name}")
 *
 *         val pickedArgument = arguments.drop(pickArgumentIndex).elementAtOrNull(1)
 *             ?: throw IllegalArgumentException("Parameter ${property.name} has no argument at index $pickArgumentIndex")
 *
 *         return parameterizeScope.run {
 *             parameterOf(pickedArgument).provideDelegate(thisRef, property)
 *         }
 *     }
 * }
 * ```
 *
 * A test framework might also want to report the values that were used. For that, something like this is also possible:
 * ```
 * private class TrackingParameterizeScope(
 *     private val parameterizeScope: ParameterizeScope,
 * ) : ParameterizeScope by parameterizeScope {
 *     private val usedParameters = mutableSetOf<DeclaredParameter<*>>()
 *
 *     // Track the parameters that have been used, that way it's possible to report only the relevant values
 *     override fun <T> DeclaredParameter<T>.getValue(thisRef: Nothing?, property: KProperty<*>): T {
 *         usedParameters += this
 *         return parameterizeScope.run { getValue(thisRef, property) }
 *     }
 * }
 * ```
 *
 * @see Parameter
 */
@ParameterizeDsl
public interface ParameterizeScope {
    /**
     * Declares this [Parameter], allowing one of its arguments to be used as the [value][getValue] of the Kotlin
     * [property].
     *
     * @throws ParameterizeException if this [Parameter] is being declared with the wrong [property].
     * @see Parameter
     */
    public operator fun <T> Parameter<T>.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): DeclaredParameter<T>

    /**
     * Returns the argument that this parameter was [declared][provideDelegate] with.
     *
     * @see Parameter
     */
    public operator fun <T> DeclaredParameter<T>.getValue(thisRef: Nothing?, property: KProperty<*>): T

    /**
     * A sequence of [arguments] that can be [declared][provideDelegate] within a [ParameterizeScope] to make one of the
     * [values][getValue] available.
     *
     * Parameter builders are available as extensions on [ParameterizeScope], making them straightforward to
     * [declare][provideDelegate] given some specified/listed/computed arguments:
     *
     * ```
     * val letter by parameter('a'..'z')
     * val primeUnder20 by parameterOf(2, 3, 5, 7, 11, 13, 17, 19)
     * val computedValue by parameter { lazilyComputedValues() }
     * ```
     *
     * @constructor
     * **Experimental:** Prefer using the scope-limited [parameter] function, if possible.
     * The constructor will be made `@PublishedApi internal` once
     * [context parameters](https://github.com/Kotlin/KEEP/issues/367) are introduced to the language.
     */
    @JvmInline
    public value class Parameter<out T> @ExperimentalParameterizeApi constructor(
        /**
         * The [value][getValue]s that this parameter can be [declared][provideDelegate] with.
         */
        public val arguments: Sequence<T>
    )

    /**
     * A [Parameter] declared in a [ParameterizeScope], providing access to the selected argument and enabling its use
     * through [property delegation][DeclaredParameter.getValue].
     *
     * @see Parameter
     */
    public class DeclaredParameter<out T> internal constructor(
        internal val parameterState: ParameterState,
        internal val argument: T
    ) {
        /**
         * Returns a string representation of the current argument.
         *
         * Useful while debugging, e.g. inline hints that show property values:
         * ```
         * val letter by parameter('a'..'z')  //letter$delegate: b
         * ```
         */
        override fun toString(): String =
            argument.toString()
    }

    /** @suppress */
    @Suppress("unused")
    @Deprecated(
        "Renamed to DeclaredParameter",
        ReplaceWith(
            "ParameterizeScope.DeclaredParameter<T>",
            "com.benwoodworth.parameterize.ParameterizeScope"
        ),
        DeprecationLevel.ERROR
    )
    public class ParameterDelegate<T> private constructor()
}

/**
 * Used to [declare][ParameterizeScope.provideDelegate] a parameter with the supplied [arguments].
 *
 * ```
 * val letter by parameter('a'..'z')
 * ```
 *
 * @see Parameter
 */
@Suppress("UnusedReceiverParameter") // Should only be accessible within parameterize scopes
@OptIn(ExperimentalParameterizeApi::class)
public fun <T> ParameterizeScope.parameter(arguments: Sequence<T>): Parameter<T> =
    Parameter(arguments)

/**
 * Used to [declare][ParameterizeScope.provideDelegate] a parameter with the supplied [arguments].
 *
 * ```
 * val letter by parameter('a'..'z')
 * ```
 *
 * @see Parameter
 */
public fun <T> ParameterizeScope.parameter(arguments: Iterable<T>): Parameter<T> =
    parameter(arguments.asSequence())

/**
 * Used to [declare][ParameterizeScope.provideDelegate] a parameter with the listed [arguments].
 *
 * ```
 * val primeUnder20 by parameterOf(2, 3, 5, 7, 11, 13, 17, 19)
 * ```
 *
 * @see Parameter
 */
public fun <T> ParameterizeScope.parameterOf(vararg arguments: T): Parameter<T> =
    parameter(arguments.asSequence())

/**
 * Used to [declare][ParameterizeScope.provideDelegate] a parameter with the computed [lazyArguments].
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
 *
 * @see Parameter
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("parameterLazySequence")
public inline fun <T> ParameterizeScope.parameter(
    crossinline lazyArguments: LazyParameterScope.() -> Sequence<T>
): Parameter<T> =
    parameter(object : Sequence<T> {
        private var arguments: Sequence<T>? = null

        override fun iterator(): Iterator<T> {
            var arguments = this.arguments

            if (arguments == null) {
                arguments = LazyParameterScope(this@parameter).lazyArguments()
                this.arguments = arguments
            }

            return arguments.iterator()
        }
    })

/**
 * Used to [declare][ParameterizeScope.provideDelegate] a parameter with the computed [lazyArguments].
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
 *
 * @see Parameter
 */
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("parameterLazyIterable")
public inline fun <T> ParameterizeScope.parameter(
    crossinline lazyArguments: LazyParameterScope.() -> Iterable<T>
): Parameter<T> =
    parameter {
        lazyArguments().asSequence()
    }

/**
 * Used to prevent `parameter` functions from being used within lazy `parameter {}` blocks, since doing so is not
 * currently supported.
 *
 * @suppress
 * @see ParameterizeDsl
 */
@JvmInline
@ParameterizeDsl
public value class LazyParameterScope @PublishedApi internal constructor(
    private val parameterizeScope: ParameterizeScope
)
