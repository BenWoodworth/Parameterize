package com.benwoodworth.parameterize

/**
 * Thrown to indicate that [parameterize] has completed with failures.
 *
 * The [message] summarizes the number of failures and total iterations. A plus after the total indicates that
 * [completedEarly][ParameterizeConfiguration.OnCompleteScope.completedEarly] was true in
 * [onComplete][ParameterizeConfiguration.Builder.onComplete].
 *
 * The [suppressedExceptions] include [recordedFailures][ParameterizeConfiguration.OnCompleteScope.recordedFailures]
 * from the [onComplete][ParameterizeConfiguration.Builder.onComplete] handler, with each being decorated with a message to
 * include a list of the [arguments][ParameterizeFailure.arguments] that caused it.
 *
 * Can only be constructed from [ParameterizeConfiguration.Builder.onComplete].
 */
public class ParameterizeFailedError internal constructor(
    internal val recordedFailures: List<ParameterizeFailure>,
    internal val failureCount: Long,
    internal val iterationCount: Long,
    internal val completedEarly: Boolean
) : AssertionError() {
    // TODO: Use context receiver instead of companion + pseudo constructor
    public companion object;

    init {
        if (recordedFailures.isNotEmpty()) {
            clearStackTrace()
        }

        recordedFailures.forEach { failure ->
            addSuppressed(Failure(failure))
        }
    }

    override val message: String = buildString {
        append("Failed ")
        append(failureCount)
        append('/')
        append(iterationCount)

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
            }

            if (recordedFailures.size < failureCount) {
                append("\n\t...")
            }
        }
    }
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
