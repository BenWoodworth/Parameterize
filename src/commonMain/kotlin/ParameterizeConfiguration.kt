package com.benwoodworth.parameterize

import kotlin.native.concurrent.ThreadLocal

public class ParameterizeConfiguration private constructor(
    public val throwHandler: ParameterizeThrowHandler,
) {
    @ThreadLocal
    public companion object {
        public operator fun invoke(
            from: ParameterizeConfiguration = default,
            builderAction: Builder.() -> Unit,
        ): ParameterizeConfiguration =
            Builder(from).apply(builderAction).build()

        public var default: ParameterizeConfiguration = ParameterizeConfiguration(
            throwHandler = { throw ParameterizeFailedError() },
        )
    }

    public class Builder internal constructor(from: ParameterizeConfiguration) {
        /**
         * Handles a failure during an iteration.
         *
         * If the throw handler returns, then [parameterize] will continue to the next iteration.
         * Throwing from the handler will end the execution, propagating out to the caller.
         *
         * Throws a [ParameterizeFailedError] by default.
         */
        public var throwHandler: ParameterizeThrowHandler = from.throwHandler


        internal fun build(): ParameterizeConfiguration = ParameterizeConfiguration(
            throwHandler = throwHandler,
        )
    }
}
