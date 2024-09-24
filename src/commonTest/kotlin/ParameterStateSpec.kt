/*
 * Copyright 2024 Ben Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.benwoodworth.parameterize

import kotlin.test.*

class ParameterStateSpec {
    private val getArgumentBeforeDeclaredMessage = "Cannot get argument before parameter has been declared"
    private val getFailureArgumentBeforeDeclaredMessage =
        "Cannot get failure argument before parameter has been declared"

    private val property: String get() = error("${::property.name} is not meant to be used")
    private val differentProperty: String get() = error("${::differentProperty.name} is not meant to be used")

    private lateinit var parameter: ParameterState<Any?>

    @BeforeTest
    fun beforeTest() {
        parameter = ParameterState()
    }


    private fun assertUndeclared(parameter: ParameterState<*>) {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.getArgument()
        }

        assertEquals(getArgumentBeforeDeclaredMessage, failure.message, "message")
    }

    @Test
    fun string_representation_when_not_declared_should_match_message_from_lazy() {
        val messageFromLazy = lazy { error("unused") }.toString()

        val replacements = listOf(
            "Lazy value" to "Parameter",
            "initialized" to "declared"
        )

        val expected = replacements
            .onEach { (old) ->
                check(old in messageFromLazy) { "'$old' in '$messageFromLazy'" }
            }
            .fold(messageFromLazy) { result, (old, new) ->
                result.replace(old, new)
            }

        assertEquals(expected, parameter.toString())
    }

    @Test
    fun string_representation_when_initialized_should_equal_that_of_the_current_argument() {
        val argument = "argument"

        parameter.declare(sequenceOf(argument))

        assertSame(argument, parameter.toString())
    }

    @Test
    fun has_been_used_should_initially_be_false() {
        assertFalse(parameter.hasBeenUsed)
    }

    @Test
    fun declaring_with_no_arguments_should_throw_ParameterizeContinue() {
        assertFailsWith<ParameterizeContinue> {
            parameter.declare(emptySequence<String>())
        }
    }

    @Test
    fun declaring_with_no_arguments_should_leave_parameter_undeclared() {
        runCatching {
            parameter.declare(emptySequence<String>())
        }

        assertUndeclared(parameter)
    }

    @Test
    fun declare_should_immediately_get_the_first_argument() {
        var gotFirstArgument = false

        val arguments = Sequence {
            gotFirstArgument = true
            listOf(Unit).iterator()
        }

        parameter.declare(arguments)
        assertTrue(gotFirstArgument, "gotFirstArgument")
    }

    @Test
    fun declare_should_not_immediately_get_the_second_argument() {
        class AssertingIterator : Iterator<String> {
            var nextArgument = 1

            override fun hasNext(): Boolean =
                nextArgument <= 2

            override fun next(): String {
                assertNotEquals(2, nextArgument, "should not get argument 2")

                return "argument $nextArgument"
                    .also { nextArgument++ }
            }
        }

        parameter.declare(Sequence(::AssertingIterator))
    }

    @Test
    fun declare_with_one_argument_should_set_is_last_argument_to_true() {
        parameter.declare(sequenceOf("first"))

        assertTrue(parameter.isLastArgument)
    }

    @Test
    fun declare_with_more_than_one_argument_should_set_is_last_argument_to_false() {
        parameter.declare(sequenceOf("first", "second"))

        assertFalse(parameter.isLastArgument)
    }

    @Test
    fun getting_argument_before_declared_should_throw_IllegalStateException() {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.getArgument()
        }

        assertEquals(getArgumentBeforeDeclaredMessage, failure.message, "message")
    }

    @Test
    @Ignore
    fun getting_argument_with_the_wrong_property_should_throw_ParameterizeException() {
        parameter.declare(sequenceOf(Unit))

        val exception = assertFailsWith<ParameterizeException> {
            parameter.getArgument()
        }

        assertEquals(
            "Cannot use parameter delegate with `differentProperty`, since it was declared with `property`.",
            exception.message
        )
    }

    @Test
    fun getting_argument_should_initially_return_the_first_argument() {
        parameter.declare(sequenceOf("first", "second"))

        assertEquals("first", parameter.getArgument())
    }

    @Test
    fun use_argument_should_set_has_been_used_to_true() {
        parameter.declare(sequenceOf("first", "second"))
        parameter.useArgument()

        assertTrue(parameter.hasBeenUsed)
    }

    @Test
    fun next_before_declare_should_throw_IllegalStateException() {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.nextArgument()
        }

        assertEquals("Cannot iterate arguments before parameter has been declared", failure.message)
    }

    @Test
    fun next_should_move_to_the_next_argument() {
        parameter.declare(sequenceOf("first", "second", "third"))
        parameter.getArgument()

        parameter.nextArgument()
        assertEquals("second", parameter.getArgument())

        parameter.nextArgument()
        assertEquals("third", parameter.getArgument())
    }

    @Test
    fun next_to_a_middle_argument_should_leave_is_last_argument_as_false() {
        parameter.declare(sequenceOf("first", "second", "third", "fourth"))
        parameter.getArgument()

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "second")

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "third")
    }

    @Test
    fun next_to_the_last_argument_should_set_is_last_argument_to_true() {
        parameter.declare(sequenceOf("first", "second", "third", "fourth"))
        parameter.getArgument()
        parameter.nextArgument() // second
        parameter.nextArgument() // third
        parameter.nextArgument() // forth

        assertTrue(parameter.isLastArgument)
    }

    @Ignore
    @Test
    fun next_after_the_last_argument_should_loop_back_to_the_first() {
        parameter.declare(sequenceOf("first", "second"))
        parameter.getArgument()
        parameter.nextArgument() // second
        parameter.nextArgument() // first

        assertEquals("first", parameter.getArgument())
    }

    @Ignore
    @Test
    fun next_after_the_last_argument_should_set_is_last_argument_to_false() {
        parameter.declare(sequenceOf("first", "second"))
        parameter.getArgument()
        parameter.nextArgument() // second
        parameter.nextArgument() // first

        assertFalse(parameter.isLastArgument)
    }

    @Test
    fun redeclare_should_not_change_current_argument() {
        parameter.declare(sequenceOf("a", "b"))

        val newArguments = Sequence<String> {
            fail("Re-declaring should keep the old arguments")
        }
        parameter.declare(newArguments)

        assertEquals("a", parameter.getArgument())
    }

    @Test
    fun redeclare_arguments_should_keep_using_the_original_arguments() {
        parameter.declare(sequenceOf("a"))

        val newArguments = Sequence<String> {
            fail("Re-declaring should keep the old arguments")
        }
        parameter.declare(newArguments)
    }

    @Test
    @Ignore
    fun redeclare_with_different_parameter_should_throw_ParameterizeException() {
        parameter.declare(sequenceOf(Unit))

        assertFailsWith<ParameterizeException> {
            parameter.declare(sequenceOf(Unit))
        }
    }

    @Test
    fun redeclare_with_different_parameter_should_not_change_has_been_used() {
        parameter.declare(sequenceOf("a"))
        parameter.useArgument()

        runCatching {
            parameter.declare(sequenceOf("a"))
        }

        assertTrue(parameter.hasBeenUsed)
    }

    @Test
    fun is_last_argument_before_declared_should_throw() {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.isLastArgument
        }
        assertEquals("Parameter has not been declared", failure.message)
    }

    @Test
    fun get_failure_argument_when_not_declared_should_throw_IllegalStateException() {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.getFailureArgument()
        }

        assertEquals(getFailureArgumentBeforeDeclaredMessage, failure.message, "message")
    }

    @Test
    fun get_failure_argument_when_declared_should_have_correct_property_and_argument() {
        val expectedArgument = "a"
        parameter.declare(sequenceOf(expectedArgument))
        parameter.property = ::property
        parameter.getArgument()

        val (property, argument) = parameter.getFailureArgument()
        assertTrue(property.equalsProperty(::property))
        assertSame(expectedArgument, argument)
    }
}
