/*
 * Copyright 2024 Ben Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeConfiguration.OnCompleteScope
import com.benwoodworth.parameterize.ParameterizeConfiguration.OnFailureScope
import com.benwoodworth.parameterize.ParameterizeScope.ParameterDelegate
import effekt.Handler
import effekt.HandlerPrompt
import effekt.use
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

internal class ParameterizeState(p: HandlerPrompt<Unit>, val configuration: ParameterizeConfiguration) : Handler<Unit> by p {
    /**
     * The parameters created for [parameterize].
     */
    private val parameters = ArrayList<ParameterState<*>>()

    private var iterationCount = 0L
    private var skipCount = 0L
    private var failureCount = 0L
    private val recordedFailures = mutableListOf<ParameterizeFailure>()
    private var decoratorCoroutine: DecoratorCoroutine? = null

    val isFirstIteration: Boolean
        get() = iterationCount == 0L

    val hasNextArgumentCombination get() = parameters.any { !it.isLast }

    suspend fun <T> declareParameter(
        arguments: Sequence<T>
    ): ParameterDelegate<T> = use { resume ->
        val iterator = arguments.iterator()
        if (!iterator.hasNext()) {
            afterEach()
            return@use
        }
        var isFirstIteration = true
        while (true) {
            iterationCount++
            val argument = iterator.next()
            val isLast = !iterator.hasNext()
            val parameter = ParameterState(argument, isLast)
            if(isFirstIteration) {
                isFirstIteration = false
            } else {
                beforeEach()
            }
            val hasBeenUsed = BooleanArray(parameters.size) {
                parameters[it].hasBeenUsed
            }
            parameters.add(parameter)
            resume(ParameterDelegate(parameter))
            check(parameters.removeLast() == parameter) { "Unexpected last parameter" }
            parameters.forEachIndexed { i, parameter ->
                parameter.hasBeenUsed = hasBeenUsed[i]
            }
            if (isLast) break
        }
    }

    fun handleContinue() {
        skipCount++
    }
    /**
     * Get a list of used arguments for reporting a failure.
     */
    fun getFailureArguments(): List<ParameterizeFailure.Argument<*>> =
        parameters
            .filter { it.hasBeenUsed }
            .map { it.getFailureArgument() }

    @JvmInline
    value class HandleFailureResult(val breakEarly: Boolean)

    fun handleFailure(onFailure: OnFailureScope.(Throwable) -> Unit, failure: Throwable): HandleFailureResult {
        if(failure is ParameterizeException && failure.parameterizeState === this) throw failure
        failureCount++

        val scope = OnFailureScope(
            state = this,
            iterationCount,
            failureCount,
        )

        with(scope) {
            onFailure(failure)

            if (recordFailure) {
                recordedFailures += ParameterizeFailure(failure, arguments)
            }

            return HandleFailureResult(breakEarly)
        }
    }

    fun handleComplete(onComplete: OnCompleteScope.() -> Unit) {
        contract {
            callsInPlace(onComplete, InvocationKind.EXACTLY_ONCE)
        }

        val scope = OnCompleteScope(
            iterationCount,
            skipCount,
            failureCount,
            completedEarly = hasNextArgumentCombination,
            recordedFailures,
        )

        with(scope) {
            onComplete()
        }
    }

    internal fun beforeEach() {
        decoratorCoroutine = DecoratorCoroutine(this, configuration)
            .also { it.beforeIteration() }
    }

    internal fun afterEach() {
        val decoratorCoroutine = checkNotNull(decoratorCoroutine) { "${::decoratorCoroutine.name} was null" }

        decoratorCoroutine.afterIteration()

        this.decoratorCoroutine = null
    }
}
