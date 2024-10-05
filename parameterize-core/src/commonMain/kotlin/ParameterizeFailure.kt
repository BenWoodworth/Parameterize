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
        public val parameter: KProperty<*>,

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
        public operator fun component1(): KProperty<*> = parameter

        /**
         * Returns the [argument] component.
         */
        public operator fun component2(): T = argument
    }
}
