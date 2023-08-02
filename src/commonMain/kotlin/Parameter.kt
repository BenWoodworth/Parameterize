package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

public class Parameter<T> internal constructor() {
    /*
     * Conceptually, a parameter is a reusable source of arguments, and is in
     * one of three different states internally.
     *
     * Undeclared:
     *   Completely null state, waiting to be setup with arguments.
     *
     *   Parameters can be reset to this state, enabling instances to be reused
     *   in the future.
     *
     *
     * Declared:
     *   Set up with arguments, but has not been read from yet.
     *
     *   The parameter property can not be known until the property is read,
     *   since it is provided by the property delegation, and the delegation
     *   first occurs when the property is read.
     *
     *   The argument and iterator are also loaded lazily, in case the property
     *   is not actually used, saving any potentially unnecessary computation.
     *
     *
     * Initialized:
     *   The parameter has been read, and has an argument set from the iterator.
     *
     *   The stored argument iterator will always have a next argument. When
     *   there is no next argument, it will be set to null, and then lazily set
     *   to a new iterator again when the next argument is needed.
     */

    // Declared
    private var arguments: Iterable<T>? = null

    // Initialized
    private var property: KProperty<T>? = null
    private var argumentIterator: Iterator<T>? = null
    private var argument: T? = null

    internal fun reset() {
        arguments = null
        property = null
        argumentIterator = null
        argument = null
    }


    internal val isDeclared: Boolean
        get() = arguments != null

    internal val isInitialized: Boolean
        get() = property != null

    internal val isLastArgument: Boolean
        get() = argumentIterator == null


    /**
     * Returns the current argument's `toString`, or a "not initialized" message.
     *
     *
     * Useful while debugging, e.g. inline hints that show property values:
     * ```
     * val letter by parameter { 'a'..'z' }  //letter$delegate: b
     * ```
     */
    override fun toString(): String =
        if (isInitialized) {
            argument.toString()
        } else {
            "Parameter argument not initialized yet."
        }

    internal fun declare(arguments: Iterable<T>) {
        check(!isDeclared) { "Parameter is already declared" }
        this.arguments = arguments
    }

    private fun initialize(property: KProperty<T>) {
        this.property = property
        nextArgument()
    }

    internal fun readArgument(property: KProperty<T>): T {
        if (!isInitialized) {
            initialize(property)
        }

        // The argument must be `T` and not `T?`, since this Parameter is initialized
        @Suppress("UNCHECKED_CAST")
        return argument as T
    }

    internal fun nextArgument() {
        var iterator = argumentIterator

        if (iterator == null) {
            val arguments = requireNotNull(arguments) { "Cannot iterate arguments before declared" }
            iterator = arguments.iterator()

            if (!iterator.hasNext()) throw ParameterizeContinue
            argumentIterator = iterator
        }

        argument = iterator.next()

        if (!iterator.hasNext()) {
            argumentIterator = null
        }
    }

    /**
     * Returns the property and argument if initialized, or null otherwise.
     */
    internal fun getPropertyArgumentOrNull(): Pair<KProperty<T>, T>? {
        val property = property ?: return null

        return property to readArgument(property)
    }
}
