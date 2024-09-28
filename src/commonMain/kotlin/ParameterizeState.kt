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
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

internal class ParameterizeState {
    /**
     * The parameters created for [parameterize].
     */
    private val parameters = ArrayList<ParameterState<*>>()

    private var iterationCount = 0L
    private var skipCount = 0L
    private var failureCount = 0L
    private val recordedFailures = mutableListOf<ParameterizeFailure>()

    val isFirstIteration: Boolean
        get() = iterationCount == 0L

    val hasNextArgumentCombination get() = parameters.any { !it.isLast }

    /**
     * Get a list of used arguments for reporting a failure.
     */
    fun getFailureArguments(): List<ParameterizeFailure.Argument<*>> =
        parameters
            .filter { it.hasBeenUsed }
            .map { it.getFailureArgument() }

    @JvmInline
    value class HandleFailureResult(val breakEarly: Boolean)

    fun newIteration() {
        iterationCount++
    }

    fun handleContinue() {
        skipCount++
    }

    inline fun <T> withParameter(parameter: ParameterState<T>, block: () -> Unit) {
        parameters.add(parameter)
        block()
        check(parameters.removeLast() == parameter) { "Unexpected last parameter" }
    }

    inline fun preservingHasBeenUsed(block: () -> Unit) {
        val hasBeenUsed = BooleanArray(parameters.size) {
            parameters[it].hasBeenUsed
        }
        block()
        parameters.forEachIndexed { index, parameter ->
            parameter.hasBeenUsed = hasBeenUsed[index]
        }
    }

    fun handleFailure(onFailure: OnFailureScope.(Throwable) -> Unit, failure: Throwable): HandleFailureResult {
        if (failure is ParameterizeException && failure.parameterizeState === this) throw failure
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
}
