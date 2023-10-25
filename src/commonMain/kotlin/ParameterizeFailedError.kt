package com.benwoodworth.parameterize

/**
 * Thrown from a [parameterize] [onFailure][ParameterizeConfiguration.Builder.onComplete] handler to indicate that it failed with
 * the given [arguments], and the thrown failure as the [cause].
 *
 * Can only be constructed from [ParameterizeConfiguration.Builder.onComplete].
 */
public class ParameterizeFailedError internal constructor(
    internal val recordedFailures: List<ParameterizeFailure>,
    internal val iterationCount: Long,
    internal val failureCount: Long,
) : Error() {
    // TODO: Use context receiver instead of companion + pseudo constructor
    public companion object;

    init {
        clearStackTrace()
    }

    private val arguments = recordedFailures.firstOrNull()?.arguments

    override val message: String = when (arguments?.size) {
        null -> "No recorded failures"

        0 -> "Failed with no arguments"

        1 -> arguments.single().let { argument ->
            "Failed with argument: $argument"
        }

        else -> arguments.joinToString(
            prefix = "Failed with arguments:\n\t",
            separator = "\n\t"
        )
    }
}

internal expect fun Throwable.clearStackTrace()
