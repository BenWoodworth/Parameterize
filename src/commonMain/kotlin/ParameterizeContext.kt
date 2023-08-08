package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

internal class ParameterizeContext {
    /**
     * The parameters created for [parameterize].
     *
     * Parameter instances are re-used between iterations, so will never be removed.
     * The true number of parameters in the current iteration is maintained in [parameterCount].
     */
    private val parameters = ArrayList<Parameter<*>>()
    private var parameterCount = 0

    var hasNextIteration: Boolean = true
        private set

    fun finishIteration() {
        nextArgumentPermutation()
        hasNextIteration = parameterCount > 0

        for (i in 0..<parameterCount) {
            parameters[i].hasBeenRead = false
        }

        parameterCount = 0
    }

    fun <T> declareParameter(arguments: Iterable<T>): Parameter<T> {
        val parameterIndex = parameterCount++

        val parameter = if (parameterIndex in parameters.indices) {
            @Suppress("UNCHECKED_CAST")
            parameters[parameterIndex] as Parameter<T>
        } else {
            Parameter<T>(this)
                .also { parameters += it }
        }

        if (!parameter.isDeclared) {
            parameter.declare(arguments)
        }

        return parameter
    }

    /**
     * Iterate the last parameter to its next argument,
     * or if all its arguments have been used, remove it and try again.
     */
    private tailrec fun nextArgumentPermutation() {
        if (parameterCount == 0) {
            return
        }

        val lastParameter = parameters[parameterCount - 1]

        if (!lastParameter.isInitialized) {
            parameterCount--
            return nextArgumentPermutation()
        }

        if (lastParameter.isLastArgument) {
            parameterCount--
            lastParameter.reset()
            return nextArgumentPermutation()
        }

        lastParameter.nextArgument()
    }

    fun getReadParameters(): List<Pair<KProperty<*>, *>> =
        parameters.take(parameterCount)
            .filter { it.isInitialized }
            .mapNotNull { it.getPropertyArgumentOrNull() }
}
