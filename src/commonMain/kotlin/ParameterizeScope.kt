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

/** @see parameterize */
@ParameterizeDsl
public interface ParameterizeScope {
    /** @suppress */
    public operator fun <T> Parameter<T>.provideDelegate(thisRef: Any?, property: KProperty<*>): DeclaredParameter<T>

    /** @suppress */
    public operator fun <T> DeclaredParameter<T>.getValue(thisRef: Any?, property: KProperty<*>): T

    /**
     * @constructor
     * **Experimental:** Prefer using the scope-limited [parameter] function, if possible.
     * The constructor will be made `@PublishedApi internal` once
     * [context parameters](https://github.com/Kotlin/KEEP/issues/367) are introduced to the language.
     *
     * @suppress
     */
    @JvmInline
    public value class Parameter<out T> @ExperimentalParameterizeApi constructor(
        public val arguments: Sequence<T>
    )

    /** @suppress */
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
        ReplaceWith("DeclaredParameter<T>"),
        DeprecationLevel.ERROR
    )
    public class ParameterDelegate<T> private constructor()
}

/**
 * Declare a parameter with the given [arguments].
 *
 * ```
 * val letter by parameter('a'..'z')
 * ```
 */
@Suppress("UnusedReceiverParameter") // Should only be accessible within parameterize scopes
public fun <T> ParameterizeScope.parameter(arguments: Sequence<T>): Parameter<T> =
    @OptIn(ExperimentalParameterizeApi::class)
    (Parameter(arguments))

/**
 * Declare a parameter with the given [arguments].
 *
 * ```
 * val letter by parameter('a'..'z')
 * ```
 */
public fun <T> ParameterizeScope.parameter(arguments: Iterable<T>): Parameter<T> =
    parameter(arguments.asSequence())

/**
 * Declare a parameter with the given [arguments].
 *
 * ```
 * val primeUnder20 by parameterOf(2, 3, 5, 7, 11, 13, 17, 19)
 * ```
 */
public fun <T> ParameterizeScope.parameterOf(vararg arguments: T): Parameter<T> =
    parameter(arguments.asSequence())

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
