package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

/**
 * The parameter state is responsible for managing a parameter in the
 * [parameterize] DSL, maintaining an argument, and lazily loading the next ones
 * in as needed.
 *
 * The state can also be reset for reuse later with another parameter, allowing
 * the same instance to be shared, saving on unnecessary instantiations. Since
 * this means the underlying argument type can change, this class doesn't have a
 * generic type for it, and instead has each function pull a generic type from a
 * provided property, and validates it against the property this parameter was
 * declared with. This ensures that the argument type is correct at runtime, and
 * also validates that the parameter is in fact being used with the expected
 * property.
 *
 * When first declared, the parameter [property] and the [arguments] it was
 * declared with will be stored, along with a new argument iterator and the
 * first argument from it. The arguments are lazily read in from the iterator as
 * they're needed, and will seamlessly continue with the start again after the
 * last argument, using [isLastArgument] as an indicator. The stored iterator
 * will always have a next argument available, and will be set to null when its
 * last argument is read in to release its reference until the next iterator is
 * created to begin from the start again.
 *
 * Since each [parameterize] iteration should declare the same parameters,
 * in the same order with the same arguments, declared with the same
 * already-declared state instance as the previous iteration. Calling [declare]
 * again will leave the state unchanged, only serving to validate that the
 * parameter was in fact declared the same as before. The new arguments are
 * ignored since they're assumed to be the same, and the state remains unchanged
 * in favor of continuing through the current iterator where it left off.
 */
internal class ParameterState {
    private var property: KProperty<*>? = null
    private var arguments: Iterable<*>? = null
    private var argument: Any? = null // T
    private var argumentIterator: Iterator<*>? = null

    var hasBeenUsed: Boolean = false
        private set

    internal fun reset() {
        property = null
        arguments = null
        argument = null
        argumentIterator = null
        hasBeenUsed = false
    }

    /**
     * @throws IllegalStateException if used before the argument has been declared.
     */
    val isLastArgument: Boolean
        get() {
            checkNotNull(property) { "Parameter has not been declared" }
            return argumentIterator == null
        }


    /**
     * Returns a string representation of the current argument, or a "not declared" message.
     */
    override fun toString(): String =
        if (property == null) {
            "Parameter not declared yet."
        } else {
            argument.toString()
        }

    /**
     * Set up the delegate for a parameter [property] with the given [arguments].
     *
     * If this delegate is already [declare]d, [property] and [arguments] should be equal to those that were originally passed in.
     * The [property] will be checked to make sure it's the same, and the current argument will remain the same.
     * The new [arguments] will be ignored in favor of reusing the existing arguments, under the assumption that they're equal.
     *
     * @throws ParameterizeException if already declared for a different [property].
     * @throws ParameterizeContinue if [arguments] is empty.
     */
    fun <T> declare(property: KProperty<T>, arguments: Iterable<T>) {
        // Nothing to do if already declared (besides validating the property)
        this.property?.let { declaredProperty ->
            if (property.equalsProperty(declaredProperty)) return
            throw ParameterizeException("Expected to be declaring `${declaredProperty.name}`, but got `${property.name}`")
        }

        val iterator = arguments.iterator()
        if (!iterator.hasNext()) {
            throw ParameterizeContinue // Before changing any state
        }

        this.property = property
        this.arguments = arguments
        this.argument = iterator.next()
        this.argumentIterator = iterator.takeIf { it.hasNext() }
    }

    /**
     * Get the current argument, and set [hasBeenUsed] to true.
     *
     * @throws ParameterizeException if already declared for a different [property].
     * @throws IllegalStateException if the argument has not been declared yet.
     */
    fun <T> getArgument(property: KProperty<T>): T {
        val declaredProperty = checkNotNull(this.property) {
            "Cannot get argument before parameter has been declared"
        }

        if (!property.equalsProperty(declaredProperty)) {
            throw ParameterizeException("Cannot use parameter delegate with `${property.name}`, since it was declared with `${declaredProperty.name}`.")
        }

        @Suppress("UNCHECKED_CAST") // Argument is declared with property's arguments, so must be T
        return argument as T
    }

    fun useArgument() {
        hasBeenUsed = true
    }

    /**
     * Iterates the parameter argument.
     *
     * @throws IllegalStateException if the argument has not been declared yet.
     */
    fun nextArgument() {
        val arguments = checkNotNull(arguments) {
            "Cannot iterate arguments before parameter has been declared"
        }

        val iterator = argumentIterator ?: arguments.iterator()

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
