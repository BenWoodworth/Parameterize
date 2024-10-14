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
    private val getDeclaredParameterBeforeDeclaredMessage = "Cannot get declared parameter before it's been declared"

    private val property: String get() = error("${::property.name} is not meant to be used")
    private val differentProperty: String get() = error("${::differentProperty.name} is not meant to be used")

    private lateinit var parameter: ParameterState

    @BeforeTest
    fun beforeTest() {
        parameter = ParameterState(ParameterizeState())
    }


    private fun assertUndeclared(parameter: ParameterState) {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.getDeclaredParameter()
        }

        assertEquals(getDeclaredParameterBeforeDeclaredMessage, failure.message, "message")
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
            parameter.declare(::property, emptySequence<Nothing>())
        }
    }

    @Test
    fun declaring_with_no_arguments_should_leave_parameter_undeclared() {
        runCatching {
            parameter.declare(::property, emptySequence<Nothing>())
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
    fun getting_declared_parameter_before_declared_should_throw_IllegalStateException() {
        val failure = assertFailsWith<IllegalStateException> {
            parameter.getDeclaredParameter()
        }

        assertEquals(getDeclaredParameterBeforeDeclaredMessage, failure.message, "message")
    }

    @Test
    fun getting_declared_parameter_should_initially_return_the_first_argument() {
        parameter.declare(::property, sequenceOf("first", "second"))

        val declaredParameter = parameter.getDeclaredParameter()
        assertEquals("first", declaredParameter.argument)
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
        parameter.getDeclaredParameter()

        parameter.nextArgument()
        val secondDeclaredParameter = parameter.getDeclaredParameter()
        assertEquals("second", secondDeclaredParameter.argument)

        parameter.nextArgument()
        val thirdDeclaredParameter = parameter.getDeclaredParameter()
        assertEquals("third", thirdDeclaredParameter.argument)
    }

    @Test
    fun next_to_a_middle_argument_should_leave_is_last_argument_as_false() {
        parameter.declare(::property, sequenceOf("first", "second", "third", "fourth"))
        parameter.getDeclaredParameter()

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "second")

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "third")
    }

    @Test
    fun next_to_the_last_argument_should_set_is_last_argument_to_true() {
        parameter.declare(::property, sequenceOf("first", "second", "third", "fourth"))
        parameter.getDeclaredParameter()
        parameter.nextArgument() // second
        parameter.nextArgument() // third
        parameter.nextArgument() // forth

        assertTrue(parameter.isLastArgument)
    }

    @Test
    fun next_after_the_last_argument_should_loop_back_to_the_first() {
        parameter.declare(::property, sequenceOf("first", "second"))
        parameter.getDeclaredParameter()
        parameter.nextArgument() // second
        parameter.nextArgument() // first

        val declaredParameter = parameter.getDeclaredParameter()
        assertEquals("first", declaredParameter.argument)
    }

    @Test
    fun next_after_the_last_argument_should_set_is_last_argument_to_false() {
        parameter.declare(::property, sequenceOf("first", "second"))
        parameter.getDeclaredParameter()
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

        val declaredParameter = parameter.getDeclaredParameter()
        assertEquals("a", declaredParameter.argument)
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
    fun redeclare_with_different_parameter_should_throw_ParameterizeBreak() {
        parameter.declare(::property, sequenceOf(Unit))

        assertFailsWith<ParameterizeBreak> {
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
}
