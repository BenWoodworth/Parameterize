package com.benwoodworth.parameterize

import kotlin.test.*

class ParameterDelegateSpec {
    private val property: String get() = error("${::property.name} is not meant to be used")
    private val differentProperty: String get() = error("${::differentProperty.name} is not meant to be used")

    private lateinit var parameter: ParameterDelegate<String>

    @BeforeTest
    fun beforeTest() {
        parameter = ParameterDelegate()
    }


    @Test
    fun has_been_used_should_initially_be_false() {
        assertFalse(parameter.hasBeenUsed)
    }

    @Test
    fun declare_should_not_immediately_get_an_argument() {
        val arguments = Iterable<String> {
            fail("Arguments should not be iterated")
        }

        parameter.declare(::property, arguments)
    }

    @Test
    fun getting_argument_before_declared_should_throw_IllegalStateException() {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.getArgument(::property)
        }

        assertEquals("Cannot get argument before parameter delegate has been declared", failure.message)
    }

    @Test
    fun getting_argument_with_the_wrong_property_should_throw_ParameterizeException() {
        parameter.declare(::property, emptyList())

        val exception = assertFailsWith<ParameterizeException> {
            parameter.getArgument(::differentProperty)
        }

        assertEquals(
            "Cannot use parameter delegate with `differentProperty`. Already declared for `property`.",
            exception.message
        )
    }

    @Test
    fun getting_argument_with_no_arguments_should_throw_ParameterizeContinue() {
        parameter.declare(::property, emptyList())

        assertFailsWith<ParameterizeContinue> {
            parameter.getArgument(::property)
        }
    }

    @Test
    fun getting_argument_with_no_arguments_should_not_change_has_been_used() {
        parameter.declare(::property, emptyList())
        val expected = parameter.hasBeenUsed

        runCatching {
            parameter.getArgument(::property)
        }

        assertEquals(expected, parameter.hasBeenUsed)
    }

    @Test
    fun getting_argument_should_initially_return_the_first_argument() {
        parameter.declare(::property, listOf("first", "second"))

        assertEquals("first", parameter.getArgument(::property))
    }

    @Test
    fun getting_argument_should_set_has_been_used_to_true() {
        parameter.declare(::property, listOf("first", "second"))
        parameter.getArgument(::property)

        assertTrue(parameter.hasBeenUsed)
    }

    @Test
    fun getting_argument_with_one_argument_should_set_is_last_argument_to_true() {
        parameter.declare(::property, listOf("first"))
        parameter.getArgument(::property)

        assertTrue(parameter.isLastArgument)
    }

    @Test
    fun getting_argument_with_more_than_one_argument_should_set_is_last_argument_to_false() {
        parameter.declare(::property, listOf("first", "second"))
        parameter.getArgument(::property)

        assertFalse(parameter.isLastArgument)
    }

    @Test
    fun next_before_declare_should_throw_IllegalStateException() {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.nextArgument()
        }

        assertEquals("Cannot iterate arguments before parameter delegate has been declared", failure.message)
    }

    @Test
    fun next_before_initialized_should_throw_IllegalStateException() {
        parameter.declare(::property, emptyList())

        val failure = assertFailsWith<IllegalStateException> {
            parameter.nextArgument()
        }

        assertEquals("Cannot iterate arguments before parameter argument has been initialized", failure.message)
    }

    @Test
    fun next_should_move_to_the_next_argument() {
        parameter.declare(::property, listOf("first", "second", "third"))
        parameter.getArgument(::property)

        parameter.nextArgument()
        assertEquals("second", parameter.getArgument(::property))

        parameter.nextArgument()
        assertEquals("third", parameter.getArgument(::property))
    }

    @Test
    fun next_to_a_middle_argument_should_leave_is_last_argument_as_false() {
        parameter.declare(::property, listOf("first", "second", "third", "fourth"))
        parameter.getArgument(::property)

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "second")

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "third")
    }

    @Test
    fun next_to_the_last_argument_should_set_is_last_argument_to_true() {
        parameter.declare(::property, listOf("first", "second", "third", "fourth"))
        parameter.getArgument(::property)
        parameter.nextArgument() // second
        parameter.nextArgument() // third
        parameter.nextArgument() // forth

        assertTrue(parameter.isLastArgument)
    }

    @Test
    fun next_after_the_last_argument_should_loop_back_to_the_first() {
        parameter.declare(::property, listOf("first", "second"))
        parameter.getArgument(::property)
        parameter.nextArgument() // second
        parameter.nextArgument() // first

        assertEquals("first", parameter.getArgument(::property))
    }

    @Test
    fun next_after_the_last_argument_should_set_is_last_argument_to_false() {
        parameter.declare(::property, listOf("first", "second"))
        parameter.getArgument(::property)
        parameter.nextArgument() // second
        parameter.nextArgument() // first

        assertFalse(parameter.isLastArgument)
    }

    @Test
    fun redeclare_should_not_change_current_argument() {
        parameter.declare(::property, listOf("a", "b"))

        val newArguments = Iterable<String> {
            fail("Re-declaring should keep the old arguments")
        }
        parameter.declare(::property, newArguments)
    }

    @Test
    fun redeclare_arguments_should_keep_using_the_original_arguments() {
        parameter.declare(::property, listOf("a"))

        val newArguments = Iterable<String> {
            fail("Re-declaring should keep the old arguments")
        }
        parameter.declare(::property, newArguments)
    }

    @Test
    fun redeclare_with_different_parameter_should_throw_ParameterizeException() {
        parameter.declare(::property, emptyList())

        assertFailsWith<ParameterizeException> {
            parameter.declare(::differentProperty, emptyList())
        }
    }

    @Test
    fun redeclare_with_different_parameter_should_not_change_has_been_used() {
        parameter.declare(::property, listOf("a"))
        parameter.getArgument(::property)

        runCatching {
            parameter.declare(::differentProperty, listOf("a"))
        }

        assertTrue(parameter.hasBeenUsed)
    }

    @Test
    fun reset_should_set_has_been_used_to_false() {
        parameter.declare(::property, listOf("a", "b"))
        parameter.getArgument(::property)
        parameter.reset()

        assertFalse(parameter.hasBeenUsed)
    }

    @Test
    fun to_string_when_not_initialized_should_match_message_from_lazy() {
        parameter.declare(::property, emptyList())

        val expectedToString = lazy { "unused" }
            .toString()
            .replace("Lazy value", "Parameter argument")

        assertEquals(expectedToString, parameter.toString())
    }

    @Test
    fun to_string_when_initialized_should_equal_that_of_the_current_argument() {
        parameter.declare(::property, listOf("a"))

        val argument = parameter.getArgument(::property)
        assertEquals(argument, parameter.toString())
    }

    @Test
    fun is_last_argument_before_initialized_should_throw() {
        val failure1 = assertFailsWith<IllegalStateException> {
            parameter.isLastArgument
        }
        assertEquals("Argument has not been initialized", failure1.message)

        parameter.declare(::property, listOf("a"))
        val failure2 = assertFailsWith<IllegalStateException> {
            parameter.isLastArgument
        }
        assertEquals("Argument has not been initialized", failure2.message)
    }

    @Test
    fun get_property_argument_when_not_initialized_should_be_null() {
        assertNull(parameter.getPropertyArgumentOrNull())

        parameter.declare(::property, emptyList())
        assertNull(parameter.getPropertyArgumentOrNull())
    }

    @Test
    fun get_property_argument_when_initialized_should_have_correct_property_and_argument() {
        val expectedArgument = "a"
        parameter.declare(::property, listOf(expectedArgument))
        parameter.getArgument(::property)

        val propertyArgument = parameter.getPropertyArgumentOrNull()
        assertNotNull(propertyArgument)

        val (property, argument) = propertyArgument
        assertTrue(property.equalsProperty(::property))
        assertSame(expectedArgument, argument)
    }
}
