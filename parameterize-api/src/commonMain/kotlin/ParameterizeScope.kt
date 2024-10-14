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

@file:Suppress("KDocUnresolvedReference")

package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeScope.DeclaredParameter
import com.benwoodworth.parameterize.ParameterizeScope.Parameter
import com.benwoodworth.parameterize.internal.ParameterizeApiFriendModuleApi
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmField
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

/**
 * The scope for parameterized code blocks, providing extensions to create and declare [Parameter]s.
 *
 * With this interface it's possible to introspect arguments and their usage, or even modify the behavior of parameters.
 * Having direct access to the values, even the arguments themselves can be specially handled or transformed.
 *
 * As an example, a testing framework could take advantage of this to run a specific set of arguments and track each of
 * their values:
 *
 * ```
 * private class PickingParameterizeScope(
 *     private val parameterizeScope: ParameterizeScope,
 *     private val pickArgumentIndices: List<Int>
 * ) : ParameterizeScope {
 *     private var parameterIndex = 0
 *
 *     // Intercept the parameter, and declare it with just the one picked argument
 *     override fun <T> Parameter<T>.provideDelegate(thisRef: Nothing?, property: KProperty<*>): DeclaredParameter<T> {
 *         val pickArgumentIndex = pickArgumentIndices.getOrNull(parameterIndex++)
 *             ?: throw IllegalArgumentException("Argument index not specified for ${property.name}")
 *
 *         val pickedArgument = arguments.drop(pickArgumentIndex).take(1).singleOrNull()
 *             ?: throw IllegalArgumentException("Parameter ${property.name} has no argument at index $pickArgumentIndex")
 *
 *         return parameterizeScope.run {
 *             parameterOf(pickedArgument).provideDelegate(thisRef, property)
 *         }
 *     }
 * }
 * ```
 *
 * @see Parameter
 */
public interface ParameterizeScope {
    /**
     * Declares this [Parameter], allowing one of its arguments to be used as the [value][getValue] of the Kotlin
     * [property].
     *
     * @throws ParameterizeControlFlow if signaling non-local control flow in the [ParameterizeScope].
     *         Third-party code must not `catch` this type.
     *
     * @see Parameter
     */
    public operator fun <T> Parameter<T>.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): DeclaredParameter<T>

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

    // KT-72022
    /**
     * A [Parameter][Parameter] [declared][provideDelegate] with one of its [arguments][Parameter.arguments] so that it can be used
     * as the [value][getValue] of a Kotlin property.
     *
     * Equality is done by reference, making instances suitable for use as unique keys. The same instance is also reused
     * between iterations if a parameter's argument is unchanged making it possible to reliably track parameters through
     * iterations.
     *
     * @see Parameter
     */
    public class DeclaredParameter<out T>
    /** @suppress */
    @ParameterizeApiFriendModuleApi
    constructor(
        /**
         * The Kotlin property that this parameter was [declared][provideDelegate] for.
         *
         * @see Parameter
         */
        public val property: KProperty<*>,

        /**
         * The [argument] that this parameter was [declared][provideDelegate] with.
         *
         * @see Parameter
         */
        @JvmField
        public val argument: T
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

        /**
         * Returns the [argument] that this parameter was [declared][provideDelegate] with.
         *
         * @see argument
         * @see Parameter
         */
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun getValue(thisRef: Nothing?, property: KProperty<*>): T =
            argument
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
 * Used to [declare][ParameterizeScope.provideDelegate] a parameter with the supplied [arguments].
 *
 * ```
 * val letter by parameter('a'..'z')
 * ```
 *
 * @throws ParameterizeControlFlow when declared if signaling non-local control flow in the [ParameterizeScope].
 *         Third-party code must not `catch` this type.
 *
 * @see Parameter
 */
@Suppress("UnusedReceiverParameter") // Should only be accessible within parameterize scopes
public fun <T> ParameterizeScope.parameter(arguments: Sequence<T>): Parameter<T> =
    @OptIn(ExperimentalParameterizeApi::class)
    (Parameter(arguments))

/**
 * Used to [declare][ParameterizeScope.provideDelegate] a parameter with the supplied [arguments].
 *
 * ```
 * val letter by parameter('a'..'z')
 * ```
 *
 * @throws ParameterizeControlFlow when declared if signaling non-local control flow in the [ParameterizeScope].
 *         Third-party code must not `catch` this type.
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
 * @throws ParameterizeControlFlow when declared if signaling non-local control flow in the [ParameterizeScope].
 *         Third-party code must not `catch` this type.
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
 * @throws ParameterizeControlFlow when declared if signaling non-local control flow in the [ParameterizeScope].
 *         Third-party code must not `catch` this type.
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
 * @throws ParameterizeControlFlow when declared if signaling non-local control flow in the [ParameterizeScope].
 *         Third-party code must not `catch` this type.
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
 * The scope for lazy `parameter {}` blocks.
 */
@JvmInline
public value class LazyParameterScope @PublishedApi internal constructor(
    private val parameterizeScope: ParameterizeScope
) {
    /**
     * Declares this [Parameter], allowing one of its arguments to be used as the [value][getValue] of the Kotlin
     * [property].
     *
     * @throws ParameterizeException since declaring parameters in lazy `parameter {}` blocks is not currently supported.
     * @see Parameter
     * @see ParameterizeScope.provideDelegate
     */
    @Deprecated(
        "Declaring parameters in lazy `parameter {}` blocks is not currently supported.",
        level = DeprecationLevel.ERROR
    )
    public operator fun <T> Parameter<T>.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): DeclaredParameter<T> =
        parameterizeScope.run { provideDelegate(thisRef, property) }
}
