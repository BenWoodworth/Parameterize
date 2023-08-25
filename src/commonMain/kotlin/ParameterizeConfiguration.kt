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
            throwHandler = { thrown ->
                println("Failed with arguments:" + parameters.joinToString { "\n  ${it.parameter.name} = ${it.argument}" })
                throw thrown
            },
        )
    }

    public class Builder internal constructor(from: ParameterizeConfiguration) {
        /**
         * Handles a failure during an iteration.
         *
         * If the throw handler returns, then [parameterize] will continue to the next iteration.
         * Throwing from the handler end the execution and propagate out to the caller.
         *
         * By default, prints "Failed with arguments: ..." and re-throws the same instance.
         */
        public var throwHandler: ParameterizeThrowHandler = from.throwHandler


        internal fun build(): ParameterizeConfiguration = ParameterizeConfiguration(
            throwHandler = throwHandler,
        )
    }
}
