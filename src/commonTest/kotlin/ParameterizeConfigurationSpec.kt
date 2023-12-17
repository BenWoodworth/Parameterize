package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeConfiguration.Builder
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.test.*

/**
 * Specifies [ParameterizeConfiguration] creation, and how its options are applied to [parameterize].
 *
 * The behavior of the individual options is specified in their respective `ParameterizeConfigurationSpec_*` classes.
 */
class ParameterizeConfigurationSpec {
    // Keep options sorted by the order they're executed, where relevant
    private val configurationOptions = listOf(
        ConfigurationOption(ParameterizeConfiguration::decorator, Builder::decorator, distinctValue = {}),
        ConfigurationOption(ParameterizeConfiguration::onFailure, Builder::onFailure, distinctValue = {}),
        ConfigurationOption(ParameterizeConfiguration::onComplete, Builder::onComplete, distinctValue = {}),
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
            decorator = { iteration ->
                order += "decorator (before)"
                iteration()
                order += "decorator (after)"
            },
            onFailure = { order += "onFailure" },
            onComplete = { order += "onComplete" }
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
                        configuration.decorator,
                        configuration.onFailure,
                        configuration.onComplete,
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
                    configuration.decorator,
                    configuration.onFailure,
                    configuration.onComplete,
                    block
                )
            }
        },
        "with a provided configuration" to {
            test { configure, block ->
                val configuration = ParameterizeConfiguration { configure() }
                parameterize(configuration, block)
            }
        }
    )

    private fun interface ParameterizeWithOptionDefault {
        fun parameterizeWithOptionDefault(block: ParameterizeScope.() -> Unit)
    }

    /**
     * Test a configuration option's default behavior.
     *
     * If it's not possible to test without changing another option from its default, [configureWithDefault] and
     * [parameterizeWithDefault] can be used to change ***only*** that other option.
     */
    private fun testParameterizeWithOptionDefault(
        configureWithDefault: Builder.() -> Unit,
        parameterizeWithDefault: (block: ParameterizeScope.() -> Unit) -> Unit,
        test: ParameterizeWithOptionDefault.() -> Unit
    ) = testAll(
        "with defaults from builder" to {
            val context = object : ParameterizeContext {
                override val parameterizeConfiguration = ParameterizeConfiguration { configureWithDefault() }
            }

            val defaultParameterize = ParameterizeWithOptionDefault { block ->
                with(context) {
                    parameterize(block = block)
                }
            }

            test(defaultParameterize)
        },
        "with defaults from non-contextual overload" to {
            val defaultParameterize = ParameterizeWithOptionDefault { block ->
                parameterizeWithDefault(block)
            }

            test(defaultParameterize)
        },
    )

    /** @see testParameterizeWithOptionDefault */
    private fun testParameterizeWithOptionDefault(test: ParameterizeWithOptionDefault.() -> Unit): Unit =
        testParameterizeWithOptionDefault(
            configureWithDefault = {},
            parameterizeWithDefault = { block -> parameterize(block = block) },
            test = test
        )

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
    fun decorator_default_should_invoke_iteration_function_once() = testParameterizeWithOptionDefault {
        var iterationInvocations = 0

        parameterizeWithOptionDefault {
            iterationInvocations++
        }

        assertEquals(1, iterationInvocations, "iterationInvocations")
    }

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
    fun on_failure_default_should_rethrow_the_failure() = testParameterizeWithOptionDefault {
        var iterationCount = 0

        class RethrownFailure : Throwable()

        assertFailsWith<RethrownFailure> {
            parameterizeWithOptionDefault {
                val iteration by parameterOf(1..2)

                iterationCount++
                throw RethrownFailure()
            }
        }

        assertEquals(1, iterationCount, "iterationCount")
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
    fun on_complete_default_should_throw_ParameterizeFailedError() = testParameterizeWithOptionDefault(
        // Continue on failure, so parameterize doesn't terminate before onComplete gets to run
        configureWithDefault = { onFailure = {} },
        parameterizeWithDefault = { block -> parameterize(onFailure = {}, block = block) }
    ) {
        assertFailsWith<ParameterizeFailedError> {
            parameterizeWithOptionDefault {
                fail()
            }
        }
    }
}
