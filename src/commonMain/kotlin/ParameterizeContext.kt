package com.benwoodworth.parameterize

/**
 * Provides configuration defaults to [parameterize] when it is called within this scope.
 *
 * For classes that need to provide different defaults, it is enough to implement this interface and override
 * [parameterizeConfiguration]. With that, [parameterize] calls will automatically pick up the configuration for its
 * default arguments.
 *
 * ```
 * class MySpec : ParameterizeContext {
 *     // Provide the configuration...
 *     override val parameterizeConfiguration = ParameterizeConfiguration {
 *         // ...
 *     }
 *
 *     // and `parameterize` will automatically pick it up for its default arguments
 *     @Test
 *     fun my_parameterized_test() = parameterize {
 *         // ...
 *     }
 * }
 * ```
 *
 * If the same configuration needs to be used in multiple classes, then it can be cleanly reused by taking advantage of
 * interface defaults. For example, define an interface like `TestingContext` with [parameterizeConfiguration]
 * already overridden, then on the classes that should have that same configuration pulled in, simply implement
 * `TestingContext` instead of [ParameterizeContext]:
 * ```
 * // Define the shared configuration
 * private val testingParameterizeConfiguration = ParameterizeConfiguration {
 *     // ...
 * }
 *
 * // Then configure a context that is easy to tack onto classes for reuse
 * interface TestingContext : ParameterizeContext {
 *     override val parameterizeConfiguration: ParameterizeConfiguration
 *         get() = testingParameterizeConfiguration
 *
 *     // ...plus any other contexts that might need to be shared
 * }
 * ```
 *
 * **Note:** Calling the fully-qualified [com.benwoodworth.parameterize.parameterize] function will invoke
 * `parameterize` without it receiving the [ParameterizeContext], using the default context/configuration instead. This
 * will be fixed in the future when
 * [context receivers](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md) are stable.
 *
 * Additionally, if the proposed
 * [scope properties](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md#scope-properties)
 * make it into the language, then it will be possible to bring [ParameterizeContext] into scope without having to
 * implement it in the way that was described here.
 */
public interface ParameterizeContext {
    /** @see ParameterizeConfiguration */
    public val parameterizeConfiguration: ParameterizeConfiguration
}

@PublishedApi
internal data object DefaultParameterizeContext : ParameterizeContext {
    override val parameterizeConfiguration: ParameterizeConfiguration
        get() = ParameterizeConfiguration.default
}
