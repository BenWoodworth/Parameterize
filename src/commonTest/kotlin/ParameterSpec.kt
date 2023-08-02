package com.benwoodworth.parameterize

import kotlin.reflect.KProperty
import kotlin.test.*

class ParameterSpec {
    private fun <T> declaredParameter(arguments: Iterable<T>): Parameter<T> =
        Parameter<T>().apply { declare(arguments) }

    private fun <T> initializedParameter(arguments: Iterable<T>): Pair<KProperty<T>, Parameter<T>> {
        lateinit var propertyReference: KProperty<T>
        val parameter = declaredParameter(arguments)

        class ReferenceCapture {
            operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                @Suppress("UNCHECKED_CAST")
                propertyReference = property as KProperty<T>

                return parameter.readArgument(property)
            }
        }

        val property: T by ReferenceCapture()
        readProperty(property)

        return propertyReference to parameter
    }

    @Test
    fun newly_created_parameter_should_be_neither_declared_nor_initialized() {
        val parameter = Parameter<Nothing>()

        assertFalse(parameter.isDeclared, parameter::isDeclared.toString())
        assertFalse(parameter.isInitialized, parameter::isInitialized.toString())
    }

    @Test
    fun declared_parameter_should_be_declared_but_not_initialized() {
        val parameter = declaredParameter('a'..'c')

        assertTrue(parameter.isDeclared, parameter::isDeclared.toString())
        assertFalse(parameter.isInitialized, parameter::isInitialized.toString())
    }

    @Test
    fun initialized_parameter_should_be_declared_and_initialized() {
        val (_, parameter) = initializedParameter('a'..'c')

        assertTrue(parameter.isDeclared, parameter::isDeclared.toString())
        assertTrue(parameter.isInitialized, parameter::isInitialized.toString())
    }

    @Test
    fun reset_parameter_should_be_neither_declared_nor_initialized() {
        val undeclaredParameters = listOf(
            "newly constructed" to { Parameter<Any>() },
            "declared" to { declaredParameter('a'..'c') },
            "initialized" to { initializedParameter('a'..'c').second }
        )

        undeclaredParameters.forEach { (testCase, getParameter) ->
            val parameter = getParameter()
            parameter.reset()

            assertFalse(parameter.isDeclared, "$testCase: ${parameter::isDeclared} should be false")
            assertFalse(parameter.isInitialized, "$testCase: ${parameter::isInitialized} should be false")
        }
    }

    @Test
    fun next_argument_should_endlessly_loop_arguments() {
        val (property, parameter) = initializedParameter('a'..'c')

        var arguments = ""
        repeat(9) {
            arguments += parameter.readArgument(property)
            parameter.nextArgument()
        }

        assertEquals("abcabcabc", arguments)
    }

    @Test
    fun is_last_argument_should_be_true_only_on_last_argument() {
        val (property, parameter) = initializedParameter(listOf("first", "second", "last"))

        repeat(9) {
            val argument = parameter.readArgument(property)
            val isLast = argument == "last"

            assertEquals(
                isLast, parameter.isLastArgument,
                "$argument: parameter.isLastArgument = ${parameter.isLastArgument}"
            )

            parameter.nextArgument()
        }
    }

    @Test
    fun initializing_parameter_with_zero_arguments_should_throw_ParameterizeContinue() {
        assertFailsWith<ParameterizeContinue> {
            initializedParameter(emptyList<Nothing>())
        }
    }

    @Test
    fun getting_property_argument_should_return_null_if_not_initialized() {
        val uninitializedParameters = listOf(
            "newly constructed" to { Parameter<Any>() },
            "declared" to { declaredParameter('a'..'c') },
        )

        uninitializedParameters.forEach { (testCase, getParameter) ->
            val parameter = getParameter()
            assertNull(parameter.getPropertyArgumentOrNull(), testCase)
        }
    }

    @Test
    fun getting_property_argument_should_return_property_and_argument_if_initialized() {
        val (expectedProperty, parameter) = initializedParameter('a'..'c')

        val parameterArgument = parameter.getPropertyArgumentOrNull()
        assertNotNull(parameterArgument)

        val (property, argument) = parameterArgument
        assertEquals(expectedProperty.toString(), property.toString(), "parameter argument property")
        assertEquals(parameter.readArgument(expectedProperty), argument)
    }

    @Test
    fun to_string_when_not_initialized_should_match_message_from_lazy() {
        val uninitializedParameters = listOf(
            "newly constructed" to Parameter<Any>(),
            "declared" to declaredParameter('a'..'c'),
        )

        val expectedToString = lazy { "unused" }
            .toString()
            .replace("Lazy value", "Parameter argument")

        uninitializedParameters.forEach { (testCase, parameter) ->
            assertEquals(expectedToString, parameter.toString(), testCase)
        }
    }

    @Test
    fun to_string_when_initialized_should_be_the_argument_to_string() {
        val (property, parameter) = initializedParameter('a'..'c')

        repeat(100) {
            val argument = parameter.readArgument(property)
            assertEquals(argument.toString(), parameter.toString())

            parameter.nextArgument()
        }
    }
}
