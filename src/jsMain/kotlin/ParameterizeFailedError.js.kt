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
    // The `stack` property is non-standard, but supported on all major browsers/servers:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error/stack#browser_compatibility
    this.asDynamic().stack = null
}
