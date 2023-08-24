package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

/*
 * Conceptually, a parameter delegate is a reusable source of arguments, and
 * is in one of three different states internally.
 *
 * Undeclared:
 *   Completely null state, waiting to be set up with property and arguments.
 *
 *   Parameter delegates can be reset to this state, enabling instances to
 *   be reused in the future.
 *
 * Declared:
 *   Set up with a property and arguments, but has not been read from yet.
 *
 *   The argument and iterator are loaded lazily in case the parameter is not
 *   actually used, saving any potentially unnecessary computation.
 *
 * Initialized:
 *   The parameter has been read, and has an argument set from the iterator.
 *   Stays initialized until reset.
 *
 *   The stored argument iterator will always have a next argument. When
 *   there is no next argument, it will be set to null, and then lazily set
 *   to a new iterator again when the next argument is needed.
 *
 *   The delegate can be re-declared without being reset, marking it as
 *   unread again. The property will be checked to verify it's the same, but
 *   the new arguments will be assumed to be the same and ignored in favor
 *   of continuing through the current iterator.
 */

public class ParameterDelegate<T> internal constructor() {
    // Declared
    private var property: KProperty<T>? = null
    private var arguments: Iterable<T>? = null

    // Initialized
    private var argument: T = uninitialized // T | Uninitialized
    private var argumentIterator: Iterator<T>? = null

    /**
     * True if [declare]d, and [readArgument] has been used since the last [declare] call.
     */
    internal var hasBeenRead: Boolean = false
        private set

    internal fun reset() {
        property = null
        arguments = null
        argumentIterator = null
        argument = uninitialized
        hasBeenRead = false
    }

    /**
     * @throws IllegalStateException if used before the argument has been initialized.
     */
    internal val isLastArgument: Boolean
        get() {
            check(argument !== uninitialized) { "Argument has not been initialized" }
            return argumentIterator == null
        }


    /**
     * Returns a string representation of the current argument, or a "not initialized" message.
     *
     * Useful while debugging, e.g. inline hints that show property values:
     * ```
     * val letter by parameter { 'a'..'z' }  //letter$delegate: b
     * ```
     */
    override fun toString(): String =
        argument.toString()

    /**
     * Set up the delegate for a parameter [property] with the given [arguments].
     *
     * If this delegate is already [declare]d, [property] and [arguments] should be equal to those that were originally passed in.
     * The [property] will be checked to make sure it's the same, [hasBeenRead] will be set to `false`, and the current argument will remain the same.
     * The new [arguments] will be ignored in favor of re-using the existing arguments, under the assumption that they're equal.
     *
     * @throws ParameterizeException if already declared for a different [property].
     */
    internal fun declare(property: KProperty<T>, arguments: Iterable<T>) {
        val declaredProperty = this.property

        if (declaredProperty == null) {
            this.property = property
            this.arguments = arguments
        } else if (!property.equalsProperty(declaredProperty)) {
            throw ParameterizeException("Expected to be declaring `${declaredProperty.name}`, but got `${property.name}`")
        }

        hasBeenRead = false
    }

    /**
     * Initialize and return the argument.
     */
    private fun initialize(arguments: Iterable<T>): T {
        val iterator = arguments.iterator()
        if (!iterator.hasNext()) {
            throw ParameterizeContinue
        }

        val argument = iterator.next()
        this.argument = argument

        if (iterator.hasNext()) {
            argumentIterator = iterator
        }

        return argument
    }

    /**
     * Read the current argument, or initialize it from the arguments that were originally declared.
     */
    internal fun readArgument(property: KProperty<T>): T {
        val declaredProperty = checkNotNull(this.property) {
            "Cannot read argument before parameter delegate has been declared"
        }

        if (!property.equalsProperty(declaredProperty)) {
            throw ParameterizeException("Cannot use parameter delegate with `${property.name}`. Already declared for `${declaredProperty.name}`.")
        }

        var argument = argument
        if (argument == uninitialized) {
            val arguments = checkNotNull(arguments) {
                "Parameter delegate is declared for ${property.name}, but ${::arguments.name} is null"
            }

            argument = initialize(arguments)
        }

        hasBeenRead = true
        return argument
    }

    /**
     * Iterates the parameter argument.
     *
     * @throws IllegalStateException if the argument has not been initialized yet.
     */
    internal fun nextArgument() {
        val arguments = checkNotNull(arguments) {
            "Cannot iterate arguments before parameter delegate has been declared"
        }

        check(argument != uninitialized) {
            "Cannot iterate arguments before parameter argument has been initialized"
        }

        var iterator = argumentIterator
        if (iterator == null) {
            iterator = arguments.iterator()

            argumentIterator = iterator
        }

        argument = iterator.next()

        if (!iterator.hasNext()) {
            argumentIterator = null
        }
    }

    /**
     * Returns the property and argument if initialized, or `null` otherwise.
     */
    internal fun getPropertyArgumentOrNull(): Pair<KProperty<T>, T>? {
        val argument = argument
        if (argument === uninitialized) return null

        val property = checkNotNull(property) {
            "Parameter delegate argument is initialized, but ${::property.name} is null"
        }

        return property to argument
    }

    /**
     * A placeholder value for [argument] to signify that it is not initialized.
     *
     * Since the argument itself may be nullable, `null` can't be used instead,
     * as that may actually be the argument's value.
     */
    private object Uninitialized {
        override fun toString(): String = "Parameter argument not initialized yet."
    }

    /**
     * A hack to use [Uninitialized] as a value for [argument], abusing
     * generic type erasure since [T] is effectively [Any]? at runtime.
     *
     * Developers should be conscious of this every time [argument] is read, and
     * check that it's not [uninitialized] before using.
     *
     * Ideally [argument]'s type should be `T | Uninitialized`, but Kotlin
     * doesn't support union types so instead this abuses generic type erasure
     * to disguise [Uninitialized] as T. When denotable unions are added to the
     * language ([KT-13108](https://youtrack.jetbrains.com/issue/KT-13108)),
     * this property should be removed, and [argument]'s type should be changed
     * to be `T | Uninitialized` instead.
     */
    @Suppress("UNCHECKED_CAST")
    private inline val uninitialized: T
        get() = Uninitialized as T
}
