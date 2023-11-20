package com.benwoodworth.parameterize

public actual class ParameterizeFailedError internal actual constructor(
    internal actual val recordedFailures: List<ParameterizeFailure>,
    internal actual val failureCount: Long,
    internal actual val iterationCount: Long,
    internal actual val completedEarly: Boolean
) : AssertionError() {
    public actual companion object;

    init {
        commonInit()
    }

    public actual override val message: String
        get() = commonMessage
}

internal actual fun Throwable.clearStackTrace() {
    // Currently not possible on native: https://youtrack.jetbrains.com/issue/KT-59017/
}
