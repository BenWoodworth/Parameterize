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

import com.benwoodworth.parameterize.ParameterizeConfiguration.Builder
import com.benwoodworth.parameterize.ParameterizeConfiguration.OnCompleteScope

/**
 * Thrown to indicate that [parameterize] has completed with failures.
 *
 * The [message] summarizes the number of failures and total iterations. A plus after the total indicates that
 * [completedEarly][OnCompleteScope.completedEarly] was true in [onComplete][Builder.onComplete].
 *
 * The [suppressedExceptions] include [recordedFailures][OnCompleteScope.recordedFailures] from the
 * [onComplete][Builder.onComplete] handler, with each being decorated with a message to include a list of the
 * [arguments][ParameterizeFailure.arguments] that caused it.
 *
 * Can only be constructed from [onComplete][Builder.onComplete].
 */
public expect class ParameterizeFailedError internal constructor(
    recordedFailures: List<ParameterizeFailure>,
    successCount: Long,
    failureCount: Long,
    completedEarly: Boolean
) : AssertionError {
    // TODO: Use context receiver instead of companion + pseudo constructor
    /** @suppress */
    public companion object;

    internal val recordedFailures: List<ParameterizeFailure>
    internal val successCount: Long
    internal val failureCount: Long
    internal val completedEarly: Boolean

    /** @suppress */
    override val message: String
}

private class Failure(
    failure: ParameterizeFailure,
) : AssertionError(failure.failure) {
    init {
        clearStackTrace()
    }

    override val message: String = when (failure.arguments.size) {
        0 -> "Failed with no arguments"

        1 -> failure.arguments.single().let { argument ->
            "Failed with argument:\n\t\t$argument"
        }

        else -> failure.arguments.joinToString(
            prefix = "Failed with arguments:\n\t\t",
            separator = "\n\t\t"
        )
    }
}

internal expect fun Throwable.clearStackTrace()

internal fun ParameterizeFailedError.commonInit() {
    if (recordedFailures.isNotEmpty()) {
        clearStackTrace()
    }

    recordedFailures.forEach { failure ->
        addSuppressed(Failure(failure))
    }
}

internal inline val ParameterizeFailedError.commonMessage
    get() = buildString {
        append("Failed ")
        append(failureCount)
        append('/')
        append(successCount + failureCount)

        if (completedEarly) {
            append('+')
        }

        append(" cases")

        if (recordedFailures.isNotEmpty()) {
            recordedFailures.forEach { failure ->
                append("\n\t")
                append(failure.failure::class.simpleName)
                append(": ")

                val message = failure.failure.message?.trim()
                if (message.isNullOrBlank()) {
                    append("<no message>")
                } else {
                    val firstNewLine = message.indexOfFirst { it == '\n' || it == '\r' }

                    if (firstNewLine == -1) {
                        append(message)
                    } else {
                        append(message, 0, firstNewLine)
                        append(" ...")
                    }
                }

                failure.arguments.forEach { argument ->
                    append("\n\t\t")
                    append(argument)
                }
            }

            if (recordedFailures.size < failureCount) {
                append("\n\t...")
            }
        }
    }
