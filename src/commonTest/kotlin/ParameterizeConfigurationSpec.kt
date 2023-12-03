package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeConfiguration.Builder
import com.benwoodworth.parameterize.ParameterizeConfigurationSpec.DefaultParameterize
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.test.*

/**
 * Specifies [ParameterizeConfiguration] creation, and how its options are applied to [parameterize].
 *
 * The behavior of the individual options is specified in their respective `ParameterizeConfigurationSpec_*` classes.
 */
class ParameterizeConfigurationSpec {
    private val configurationOptions = listOf(
        ConfigurationOption(ParameterizeConfiguration::onFailure, Builder::onFailure, distinctValue = {}),
        ConfigurationOption(ParameterizeConfiguration::onComplete, Builder::onComplete, distinctValue = {}),
        ConfigurationOption(ParameterizeConfiguration::decorator, Builder::decorator, distinctValue = {}),
    ).map { it.property.name to it }

    private class ConfigurationOption<T>(
        val property: KProperty1<ParameterizeConfiguration, T>,
        val builderProperty: KMutableProperty1<Builder, T>,
        val distinctValue: T
    ) {
        init {
            // In case compiler optimizations break equality assumptions (e.g. shared empty lambda instances or interned strings)
            check(property.name == builderProperty.name) { "Property name mismatch: ${property.name} != ${builderProperty.name}" }
            check(distinctValue != property.get(ParameterizeConfiguration.default)) { "Distinct value is equal to the default: ${property.name} = $distinctValue" }
            check(distinctValue == distinctValue) { "Distinct value can't be checked by equality: ${property.name} = $distinctValue" }
        }

        fun setDistinctValue(builder: Builder): Unit =
            builderProperty.set(builder, distinctValue)
    }


    @Test
    fun builder_should_apply_options_correctly() = testAll(configurationOptions) { option ->
        val configuration = ParameterizeConfiguration {
            option.setDistinctValue(this)

            assertEquals(option.distinctValue, option.builderProperty.get(this), "Builder value after setting")
        }

        assertEquals(option.distinctValue, option.property.get(configuration), "Configuration value after building")
    }

    @Test
    fun builder_should_copy_from_another_configuration_correctly() = testAll(configurationOptions) { option ->
        val copyFrom = ParameterizeConfiguration {
            option.setDistinctValue(this)
        }

        ParameterizeConfiguration(copyFrom) {
            assertEquals(option.distinctValue, option.builderProperty.get(this))
        }
    }

    @Test
    fun string_representation_should_be_class_name_with_options_listed() = testAll(configurationOptions) { testOption ->
        val configuration = ParameterizeConfiguration {
            testOption.setDistinctValue(this)
        }

        val expected = configurationOptions.joinToString(
            prefix = "${ParameterizeConfiguration::class.simpleName}(",
            separator = ", ",
            postfix = ")"
        ) { (_, option) ->
            "${option.property.name}=${option.property.get(configuration)}"
        }

        assertEquals(expected, configuration.toString())
    }

    /**
     * The parameterize block and `decorator` should execute together, since it intuitively works as if the `decorator`
     * logic were inlined into the block. Logic at the end of the block would still be executed before `onFailure` and
     * `onComplete` are invoked.
     *
     * Having `onFailure` run *after* the `decorator` is also easier to reason about if the failure handler itself
     * throws. The `decorator` has "always completes" semantics, and `onFailure` has "terminates immediately" semantics,
     * and that's only possible if `onFailure` runs after. If `onFailure` were to run immediately for a failure in the
     * parameterize block, then the `decorator`'s semantics with how it interacts with failures wouldn't be so
     * straightforward.
     */
    @Test
    fun options_should_be_executed_in_the_correct_order() {
        val order = mutableListOf<String>()

        // Non-builder constructor so all options must be specified
        val configuration = ParameterizeConfiguration(
            onFailure = { order += "onFailure" },
            onComplete = { order += "onComplete" },
            decorator = { iteration ->
                order += "decorator (before)"
                iteration()
                order += "decorator (after)"
            }
        )

        parameterize(configuration) {
            order += "block"
            fail()
        }

        val expectedOrder = listOf(
            "decorator (before)",
            "block",
            "decorator (after)",
            "onFailure",
            "onComplete"
        )

        assertEquals(order, expectedOrder)
    }

    private fun interface ConfiguredParameterize {
        fun configuredParameterize(configure: Builder.() -> Unit, block: ParameterizeScope.() -> Unit)
    }

    private fun testConfiguredParameterize(test: ConfiguredParameterize.() -> Unit) = testAll(
        "with context" to {
            test { configure, block ->
                val context = object : ParameterizeContext {
                    override val parameterizeConfiguration = ParameterizeConfiguration { configure() }
                }

                with(context) {
                    parameterize(block = block)
                }
            }
        },
        "with function parameters" to {
            test { configure, block ->
                val configuration = ParameterizeConfiguration { configure() }

                with(DefaultParameterizeContext) {
                    // Leave arguments unnamed so this call errors when new options are added
                    parameterize(
                        configuration.onFailure,
                        configuration.onComplete,
                        configuration.decorator,
                        block
                    )
                }
            }
        },
        "with function parameters of non-contextual overload" to {
            test { configure, block ->
                val configuration = ParameterizeConfiguration { configure() }

                // Leave arguments unnamed so this call errors when new options are added
                parameterize(
                    configuration.onFailure,
                    configuration.onComplete,
                    configuration.decorator,
                    block
                )
            }
        }
    )

    private fun interface DefaultParameterize {
        fun defaultParameterize(block: ParameterizeScope.() -> Unit)
    }

    private fun testDefaultParameterize(test: DefaultParameterize.() -> Unit) = testAll(
        "with defaults from builder" to {
            val context = object : ParameterizeContext {
                override val parameterizeConfiguration = ParameterizeConfiguration {}
            }

            val defaultParameterize = DefaultParameterize { block ->
                with(context) {
                    parameterize(block = block)
                }
            }

            test(defaultParameterize)
        },
        "with defaults from non-contextual overload" to {
            val defaultParameterize = DefaultParameterize { block ->
                parameterize(block = block)
            }

            test(defaultParameterize)
        },
    )

    @Test
    fun on_failure_configuration_option_should_be_applied() = testConfiguredParameterize {
        var applied = false

        runCatching {
            configuredParameterize({
                onFailure = { applied = true }
            }) {
                fail()
            }
        }

        assertTrue(applied, "applied")
    }

    @Test
    fun on_failure_default_should_record_first_10_failures_and_never_break() = testDefaultParameterize {
        val iterations = List(100) { i ->
            if (i % 2 == 0 && i % 3 == 0) {
                Result.failure(Throwable(i.toString()))
            } else {
                Result.success(Unit)
            }
        }

        val expectedRecordedFailures = iterations
            .mapNotNull { it.exceptionOrNull() }
            .take(10)

        val actualIterations = mutableListOf<Result<Unit>>()

        // Test assumes this is the default type thrown failures
        val failure = assertFailsWith<ParameterizeFailedError> {
            defaultParameterize {
                val iteration by parameter(iterations)

                actualIterations += iteration
                iteration.getOrThrow()
            }
        }

        val actualRecordedFailures = failure.suppressedExceptions
            .map { augmentedFailure -> augmentedFailure.cause }

        assertEquals(iterations, actualIterations, "Should not break")
        assertEquals(expectedRecordedFailures, actualRecordedFailures, "Should record first 10 failures")
    }

    @Test
    fun on_complete_should_be_marked_as_being_called_in_place_exactly_once() {
        // Must be assigned exactly once
        val contextualValue: Any
        val nonContextualValue: Any

        with(DefaultParameterizeContext) {
            parameterize(onComplete = { contextualValue = 0 }) {}
        }
        parameterize(onComplete = { nonContextualValue = 0 }) {}

        // Compile-time test which will error if incorrect
        useParameter(contextualValue)
        useParameter(nonContextualValue)
    }

    @Test
    fun on_complete_configuration_option_should_be_applied() = testConfiguredParameterize {
        var applied = false

        configuredParameterize({
            onComplete = { applied = true }
        }) {
        }

        assertTrue(applied, "applied")
    }

    @Test
    fun on_complete_default_should_throw_ParameterizeFailedError() = testDefaultParameterize {
        assertFailsWith<ParameterizeFailedError> {
            defaultParameterize {
                fail()
            }
        }
    }

    @Test
    fun decorator_configuration_option_should_be_applied() = testConfiguredParameterize {
        var applied = false

        configuredParameterize({
            decorator = { iteration ->
                applied = true
                iteration()
            }
        }) {
        }

        assertTrue(applied, "applied")
    }

    @Test
    fun decorator_default_should_invoke_iteration_function_once() = testDefaultParameterize {
        var iterationInvocations = 0

        defaultParameterize {
            iterationInvocations++
        }

        assertEquals(1, iterationInvocations, "iterationInvocations")
    }
}
