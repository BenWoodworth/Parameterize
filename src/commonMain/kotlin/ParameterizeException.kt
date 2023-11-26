package com.benwoodworth.parameterize

import kotlin.contracts.contract

internal class ParameterizeException(
    internal val parameterizeState: ParameterizeState,
    override val message: String,
    override val cause: Throwable? = null
) : Exception()

/**
 * Throws a [ParameterizeException] if the [value] is false.
 */
internal inline fun ParameterizeState.checkState(
    value: Boolean,
    cause: Throwable? = null,
    lazyMessage: () -> String
) {
    contract {
        returns() implies value
    }

    if (!value) {
        throw ParameterizeException(this, lazyMessage(), cause)
    }
}
