package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeConfiguration.Builder
import kotlin.native.concurrent.ThreadLocal

/**
 * Reusable [parameterize] configuration options.
 *
 * Can be used with [ParameterizeContext] to provide defaults for all [parameterize] calls in scope.
 */
public class ParameterizeConfiguration private constructor(
    /** @see Builder.onFailure */
    public val onFailure: OnFailureScope.(failure: Throwable) -> Unit,

    /** @see Builder.onComplete */
    public val onComplete: OnCompleteScope.() -> Unit,
) {
    /** @suppress */
    @ThreadLocal
    public companion object {
        internal val default: ParameterizeConfiguration = Builder(null).build()

        public operator fun invoke(
            from: ParameterizeConfiguration = default,
            builderAction: Builder.() -> Unit,
        ): ParameterizeConfiguration =
            Builder(from).apply(builderAction).build()
    }

    /** @suppress */
    override fun toString(): String =
        "ParameterizeConfiguration(" +
            "onFailure=$onFailure, " +
            "onComplete=$onComplete)"

    /** @see ParameterizeConfiguration */
    public class Builder internal constructor(from: ParameterizeConfiguration?) {
        /**
         * Invoked after each failing iteration to configure how a failure should be handled.
         *
         * ***Note:*** Throwing here will immediately abort [parameterize] without [onComplete] being executed, which
         * may be desirable if failures aren't expected.
         *
         * Defaults to:
         * ```
         * onFailure = { failure ->
         *     // Only record the first 10 failures
         *     recordFailure = failureCount <= 10
         * }
         * ```
         *
         * @see OnFailureScope.breakEarly
         * @see OnFailureScope.recordFailure
         */
        public var onFailure: OnFailureScope.(failure: Throwable) -> Unit = from?.onFailure
            ?: {
                recordFailure = failureCount <= 10
            }

        /**
         * Invoked after [parameterize] has finished iterating, enabling the final results to be handled.
         *
         * Defaults to:
         * ```
         * onComplete = {
         *     if (failureCount > 0) throw ParameterizeFailedError()
         * }
         * ```
         */
        public var onComplete: OnCompleteScope.() -> Unit = from?.onComplete
            ?: {
                if (failureCount > 0) throw ParameterizeFailedError()
            }

        internal fun build(): ParameterizeConfiguration = ParameterizeConfiguration(
            onFailure = onFailure,
            onComplete = onComplete,
        )
    }

    /** @see Builder.onFailure */
    public class OnFailureScope internal constructor(
        private val state: ParameterizeState,

        /**
         * The number of iterations that have been executed, including this failing iteration.
         */
        public val iterationCount: Long,

        /**
         * The number of failures that have occurred, including this failure.
         */
        public val failureCount: Long
    ) {
        /**
         * The parameter arguments that resulted in the failure.
         */
        public val arguments: List<ParameterizeFailure.Argument<*>> by lazy {
            state.getFailureArguments()
        }

        /**
         * Specifies whether [parameterize] should break and complete early, or continue to the next iteration.
         *
         * Defaults to `false`.
         */
        public var breakEarly: Boolean = false

        /**
         * Specifies whether this iteration's [arguments] and the resulting failure should be added to the list of
         * [recordedFailures][OnCompleteScope.recordedFailures] provided to [ParameterizeConfiguration.onComplete].
         *
         * Defaults to `false`.
         */
        public var recordFailure: Boolean = false
    }

    /** @see Builder.onComplete */
    public class OnCompleteScope internal constructor(
        /**
         * The total number of iterations completed.
         */
        public val iterationCount: Long,

        /**
         * The number of failing iterations.
         */
        public val failureCount: Long,

        /**
         * True if [parameterize] completed with more combinations of arguments left to iterate, because of
         * [onFailure]'s [breakEarly][OnFailureScope.breakEarly] being set.
         *
         * ***Note:*** Setting [breakEarly][OnFailureScope.breakEarly] on the last iteration will make this property
         * true, since all combinations have been iterated through.
         */
        public val completedEarly: Boolean,

        /** @see OnFailureScope.recordFailure */
        public val recordedFailures: List<ParameterizeFailure>
    ) {
        /** @see ParameterizeFailedError */
        public operator fun ParameterizeFailedError.Companion.invoke(): ParameterizeFailedError =
            ParameterizeFailedError(recordedFailures, failureCount, iterationCount, completedEarly)
    }
}
