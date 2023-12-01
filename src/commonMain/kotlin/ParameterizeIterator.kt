package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeConfiguration.DecoratorScope
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

internal data object ParameterizeContinue : Throwable()

@PublishedApi
internal class ParameterizeIterator(
    private val configuration: ParameterizeConfiguration
) {
    private val parameterizeState = ParameterizeState()

    private var breakEarly = false
    private var currentIterationScope: ParameterizeScope? = null
    private var decoratorCoroutine: DecoratorCoroutine? = null

    /**
     * Signals the start of a new [parameterize] iteration, and returns its scope if there is one.
     */
    @PublishedApi
    internal fun nextIteration(): ParameterizeScope? {
        currentIterationScope?.let { afterEach(it) }

        if (breakEarly || !parameterizeState.hasNextArgumentCombination) {
            handleComplete()
            return null
        }

        parameterizeState.startNextIteration()
        return ParameterizeScope(parameterizeState).also {
            currentIterationScope = it
            beforeEach()
        }
    }

    @PublishedApi
    internal fun handleFailure(failure: Throwable): Unit = when {
        failure is ParameterizeContinue -> {}
        failure is ParameterizeException && failure.parameterizeState === parameterizeState -> throw failure
        else -> {
            val result = parameterizeState.handleFailure(configuration.onFailure, failure)
            breakEarly = result.breakEarly
        }
    }

    private fun beforeEach() {
        decoratorCoroutine = DecoratorCoroutine(parameterizeState, configuration)
            .also { it.beforeIteration() }
    }

    private fun afterEach(scope: ParameterizeScope) {
        decoratorCoroutine?.afterIteration()
            ?: error("Decorator continuation was null")

        scope.iterationCompleted = true
    }

    private fun handleComplete() {
        parameterizeState.handleComplete(configuration.onComplete)
    }
}

/**
 * The [decorator][ParameterizeConfiguration.decorator] suspends for the iteration so that the one lambda can be run as
 * two separate parts, without needing to wrap the (inlined) [parameterize] block.
 */
private class DecoratorCoroutine(
    private val parameterizeState: ParameterizeState,
    private val configuration: ParameterizeConfiguration
) {
    private val scope = DecoratorScope(parameterizeState)

    private var continueAfterIteration: Continuation<Unit>? = null
    private var completed = false

    private val iteration: suspend DecoratorScope.() -> Unit = {
        parameterizeState.checkState(continueAfterIteration == null) {
            "Decorator must invoke the iteration function exactly once, but was invoked twice"
        }

        suspendDecorator { continueAfterIteration = it }
        isLastIteration = !parameterizeState.hasNextArgumentCombination
    }

    fun beforeIteration() {
        check(!completed) { "Decorator already completed" }

        suspend { configuration.decorator(scope, iteration) }
            .createCoroutineUnintercepted(
                completion = Continuation(EmptyCoroutineContext) {
                    completed = true
                    it.getOrThrow()
                }
            )
            .resume(Unit)

        parameterizeState.checkState(continueAfterIteration != null) {
            if (completed) {
                "Decorator must invoke the iteration function exactly once, but was not invoked"
            } else {
                "Decorator suspended unexpectedly"
            }
        }
    }

    fun afterIteration() {
        check(!completed) { "Decorator already completed" }

        continueAfterIteration?.resume(Unit)
            ?: error("Iteration not invoked")

        parameterizeState.checkState(completed) {
            "Decorator suspended unexpectedly"
        }
    }
}
