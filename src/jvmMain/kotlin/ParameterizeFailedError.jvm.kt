package com.benwoodworth.parameterize

import org.opentest4j.MultipleFailuresError

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-20641/
public actual class ParameterizeFailedError internal actual constructor(
    internal actual val recordedFailures: List<ParameterizeFailure>,
    internal actual val failureCount: Long,
    internal actual val iterationCount: Long,
    internal actual val completedEarly: Boolean
) : MultipleFailuresError(null, emptyList()) {
    public actual companion object;

    init {
        commonInit()
    }

    public actual override val message: String
        get() = commonMessage

    // Usually won't be used, so compute as needed (instead of always providing it to `MultipleFailuresError` up front)
    @Deprecated("Exists for MultipleFailuresError tooling, and is not API", level = DeprecationLevel.HIDDEN)
    override fun getFailures(): List<Throwable> =
        recordedFailures.map { it.failure }

    @Deprecated("Exists for MultipleFailuresError tooling, and is not API", level = DeprecationLevel.HIDDEN)
    override fun hasFailures(): Boolean =
        recordedFailures.isNotEmpty()
}

internal actual fun Throwable.clearStackTrace() {
    stackTrace = emptyArray()
}
