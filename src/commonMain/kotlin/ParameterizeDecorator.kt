package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeConfiguration.DecoratorScope
import com.benwoodworth.parameterize.ParameterizeScope.ParameterDelegate
import effekt.Handler
import effekt.HandlerPrompt
import effekt.handle
import effekt.use
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

internal class ParameterizeDecorator(
    internal val parameterizeState: ParameterizeState,
    private val decorator: suspend DecoratorScope.(iteration: suspend DecoratorScope.() -> Unit) -> Unit,
    p: HandlerPrompt<Unit>
) : Handler<Unit> by p {
    private var decoratorCoroutine: DecoratorCoroutine? = null

    internal fun beforeEach() {
        check(decoratorCoroutine == null) { "${::decoratorCoroutine.name} was improperly finished" }
        decoratorCoroutine = DecoratorCoroutine(parameterizeState, decorator)
            .also { it.beforeIteration() }
    }

    internal fun afterEach() {
        decoratorCoroutine?.afterIteration() ?: error("${::decoratorCoroutine.name} was null")
        decoratorCoroutine = null
    }

    suspend fun <T> declareParameter(
        arguments: Sequence<T>
    ): ParameterDelegate<T> = use { resume ->
        arguments.forEachWithIterations(onEmpty = {
            afterEach()
            return@use
        }) { isFirst, isLast, argument ->
            parameterizeState.newIteration()
            if (!isFirst) beforeEach()

            val parameter = ParameterState(argument, isLast)
            parameterizeState.preservingHasBeenUsed {
                parameterizeState.withParameter(parameter) {
                    resume(ParameterDelegate(parameter))
                }
            }
        }
    }
}

private inline fun <T> Sequence<T>.forEachWithIterations(
    onEmpty: () -> Unit,
    block: (isFirst: Boolean, isLast: Boolean, T) -> Unit
) {
    val iterator = iterator()
    if (!iterator.hasNext()) {
        onEmpty()
        return
    }
    var isFirstIteration = true
    var isLastIteration: Boolean
    do {
        val element = iterator.next()
        isLastIteration = !iterator.hasNext()
        block(isFirstIteration, isLastIteration, element)
        if (isFirstIteration) {
            isFirstIteration = false
        }
    } while (!isLastIteration)
}

internal suspend fun ParameterizeState.withDecorator(
    decorator: suspend DecoratorScope.(iteration: suspend DecoratorScope.() -> Unit) -> Unit,
    onFailure: suspend (Throwable) -> Unit,
    block: suspend ParameterizeScope.() -> Unit,
): Unit = handle {
    val decorator = ParameterizeDecorator(this@withDecorator, decorator, this)
    decorator.beforeEach()
    val result = runCatching { block(ParameterizeScope(decorator)) }
    decorator.afterEach()
    result.onFailure { onFailure(it) }
}

/**
 * The [decorator][ParameterizeConfiguration.decorator] suspends for the iteration so that the one lambda can be run as
 * two separate parts, without needing to wrap the (inlined) [parameterize] block.
 */
private class DecoratorCoroutine(
    private val parameterizeState: ParameterizeState,
    private val decorator: suspend DecoratorScope.(iteration: suspend DecoratorScope.() -> Unit) -> Unit
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

        val invokeDecorator: suspend DecoratorScope.() -> Unit = {
            decorator(this, iteration)
        }

        invokeDecorator
            .createCoroutineUnintercepted(
                receiver = scope,
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
