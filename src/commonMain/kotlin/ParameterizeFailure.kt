package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

/**
 * A [failure] thrown from a [parameterize] iteration, and the [arguments] that caused it.
 */
public class ParameterizeFailure internal constructor(
    /**
     * The failure thrown in a [parameterize] iteration.
     */
    public val failure: Throwable,

    /**
     * The parameter arguments when the [failure] occurred.
     */
    public val arguments: List<Argument<*>>
) {
    /** @suppress */
    override fun toString(): String =
        "ParameterizeFailure(failure=$failure, arguments=$arguments)"


    /**
     * A [parameter] and its [argument] when the [failure] occurred.
     */
    public class Argument<out T> internal constructor(
        /**
         * The Kotlin property for the parameter.
         */
        public val parameter: KProperty<T>,

        /**
         * The [parameter]'s argument when the [failure] occurred.
         */
        public val argument: T
    ) {
        /**
         * Returns `"parameter = argument"`, with the [parameter] name and [argument] value.
         */
        override fun toString(): String = "${parameter.name} = $argument"

        /**
         * Returns the [parameter][ParameterizeFailure.Argument.parameter] component.
         */
        public operator fun component1(): KProperty<T> = parameter

        /**
         * Returns the [argument] component.
         */
        public operator fun component2(): T = argument
    }
}
