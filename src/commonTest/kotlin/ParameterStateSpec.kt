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
    private val property: String get() = error("${::property.name} is not meant to be used")

    @Test
    fun string_representation_when_initialized_should_equal_that_of_the_current_argument() {
        val argument = "argument"
        val parameter = ParameterState(sequenceOf(argument))

        assertSame(argument, parameter.toString())
    }

    @Test
    fun declaring_with_no_arguments_should_throw_ParameterizeContinue() {
        assertFailsWith<ParameterizeContinue> {
            ParameterState(emptySequence<String>())
        }
    }

    @Test
    fun declare_should_immediately_get_the_first_argument() {
        var gotFirstArgument = false

        val arguments = Sequence {
            gotFirstArgument = true
            listOf(Unit).iterator()
        }

        ParameterState(arguments)
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

        val parameter = ParameterState(Sequence(::AssertingIterator))
    }

    @Test
    fun declare_with_one_argument_should_set_is_last_argument_to_true() {
        val parameter = ParameterState(sequenceOf("first"))

        assertTrue(parameter.isLastArgument)
    }

    @Test
    fun declare_with_more_than_one_argument_should_set_is_last_argument_to_false() {
        val parameter = ParameterState(sequenceOf("first", "second"))

        assertFalse(parameter.isLastArgument)
    }

    @Test
    @Ignore
    fun getting_argument_with_the_wrong_property_should_throw_ParameterizeException() {
        val parameter = ParameterState(sequenceOf(Unit))

        val exception = assertFailsWith<ParameterizeException> {
            parameter.argument
        }

        assertEquals(
            "Cannot use parameter delegate with `differentProperty`, since it was declared with `property`.",
            exception.message
        )
    }

    @Test
    fun getting_argument_should_initially_return_the_first_argument() {
        val parameter = ParameterState(sequenceOf("first", "second"))

        assertEquals("first", parameter.argument)
    }

    @Test
    fun use_argument_should_set_has_been_used_to_true() {
        val parameter = ParameterState(sequenceOf("first", "second"))
        parameter.useArgument()

        assertTrue(parameter.hasBeenUsed)
    }

    @Test
    fun next_should_move_to_the_next_argument() {
        val parameter = ParameterState(sequenceOf("first", "second", "third"))
        parameter.argument

        parameter.nextArgument()
        assertEquals("second", parameter.argument)

        parameter.nextArgument()
        assertEquals("third", parameter.argument)
    }

    @Test
    fun next_to_a_middle_argument_should_leave_is_last_argument_as_false() {
        val parameter = ParameterState(sequenceOf("first", "second", "third", "fourth"))

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "second")

        parameter.nextArgument()
        assertFalse(parameter.isLastArgument, "third")
    }

    @Test
    fun next_to_the_last_argument_should_set_is_last_argument_to_true() {
        val parameter = ParameterState(sequenceOf("first", "second", "third", "fourth"))
        parameter.nextArgument() // second
        parameter.nextArgument() // third
        parameter.nextArgument() // forth

        assertTrue(parameter.isLastArgument)
    }

    @Ignore
    @Test
    fun next_after_the_last_argument_should_loop_back_to_the_first() {
        val parameter = ParameterState(sequenceOf("first", "second"))
        parameter.argument
        parameter.nextArgument() // second
        parameter.nextArgument() // first

        assertEquals("first", parameter.argument)
    }

    @Ignore
    @Test
    fun next_after_the_last_argument_should_set_is_last_argument_to_false() {
        val parameter = ParameterState(sequenceOf("first", "second"))
        parameter.argument
        parameter.nextArgument() // second
        parameter.nextArgument() // first

        assertFalse(parameter.isLastArgument)
    }

    @Test
    fun get_failure_argument_when_declared_should_have_correct_property_and_argument() {
        val expectedArgument = "a"
        val parameter = ParameterState(sequenceOf(expectedArgument))
        parameter.property = ::property
        parameter.argument

        val (property, argument) = parameter.getFailureArgument()
        assertTrue(property.equalsProperty(::property))
        assertSame(expectedArgument, argument)
    }
}
