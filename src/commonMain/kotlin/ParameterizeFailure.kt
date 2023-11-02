package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

public class ParameterizeFailure internal constructor(
    public val failure: Throwable,
    public val arguments: List<Argument<*>>
) {
    override fun toString(): String =
        "ParameterizeFailure(failure=$failure, arguments=$arguments)"


    public class Argument<out T> internal constructor(
        public val parameter: KProperty<T>,
        public val argument: T
    ) {
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
