package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

internal class ParameterizeState {
    /**
     * The parameters created for [parameterize].
     *
     * Parameter instances are re-used between iterations, so will never be removed.
     * The true number of parameters in the current iteration is maintained in [parameterCount].
     */
    private val parameters = ArrayList<ParameterDelegate<Nothing>>()
    private val parametersUsed = ArrayList<ParameterDelegate<*>>()
    private var parameterBeingUsed: KProperty<*>? = null

    private var parameterCount = 0
    private var parameterCountAfterAllUsed = 0

    private var isFirstIteration: Boolean = true

    /**
     * Starts the next iteration, or returns `false` if there isn't one.
     */
    fun startNextIteration(): Boolean =
        if (isFirstIteration) {
            isFirstIteration = false
            true
        } else {
            nextArgumentPermutationOrFalse()
        }

    fun <T> declareParameter(property: KProperty<T>, arguments: Iterable<T>): ParameterDelegate<Nothing> {
        parameterBeingUsed.let {
            if (it != null) throw ParameterizeException("Nesting parameters is not currently supported: `${property.name}` was declared within `${it.name}`'s arguments")
        }

        val parameterIndex = parameterCount++

        val parameter = if (parameterIndex in parameters.indices) {
            parameters[parameterIndex]
        } else {
            ParameterDelegate<Nothing>()
                .also { parameters += it }
        }

        parameter.declare(property, arguments)

        return parameter
    }

    private inline fun <T> KProperty<T>.trackNestedUsage(block: () -> T): T {
        val previousParameterBeingUsed = parameterBeingUsed
        parameterBeingUsed = this

        try {
            return block()
        } finally {
            parameterBeingUsed = previousParameterBeingUsed
        }
    }

    fun <T> getParameterArgument(parameter: ParameterDelegate<*>, property: KProperty<T>): T {
        val isFirstUse = !parameter.hasBeenUsed

        return property
            .trackNestedUsage {
                parameter.getArgument(property)
            }
            .also {
                if (isFirstUse) trackUsedParameter(parameter)
            }
    }

    private fun trackUsedParameter(parameter: ParameterDelegate<*>) {
        parametersUsed += parameter

        if (!parameter.isLastArgument) {
            parameterCountAfterAllUsed = parameterCount
        }
    }

    /**
     * Iterate the last parameter (by the order they're first used) that has a
     * next argument, and reset all parameters that were first used after it
     * (since they may depend on the now changed value, and may be computed
     * differently).
     *
     * Returns `true` if the arguments are at a new permutation.
     */
    private fun nextArgumentPermutationOrFalse(): Boolean {
        var iterated = false

        val usedParameterIterator = parametersUsed
            .listIterator(parametersUsed.lastIndex + 1)

        while (usedParameterIterator.hasPrevious()) {
            val parameter = usedParameterIterator.previous()

            if (!parameter.isLastArgument) {
                parameter.nextArgument()
                iterated = true
                break
            }

            usedParameterIterator.remove()
            parameter.reset()
        }

        for (i in parameterCountAfterAllUsed..<parameterCount) {
            val parameter = parameters[i]

            if (!parameter.hasBeenUsed) {
                parameter.reset()
            }
        }

        parameterCount = 0
        parameterCountAfterAllUsed = 0

        return iterated
    }

    fun getUsedArguments(): List<ParameterizeArgument<*>> =
        parameters.take(parameterCount)
            .filter { it.hasBeenUsed }
            .mapNotNull { it.getParameterizeArgumentOrNull() }
}
