package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

public class ParameterizeArgument<out T> internal constructor(
    public val parameter: KProperty<T>,
    public val argument: T
) {
    override fun toString(): String = "${parameter.name} = $argument"

    /**
     * Returns the [parameter][ParameterizeArgument.parameter] component.
     */
    public operator fun component1(): KProperty<T> = parameter

    /**
     * Returns the [argument] component.
     */
    public operator fun component2(): T = argument
}
