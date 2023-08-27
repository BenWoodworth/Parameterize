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

public class ParameterDelegate<@Suppress("unused") out T> internal constructor() {
    /*
     * The internal state does not use the generic type, so T is purely for
     * syntax, helping Kotlin infer a property type from the parameter through
     * provideDelegate. So because T is not used, it can be safely cast to a
     * different generic type, despite it being an "unchecked cast".
     * (e.g. the declared property's type when providing the delegate)
     *
     * Instead, methods have their own generic type, checked against a property
     * that's passed in. Since the internal argument state is always of the same
     * type as the currently declared property, checking that the property taken
     * into the method is the same is enough to ensure that the argument being
     * read out is the correct type.
     */

    // Declared
    private var property: KProperty<*>? = null
    private var arguments: Iterable<*>? = null

    // Initialized
    private var argument: Any? = Uninitialized // T | Uninitialized
    private var argumentIterator: Iterator<*>? = null

    internal var hasBeenRead: Boolean = false
        private set

    internal fun reset() {
        property = null
        arguments = null
        argumentIterator = null
        argument = Uninitialized
        hasBeenRead = false
    }

    /**
     * @throws IllegalStateException if used before the argument has been initialized.
     */
    internal val isLastArgument: Boolean
        get() {
            check(argument !== Uninitialized) { "Argument has not been initialized" }
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
     * The [property] will be checked to make sure it's the same, and the current argument will remain the same.
     * The new [arguments] will be ignored in favor of re-using the existing arguments, under the assumption that they're equal.
     *
     * @throws ParameterizeException if already declared for a different [property].
     */
    internal fun <T> declare(property: KProperty<T>, arguments: Iterable<T>) {
        val declaredProperty = this.property

        if (declaredProperty == null) {
            this.property = property
            this.arguments = arguments
        } else if (!property.equalsProperty(declaredProperty)) {
            throw ParameterizeException("Expected to be declaring `${declaredProperty.name}`, but got `${property.name}`")
        }
    }

    /**
     * Initialize and return the argument.
     */
    private fun <T> initialize(arguments: Iterable<T>): T {
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
    internal fun <T> readArgument(property: KProperty<T>): T {
        val declaredProperty = checkNotNull(this.property) {
            "Cannot read argument before parameter delegate has been declared"
        }

        if (!property.equalsProperty(declaredProperty)) {
            throw ParameterizeException("Cannot use parameter delegate with `${property.name}`. Already declared for `${declaredProperty.name}`.")
        }

        var argument = argument
        if (argument !== Uninitialized) {
            @Suppress("UNCHECKED_CAST") // Argument is initialized with property's arguments, so must be T
            argument as T
        } else {
            val arguments = checkNotNull(arguments) {
                "Parameter delegate is declared for ${property.name}, but ${::arguments.name} is null"
            }

            @Suppress("UNCHECKED_CAST") // The arguments are declared for property, so must be Iterable<T>
            argument = initialize(arguments as Iterable<T>)
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

        check(argument != Uninitialized) {
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
    internal fun getPropertyArgumentOrNull(): Pair<KProperty<*>, *>? {
        val argument = argument
        if (argument === Uninitialized) return null

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
}
