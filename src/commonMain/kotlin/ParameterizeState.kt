package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

internal class ParameterizeState {
    /**
     * The parameters created for [parameterize].
     *
     * Parameter instances are re-used between iterations, so will never be removed.
     * The true number of parameters in the current iteration is maintained in [parameterCount].
     */
    private val parameterDelegates = ArrayList<ParameterDelegate<Nothing>>()
    private val parameterDelegatesUsed = ArrayList<ParameterDelegate<*>>()
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

        val parameterDelegate = if (parameterIndex in parameterDelegates.indices) {
            parameterDelegates[parameterIndex]
        } else {
            ParameterDelegate<Nothing>()
                .also { parameterDelegates += it }
        }

        parameterDelegate.declare(property, arguments)

        return parameterDelegate
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

    fun <T> getParameterArgument(parameterDelegate: ParameterDelegate<*>, property: KProperty<T>): T {
        val isFirstUse = !parameterDelegate.hasBeenUsed

        return property
            .trackNestedUsage {
                parameterDelegate.getArgument(property)
            }
            .also {
                if (isFirstUse) trackUsedParameter(parameterDelegate)
            }
    }

    private fun trackUsedParameter(parameterDelegate: ParameterDelegate<*>) {
        parameterDelegatesUsed += parameterDelegate

        if (!parameterDelegate.isLastArgument) {
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

        val usedParameterIterator = parameterDelegatesUsed
            .listIterator(parameterDelegatesUsed.lastIndex + 1)

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
            val delegate = parameterDelegates[i]

            if (!delegate.hasBeenUsed) {
                delegate.reset()
            }
        }

        parameterCount = 0
        parameterCountAfterAllUsed = 0

        return iterated
    }

    fun getUsedParameters(): List<Pair<KProperty<*>, *>> =
        parameterDelegates.take(parameterCount)
            .filter { it.hasBeenUsed }
            .mapNotNull { it.getPropertyArgumentOrNull() }
}
