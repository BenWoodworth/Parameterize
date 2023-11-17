package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeConfiguration.OnCompleteScope
import com.benwoodworth.parameterize.ParameterizeConfiguration.OnFailureScope
import com.benwoodworth.parameterize.ParameterizeScope.ParameterDelegate
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KProperty

internal class ParameterizeState {
    /**
     * The parameters created for [parameterize].
     *
     * Parameter instances are re-used between iterations, so will never be removed.
     * The true number of parameters in the current iteration is maintained in [parameterCount].
     */
    private val parameters = ArrayList<ParameterState>()
    private var parameterBeingUsed: KProperty<*>? = null
    private var parameterCount = 0

    private var iterationCount = 0L
    private var failureCount = 0L
    private val recordedFailures = mutableListOf<ParameterizeFailure>()

    private var breakEarly = false
    private var hasNextIteration = true

    /**
     * Starts the next iteration, or returns `false` if there isn't one.
     */
    fun startNextIteration(): Boolean {
        hasNextIteration = iterationCount == 0L || nextArgumentCombinationOrFalse()

        val shouldContinue = hasNextIteration && !breakEarly
        if (shouldContinue) iterationCount++

        return shouldContinue
    }

    fun <T> declareParameter(
        property: KProperty<T>,
        arguments: Iterable<T>
    ): ParameterDelegate<T> {
        parameterBeingUsed?.let {
            throw ParameterizeException("Nesting parameters is not currently supported: `${property.name}` was declared within `${it.name}`'s arguments")
        }

        val parameterIndex = parameterCount

        val parameter = if (parameterIndex in parameters.indices) {
            parameters[parameterIndex]
        } else {
            ParameterState()
                .also { parameters += it }
        }

        property.trackNestedUsage {
            parameter.declare(property, arguments)
            parameterCount++ // After declaring, since the parameter shouldn't count if declare throws
        }

        return ParameterDelegate(parameter, parameter.getArgument(property))
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

    /**
     * Iterate the last parameter that has a next argument (in order of when their arguments were calculated), and reset
     * all parameters that were first used after it (since they may depend on the now changed value, and may be computed
     * differently now that a previous argument changed).
     *
     * Returns `true` if the arguments are at a new combination.
     */
    private fun nextArgumentCombinationOrFalse(): Boolean {
        var iterated = false

        for (parameter in parameters.subList(0, parameterCount).asReversed()) {
            if (!parameter.isLastArgument) {
                parameter.nextArgument()
                iterated = true
                break
            }

            parameter.reset()
        }

        parameterCount = 0

        return iterated
    }

    /**
     * Get a list of used arguments for reporting a failure.
     */
    fun getFailureArguments(): List<ParameterizeFailure.Argument<*>> =
        parameters.take(parameterCount)
            .filter { it.hasBeenUsed }
            .map { it.getFailureArgument() }

    fun handleFailure(onFailure: OnFailureScope.(Throwable) -> Unit, failure: Throwable) {
        failureCount++

        val scope = OnFailureScope(
            state = this,
            iterationCount,
            failureCount,
        )

        with(scope) {
            onFailure(failure)

            this@ParameterizeState.breakEarly = breakEarly

            if (recordFailure) {
                recordedFailures += ParameterizeFailure(failure, arguments)
            }
        }
    }

    fun handleComplete(onComplete: OnCompleteScope.() -> Unit) {
        contract {
            callsInPlace(onComplete, InvocationKind.EXACTLY_ONCE)
        }

        val scope = OnCompleteScope(
            iterationCount,
            failureCount,
            completedEarly = hasNextIteration,
            recordedFailures,
        )

        with(scope) {
            onComplete()
        }
    }
}
