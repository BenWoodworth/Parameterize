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

internal class ParameterizeState(p: HandlerPrompt<Unit>) : Handler<Unit> by p {
    /**
     * The parameters created for [parameterize].
     */
    private val parameters = ArrayList<ParameterState<*>>()

    private var iterationCount = 0L
    private var failureCount = 0L
    private val recordedFailures = mutableListOf<ParameterizeFailure>()

    val isFirstIteration: Boolean
        get() = iterationCount == 1L

    suspend fun <T> declareParameter(
        arguments: Sequence<T>
    ): ParameterDelegate<T> = use { resume ->
        // TODO skip calling decorator on first iteration, but call it on the rest
        //  and also call it for the top-most iteration.
        val iterator = arguments.iterator()
        if (!iterator.hasNext()) return@use
        while (true) {
            iterationCount++
            val argument = iterator.next()
            val isLast = !iterator.hasNext()
            val parameter = ParameterState(sequenceOf(argument), isLast)
            parameters.add(parameter)
            resume(ParameterDelegate(parameter, argument))
            check(parameters.removeLast() == parameter) { "Unexpected last parameter" }
            if (isLast) break
        }
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
            failureCount,
            completedEarly = parameters.any { !it.isLast },
            recordedFailures,
        )

        with(scope) {
            onComplete()
        }
    }
}
