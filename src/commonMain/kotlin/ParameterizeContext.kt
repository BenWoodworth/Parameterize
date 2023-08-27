package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

internal class ParameterizeContext {
    /**
     * The parameters created for [parameterize].
     *
     * Parameter instances are re-used between iterations, so will never be removed.
     * The true number of parameters in the current iteration is maintained in [parameterCount].
     */
    private val parameterDelegates = ArrayList<ParameterDelegate<Nothing>>()
    private val parameterDelegatesUsed = ArrayList<ParameterDelegate<*>>()

    private var parameterCount = 0
    private var parameterCountAfterAllUsed = 0

    var hasNextIteration: Boolean = true
        private set

    fun finishIteration() {
        hasNextIteration = nextArgumentPermutationOrFalse()

        parameterDelegatesUsed.clear()
        parameterCount = 0
        parameterCountAfterAllUsed = 0
    }

    fun <T> declareParameter(property: KProperty<T>, arguments: Iterable<T>): ParameterDelegate<Nothing> {
        val parameterIndex = parameterCount++

        val parameterDelegate = if (parameterIndex in parameterDelegates.indices) {
            parameterDelegates[parameterIndex]
        } else {
            ParameterDelegate<Nothing>()
                .also { parameterDelegates += it }
        }

        parameterDelegate.declare(property, arguments)

        return parameterDelegate
    }

    fun <T> readParameter(parameterDelegate: ParameterDelegate<*>, property: KProperty<T>): T {
        val isFirstRead = !parameterDelegate.hasBeenRead

        return parameterDelegate.readArgument(property)
            .also {
                if (isFirstRead) trackUsedParameter(parameterDelegate)
            }
    }

    private fun trackUsedParameter(parameterDelegate: ParameterDelegate<*>) {
        parameterDelegatesUsed += parameterDelegate

        if (!parameterDelegate.isLastArgument) {
            parameterCountAfterAllUsed = parameterCount
        }
    }

    /**
     * Iterate the last parameter (by first read this iteration) that has a next
     * argument, and reset all parameters that were first read after it (since
     * they may depend on the now changed value, and may be calculated
     * differently).
     *
     * Returns `true` if the arguments are at a new permutation.
     */
    private fun nextArgumentPermutationOrFalse(): Boolean {
        var iterated = false

        for (parameter in parameterDelegatesUsed.asReversed()) {
            if (!parameter.isLastArgument) {
                parameter.nextArgument()
                iterated = true
                break
            }

            parameter.reset()
        }

        for (i in parameterCountAfterAllUsed..<parameterCount) {
            val delegate = parameterDelegates[i]

            if (!delegate.hasBeenRead) {
                delegate.reset()
            }
        }

        return iterated
    }

    fun getReadParameters(): List<Pair<KProperty<*>, *>> =
        parameterDelegates.take(parameterCount)
            .filter { it.hasBeenRead }
            .mapNotNull { it.getPropertyArgumentOrNull() }
}
