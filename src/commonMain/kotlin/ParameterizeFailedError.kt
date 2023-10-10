package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

/**
 * Thrown from a parameterize [throw handler][ParameterizeConfiguration.throwHandler] to indicate that [parameterize]
 * failed with the given [arguments], and the thrown failure as the [cause].
 *
 * Can only be constructed within a [ParameterizeThrowHandlerScope].
 */
public class ParameterizeFailedError internal constructor(
    public val arguments: List<Pair<KProperty<*>, *>>,
    override val cause: Throwable
) : Error() {
    // TODO: Use context receiver instead of companion + pseudo constructor
    public companion object;

    init {
        clearStackTrace()
    }

    override val message: String = when (arguments.size) {
        0 -> "Failed with no arguments"

        1 -> arguments.single().let { (parameter, argument) ->
            "Failed with argument: ${parameter.name} = $argument"
        }

        else -> buildString {
            append("Failed with arguments:")
            arguments.forEach { (parameter, argument) ->
                append("\n\t")
                append(parameter.name)
                append(" = ")
                append(argument)
            }
        }
    }
}

internal expect fun Throwable.clearStackTrace()
