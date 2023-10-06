package com.benwoodworth.parameterize

import kotlin.test.*

class ParameterDelegateSpec {
    private val property: String get() = error("${::property.name} is not meant to be used")
    private val differentProperty: String get() = error("${::differentProperty.name} is not meant to be used")

    private lateinit var delegate: ParameterDelegate<String>

    @BeforeTest
    fun beforeTest() {
        delegate = ParameterDelegate()
    }


    @Test
    fun has_been_used_should_initially_be_false() {
        assertFalse(delegate.hasBeenUsed)
    }

    @Test
    fun declare_should_not_immediately_get_an_argument() {
        val arguments = Iterable<String> {
            fail("Arguments should not be iterated")
        }

        delegate.declare(::property, arguments)
    }

    @Test
    fun getting_argument_before_declared_should_throw_IllegalStateException() {
        val failure = assertFailsWith<IllegalStateException> {
            delegate.getArgument(::property)
        }

        assertEquals("Cannot get argument before parameter delegate has been declared", failure.message)
    }

    @Test
    fun getting_argument_with_the_wrong_property_should_throw_ParameterizeException() {
        delegate.declare(::property, emptyList())

        val exception = assertFailsWith<ParameterizeException> {
            delegate.getArgument(::differentProperty)
        }

        assertEquals(
            "Cannot use parameter delegate with `differentProperty`. Already declared for `property`.",
            exception.message
        )
    }

    @Test
    fun getting_argument_with_no_arguments_should_throw_ParameterizeContinue() {
        delegate.declare(::property, emptyList())

        assertFailsWith<ParameterizeContinue> {
            delegate.getArgument(::property)
        }
    }

    @Test
    fun getting_argument_with_no_arguments_should_not_change_has_been_used() {
        delegate.declare(::property, emptyList())
        val expected = delegate.hasBeenUsed

        runCatching {
            delegate.getArgument(::property)
        }

        assertEquals(expected, delegate.hasBeenUsed)
    }

    @Test
    fun getting_argument_should_initially_return_the_first_argument() {
        delegate.declare(::property, listOf("first", "second"))

        assertEquals("first", delegate.getArgument(::property))
    }

    @Test
    fun getting_argument_should_set_has_been_used_to_true() {
        delegate.declare(::property, listOf("first", "second"))
        delegate.getArgument(::property)

        assertTrue(delegate.hasBeenUsed)
    }

    @Test
    fun getting_argument_with_one_argument_should_set_is_last_argument_to_true() {
        delegate.declare(::property, listOf("first"))
        delegate.getArgument(::property)

        assertTrue(delegate.isLastArgument)
    }

    @Test
    fun getting_argument_with_more_than_one_argument_should_set_is_last_argument_to_false() {
        delegate.declare(::property, listOf("first", "second"))
        delegate.getArgument(::property)

        assertFalse(delegate.isLastArgument)
    }

    @Test
    fun next_before_declare_should_throw_IllegalStateException() {
        val failure = assertFailsWith<IllegalStateException> {
            delegate.nextArgument()
        }

        assertEquals("Cannot iterate arguments before parameter delegate has been declared", failure.message)
    }

    @Test
    fun next_before_initialized_should_throw_IllegalStateException() {
        delegate.declare(::property, emptyList())

        val failure = assertFailsWith<IllegalStateException> {
            delegate.nextArgument()
        }

        assertEquals("Cannot iterate arguments before parameter argument has been initialized", failure.message)
    }

    @Test
    fun next_should_move_to_the_next_argument() {
        delegate.declare(::property, listOf("first", "second", "third"))
        delegate.getArgument(::property)

        delegate.nextArgument()
        assertEquals("second", delegate.getArgument(::property))

        delegate.nextArgument()
        assertEquals("third", delegate.getArgument(::property))
    }

    @Test
    fun next_to_a_middle_argument_should_leave_is_last_argument_as_false() {
        delegate.declare(::property, listOf("first", "second", "third", "fourth"))
        delegate.getArgument(::property)

        delegate.nextArgument()
        assertFalse(delegate.isLastArgument, "second")

        delegate.nextArgument()
        assertFalse(delegate.isLastArgument, "third")
    }

    @Test
    fun next_to_the_last_argument_should_set_is_last_argument_to_true() {
        delegate.declare(::property, listOf("first", "second", "third", "fourth"))
        delegate.getArgument(::property)
        delegate.nextArgument() // second
        delegate.nextArgument() // third
        delegate.nextArgument() // forth

        assertTrue(delegate.isLastArgument)
    }

    @Test
    fun next_after_the_last_argument_should_loop_back_to_the_first() {
        delegate.declare(::property, listOf("first", "second"))
        delegate.getArgument(::property)
        delegate.nextArgument() // second
        delegate.nextArgument() // first

        assertEquals("first", delegate.getArgument(::property))
    }

    @Test
    fun next_after_the_last_argument_should_set_is_last_argument_to_false() {
        delegate.declare(::property, listOf("first", "second"))
        delegate.getArgument(::property)
        delegate.nextArgument() // second
        delegate.nextArgument() // first

        assertFalse(delegate.isLastArgument)
    }

    @Test
    fun redeclare_should_not_change_current_argument() {
        delegate.declare(::property, listOf("a", "b"))

        val newArguments = Iterable<String> {
            fail("Re-declaring should keep the old arguments")
        }
        delegate.declare(::property, newArguments)
    }

    @Test
    fun redeclare_arguments_should_keep_using_the_original_arguments() {
        delegate.declare(::property, listOf("a"))

        val newArguments = Iterable<String> {
            fail("Re-declaring should keep the old arguments")
        }
        delegate.declare(::property, newArguments)
    }

    @Test
    fun redeclare_with_different_parameter_should_throw_ParameterizeException() {
        val delegate = delegate

        delegate.declare(::property, emptyList())

        assertFailsWith<ParameterizeException> {
            delegate.declare(::differentProperty, emptyList())
        }
    }

    @Test
    fun redeclare_with_different_parameter_should_not_change_has_been_used() {
        delegate.declare(::property, listOf("a"))
        delegate.getArgument(::property)

        runCatching {
            delegate.declare(::differentProperty, listOf("a"))
        }

        assertTrue(delegate.hasBeenUsed)
    }

    @Test
    fun reset_should_set_has_been_used_to_false() {
        delegate.declare(::property, listOf("a", "b"))
        delegate.getArgument(::property)
        delegate.reset()

        assertFalse(delegate.hasBeenUsed)
    }

    @Test
    fun to_string_when_not_initialized_should_match_message_from_lazy() {
        delegate.declare(::property, emptyList())

        val expectedToString = lazy { "unused" }
            .toString()
            .replace("Lazy value", "Parameter argument")

        assertEquals(expectedToString, delegate.toString())
    }

    @Test
    fun to_string_when_initialized_should_equal_that_of_the_current_argument() {
        delegate.declare(::property, listOf("a"))

        val argument = delegate.getArgument(::property)
        assertEquals(argument, delegate.toString())
    }

    @Test
    fun is_last_argument_before_initialized_should_throw() {
        val failure1 = assertFailsWith<IllegalStateException> {
            delegate.isLastArgument
        }
        assertEquals("Argument has not been initialized", failure1.message)

        delegate.declare(::property, listOf("a"))
        val failure2 = assertFailsWith<IllegalStateException> {
            delegate.isLastArgument
        }
        assertEquals("Argument has not been initialized", failure2.message)
    }

    @Test
    fun get_property_argument_when_not_initialized_should_be_null() {
        assertNull(delegate.getPropertyArgumentOrNull())

        delegate.declare(::property, emptyList())
        assertNull(delegate.getPropertyArgumentOrNull())
    }

    @Test
    fun get_property_argument_when_initialized_should_have_correct_property_and_argument() {
        val expectedArgument = "a"
        delegate.declare(::property, listOf(expectedArgument))
        delegate.getArgument(::property)

        val propertyArgument = delegate.getPropertyArgumentOrNull()
        assertNotNull(propertyArgument)

        val (property, argument) = propertyArgument
        assertTrue(property.equalsProperty(::property))
        assertSame(expectedArgument, argument)
    }
}
