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
 * [parameterize] DSL, maintaining an argument, and lazily loading the next ones
 * in as needed.
 *
 * When first declared, the parameter [property] it was
 * declared with will be stored, along with a new argument iterator and the
 * first argument from it. The arguments are lazily read in from the iterator as
 * they're needed, using [isLastArgument] as an indicator. The stored iterator
 * will always have a next argument available, and will be set to null when its
 * last argument is read in to release its reference
 *
 */
internal class ParameterState<T>(argumentIterator: Iterator<T>) {
    /**
     * Set up the delegate with the given [arguments].
     *
     * @throws ParameterizeContinue if [arguments] is empty.
     */
    constructor(arguments: Sequence<T>) : this(arguments.iterator())

    init {
        if (!argumentIterator.hasNext()) throw ParameterizeContinue // Before changing any state
    }

    var argument: T = argumentIterator.next()
        private set
    var property: KProperty<T>? = null
    private var argumentIterator: Iterator<T>? = argumentIterator.takeIf { it.hasNext() }

    var hasBeenUsed: Boolean = false
        private set

    /**
     * @throws IllegalStateException if used before the argument has been declared.
     */
    val isLastArgument: Boolean
        get() = argumentIterator == null


    /**
     * Returns a string representation of the current argument, or a "not declared" message.
     */
    override fun toString(): String = argument.toString()

    fun useArgument() {
        hasBeenUsed = true
    }

    /**
     * Iterates the parameter argument.
     *
     * @throws IllegalStateException if the argument has not been declared yet.
     */
    fun nextArgument() {
        val iterator = argumentIterator ?: error("Cannot iterate arguments before parameter has been declared")

        argument = iterator.next()
        argumentIterator = iterator.takeIf { it.hasNext() }
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
