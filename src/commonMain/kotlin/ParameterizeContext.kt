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
    private var parameterCount = 0

    var hasNextIteration: Boolean = true
        private set

    fun finishIteration() {
        nextArgumentPermutation()

        hasNextIteration = parameterCount > 0
        parameterCount = 0
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

    /**
     * Iterate the last parameter to its next argument,
     * or if all its arguments have been used, remove it and try again.
     */
    private tailrec fun nextArgumentPermutation() {
        if (parameterCount == 0) {
            return
        }

        val lastParameter = parameterDelegates[parameterCount - 1]

        if (!lastParameter.hasBeenRead || lastParameter.isLastArgument) {
            parameterCount--
            lastParameter.reset()
            return nextArgumentPermutation()
        }

        lastParameter.nextArgument()
    }

    fun getReadParameters(): List<Pair<KProperty<*>, *>> =
        parameterDelegates.take(parameterCount)
            .filter { it.hasBeenRead }
            .mapNotNull { it.getPropertyArgumentOrNull() }
}
