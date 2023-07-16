package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

private sealed interface ParameterState<T> {
    class Uninitialized<T>(
        val arguments: Iterable<T>
    ) : ParameterState<T>

    class Initialized<T>(
        val variable: KProperty<*>,
        val argumentIterator: Iterator<T>,
        var argument: T
    ) : ParameterState<T> {
        fun nextArgument() {
            argument = argumentIterator.next()
        }
    }
}

internal class ParameterizeContext {
    private val parameterCache = ArrayList<Parameter<*>>()
    private val parameterStates = ArrayList<ParameterState<*>>()
    private var nextParameterIndex = 0

    var hasNextIteration: Boolean = true
        private set

    fun finishIteration() {
        nextParameterIndex = 0
        nextArgumentPermutation()

        hasNextIteration = parameterStates.isNotEmpty()
    }

    fun <T> createParameter(arguments: Iterable<T>): Parameter<T> {
        val parameter = getParameterFromCache(nextParameterIndex++)

        if (parameter.index > parameterStates.lastIndex) {
            parameterStates += ParameterState.Uninitialized(arguments)
        }

        @Suppress("UNCHECKED_CAST")
        return parameter as Parameter<T>
    }

    private fun getParameterFromCache(index: Int): Parameter<*> =
        if (index <= parameterCache.lastIndex) {
            parameterCache[index]
        } else {
            Parameter<Any?>(this, index)
                .also { parameterCache.add(it) }
        }

    private fun <T> getParameterState(parameter: Parameter<T>): ParameterState<T> =
        @Suppress("UNCHECKED_CAST")
        (parameterStates[parameter.index] as ParameterState<T>)

    fun <T> getParameterArgument(variable: KProperty<*>, parameter: Parameter<T>): T =
        when (val state = getParameterState(parameter)) {
            is ParameterState.Initialized -> {
                state.argument
            }

            is ParameterState.Uninitialized -> {
                val iterator = state.arguments.iterator()
                val initialized = ParameterState.Initialized(variable, iterator, iterator.next())

                parameterStates[parameter.index] = initialized
                initialized.argument
            }
        }

    fun getParameterVariableOrNull(parameter: Parameter<*>): KProperty<*>? =
        (parameterStates.getOrNull(parameter.index) as? ParameterState.Initialized)?.variable

    /**
     * Iterate the last parameter to its next argument,
     * or if all its arguments have been used, remove it and try again.
     */
    private tailrec fun nextArgumentPermutation(): Unit =
        when (val lastParameter = parameterStates.lastOrNull()) {
            is ParameterState.Initialized -> {
                if (lastParameter.argumentIterator.hasNext()) {
                    lastParameter.nextArgument()
                } else {
                    parameterStates.removeLast()
                    nextArgumentPermutation()
                }
            }

            is ParameterState.Uninitialized -> {
                parameterStates.removeLast()
                nextArgumentPermutation()
            }

            null -> Unit
        }
}
