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
 * The parameter state is responsible for managing a parameter in the
 * [parameterize] DSL and maintaining an argument.
 *
 * When first declared, the parameter [property] it was
 * declared with will be stored, along with the argument.
 *
 */
internal class ParameterState<T>(val argument: T, val isLast: Boolean = false) {
    var property: KProperty<T>? = null

    var hasBeenUsed: Boolean = false

    /**
     * Returns a string representation of the current argument, or a "not declared" message.
     */
    override fun toString(): String = argument.toString()

    fun useArgument() {
        hasBeenUsed = true
    }

    /**
     * Returns the property and argument.
     *
     * @throws IllegalStateException if this parameter is not declared.
     */
    fun getFailureArgument(): ParameterizeFailure.Argument<*> {
        val property = checkNotNull(property) {
            "Cannot get failure argument before parameter has been declared"
        }

        return ParameterizeFailure.Argument(property, argument)
    }
}
