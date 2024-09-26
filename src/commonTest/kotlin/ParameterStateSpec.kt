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

    private lateinit var parameter: ParameterState

    @BeforeTest
    fun beforeTest() {
        parameter = ParameterState(ParameterizeState())
    }


    private fun assertUndeclared(parameter: ParameterState) {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.getArgument(::property)
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

        parameter.declare(::property, sequenceOf(argument))

        assertSame(argument, parameter.toString())
    }

    @Test
    fun declaring_with_no_arguments_should_throw_ParameterizeContinue() {
        assertFailsWith<ParameterizeContinue> {
            parameter.declare(::property, emptySequence())
        }
    }

    @Test
    fun declaring_with_no_arguments_should_leave_parameter_undeclared() {
        runCatching {
            parameter.declare(::property, emptySequence())
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

        parameter.declare(::property, arguments)
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

        parameter.declare(::property, Sequence(::AssertingIterator))
    }

    @Test
    fun declare_with_one_argument_should_set_is_last_argument_to_true() {
        parameter.declare(::property, sequenceOf("first"))

        assertTrue(parameter.isLastArgument)
    }

    @Test
    fun declare_with_more_than_one_argument_should_set_is_last_argument_to_false() {
        parameter.declare(::property, sequenceOf("first", "second"))

        assertFalse(parameter.isLastArgument)
    }

    @Test
    fun getting_argument_before_declared_should_throw_IllegalStateException() {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.getArgument(::property)
        }

        assertEquals(getArgumentBeforeDeclaredMessage, failure.message, "message")
    }

    @Test
    fun getting_argument_with_the_wrong_property_should_throw_ParameterizeException() {
        parameter.declare(::property, sequenceOf(Unit))

        val exception = assertFailsWith<ParameterizeException> {
            parameter.getArgument(::differentProperty)
        }

        assertEquals(
            "Cannot use parameter with `differentProperty`, since it was declared with `property`.",
            exception.message
        )
    }

    @Test
    fun getting_argument_should_initially_return_the_first_argument() {
        parameter.declare(::property, sequenceOf("first", "second"))

        assertEquals("first", parameter.getArgument(::property))
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
        parameter.declare(::property, sequenceOf("first", "second", "third"))
        parameter.getArgument(::property)

        parameter.nextArgument()
        assertEquals("second", parameter.getArgument(::property))

        parameter.nextArgument()
        assertEquals("third", parameter.getArgument(::property))
    }

    @Test
    fun next_to_a_middle_argument_should_leave_is_last_argument_as_false() {
        parameter.declare(::property, sequenceOf("first", "second", "third", "fourth"))
        parameter.getArgument(::property)

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "second")

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "third")
    }

    @Test
    fun next_to_the_last_argument_should_set_is_last_argument_to_true() {
        parameter.declare(::property, sequenceOf("first", "second", "third", "fourth"))
        parameter.getArgument(::property)
        parameter.nextArgument() // second
        parameter.nextArgument() // third
        parameter.nextArgument() // forth

        assertTrue(parameter.isLastArgument)
    }

    @Test
    fun next_after_the_last_argument_should_loop_back_to_the_first() {
        parameter.declare(::property, sequenceOf("first", "second"))
        parameter.getArgument(::property)
        parameter.nextArgument() // second
        parameter.nextArgument() // first

        assertEquals("first", parameter.getArgument(::property))
    }

    @Test
    fun next_after_the_last_argument_should_set_is_last_argument_to_false() {
        parameter.declare(::property, sequenceOf("first", "second"))
        parameter.getArgument(::property)
        parameter.nextArgument() // second
        parameter.nextArgument() // first

        assertFalse(parameter.isLastArgument)
    }

    @Test
    fun redeclare_should_not_change_current_argument() {
        parameter.declare(::property, sequenceOf("a", "b"))

        val newArguments = Sequence<String> {
            fail("Re-declaring should keep the old arguments")
        }
        parameter.declare(::property, newArguments)

        assertEquals("a", parameter.getArgument(::property))
    }

    @Test
    fun redeclare_arguments_should_keep_using_the_original_arguments() {
        parameter.declare(::property, sequenceOf("a"))

        val newArguments = Sequence<String> {
            fail("Re-declaring should keep the old arguments")
        }
        parameter.declare(::property, newArguments)
    }

    @Test
    fun redeclare_with_different_parameter_should_throw_ParameterizeException() {
        parameter.declare(::property, sequenceOf(Unit))

        assertFailsWith<ParameterizeException> {
            parameter.declare(::differentProperty, sequenceOf(Unit))
        }
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
        parameter.declare(::property, sequenceOf(expectedArgument))
        parameter.getArgument(::property)

        val (property, argument) = parameter.getFailureArgument()
        assertTrue(property.equalsProperty(::property))
        assertSame(expectedArgument, argument)
    }
}
