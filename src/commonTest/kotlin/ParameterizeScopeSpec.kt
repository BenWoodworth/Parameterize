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

import com.benwoodworth.parameterize.ParameterizeScope.DeclaredParameter
import com.benwoodworth.parameterize.ParameterizeScope.Parameter
import com.benwoodworth.parameterize.ParameterizeScopeSpec.LazyParameterFunction.LazyArguments
import kotlin.properties.PropertyDelegateProvider
import kotlin.test.*

/**
 * Specifies the [parameterize] DSL and its syntax.
 */
class ParameterizeScopeSpec {
    /**
     * A unique iterator that the tests can use to verify that a constructed [Parameter] has the correct
     * [arguments][Parameter.arguments].
     */
    private data object ArgumentIterator : Iterator<Nothing> {
        override fun hasNext(): Boolean = false
        override fun next(): Nothing = throw NoSuchElementException()
    }

    @Test
    fun parameter_from_sequence_should_be_constructed_with_the_same_arguments_instance() = parameterize {
        val sequence = sequenceOf<Nothing>()
        val parameter = parameter(sequence)

        assertSame(sequence, parameter.arguments)
    }

    @Test
    fun parameter_from_iterable_should_have_the_correct_arguments() = parameterize {
        val parameter = parameter(Iterable { ArgumentIterator })

        assertSame(ArgumentIterator, parameter.arguments.iterator())
    }

    @Test
    fun parameter_of_listed_arguments_should_have_the_correct_arguments() = parameterize {
        data class UniqueArgument(val argument: String)

        val listedArguments = listOf(
            UniqueArgument("A"),
            UniqueArgument("B"),
            UniqueArgument("C")
        )

        val parameter = parameterOf(*listedArguments.toTypedArray())

        assertContentEquals(listedArguments.asSequence(), parameter.arguments)
    }

    /**
     * The lazy `parameter {}` functions should have the same behavior, so this provides an abstraction that a test can
     * use to specify for all the lazy overloads parametrically.
     */
    private interface LazyParameterFunction {
        operator fun <T> invoke(scope: ParameterizeScope, lazyArguments: () -> LazyArguments<T>): Parameter<T>

        class LazyArguments<T>(val createIterator: () -> Iterator<T>)
    }

    private val lazyParameterFunctions = listOf(
        "from sequence" to object : LazyParameterFunction {
            override fun <T> invoke(scope: ParameterizeScope, lazyArguments: () -> LazyArguments<T>): Parameter<T> =
                with(scope) {
                    parameter {
                        val arguments = lazyArguments()
                        Sequence { arguments.createIterator() }
                    }
                }
        },
        "from iterable" to object : LazyParameterFunction {
            override fun <T> invoke(scope: ParameterizeScope, lazyArguments: () -> LazyArguments<T>): Parameter<T> =
                with(scope) {
                    parameter {
                        val arguments = lazyArguments()
                        Iterable { arguments.createIterator() }
                    }
                }
        }
    )

    @Test
    fun parameter_from_lazy_arguments_should_have_the_correct_arguments() = parameterize {
        testAll(lazyParameterFunctions) { lazyParameterFunction ->
            val lazyParameter = lazyParameterFunction(this@parameterize) {
                LazyArguments { ArgumentIterator }
            }

            assertSame(ArgumentIterator, lazyParameter.arguments.iterator())
        }
    }

    @Test
    fun parameter_from_lazy_arguments_should_not_be_computed_before_declaring() = parameterize {
        testAll(lazyParameterFunctions) { lazyParameterFunction ->
            /*val undeclared by*/ lazyParameterFunction<Nothing>(this@parameterize) { fail("computed") }
        }
    }

    @Test
    fun parameter_from_lazy_argument_iterable_should_only_be_computed_once() = parameterize {
        testAll(lazyParameterFunctions) { lazyParameterFunction ->
            var evaluationCount = 0

            val lazyParameter = lazyParameterFunction(this@parameterize) {
                evaluationCount++
                LazyArguments { (1..10).iterator() }
            }

            repeat(5) { i ->
                val arguments = lazyParameter.arguments.toList()
                assertEquals((1..10).toList(), arguments, "Iteration #$i")
            }

            assertEquals(1, evaluationCount)
        }
    }

    @Test
    fun string_representation_should_show_used_parameter_arguments_in_declaration_order() = parameterize {
        val a by parameterOf(1)
        val unused1 by parameterOf(Unit)
        val b by parameterOf(2)
        val unused2 by parameterOf(Unit)
        val c by parameterOf(3)

        // Used in a different order
        useParameter(c)
        useParameter(b)
        useParameter(a)

        assertEquals("${ParameterizeScope::class.simpleName}(a = $a, b = $b, c = $c)", this.toString())
    }

    @Test
    fun declared_parameter_string_representation_when_declared_should_equal_that_of_the_current_argument() =
        parameterize {
            lateinit var declaredParameter: DeclaredParameter<String>

            val parameter by PropertyDelegateProvider { thisRef: Nothing?, property ->
                parameterOf("argument")
                    .provideDelegate(thisRef, property)
                    .also { declaredParameter = it } // intercept delegate
            }

            assertSame(parameter, declaredParameter.toString())
        }
}
