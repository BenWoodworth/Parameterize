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

import com.benwoodworth.parameterize.ParameterizeScope.ParameterDelegate
import com.benwoodworth.parameterize.ParameterizeScopeSpec.LazyParameterFunction.LazyArguments
import kotlin.properties.PropertyDelegateProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

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

    /**
     * The lazy `parameter {}` functions should have the same behavior, so this provides an abstraction that a test can
     * use to specify for all the lazy overloads parametrically.
     */
    private interface LazyParameterFunction {
        suspend operator fun <T> invoke(
            scope: ParameterizeScope,
            lazyArguments: () -> LazyArguments<T>
        ): ParameterDelegate<T>

        class LazyArguments<T>(val createIterator: () -> Iterator<T>)
    }

    private val lazyParameterFunctions = listOf(
        "from sequence" to object : LazyParameterFunction {
            override suspend fun <T> invoke(
                scope: ParameterizeScope,
                lazyArguments: () -> LazyArguments<T>
            ): ParameterDelegate<T> =
                with(scope) {
                    parameter {
                        val arguments = lazyArguments()
                        Sequence { arguments.createIterator() }
                    }
                }
        },
        "from iterable" to object : LazyParameterFunction {
            override suspend fun <T> invoke(
                scope: ParameterizeScope,
                lazyArguments: () -> LazyArguments<T>
            ): ParameterDelegate<T> =
                with(scope) {
                    parameter {
                        val arguments = lazyArguments()
                        Iterable { arguments.createIterator() }
                    }
                }
        }
    )

    @Test
    fun parameter_from_lazy_arguments_should_be_computed_before_delegation() = runTestCC {
        testAll(lazyParameterFunctions) { lazyParameterFunction ->
            assertFailsWith<IllegalStateException>("computed") {
                parameterize {
                    lazyParameterFunction<Nothing>(this@parameterize) { throw IllegalStateException("computed") }
                }
            }
        }
    }

    @Test
    fun parameter_from_lazy_argument_iterable_should_only_be_computed_once() = runTestCC {
        testAll(lazyParameterFunctions) { lazyParameterFunction ->
            var currentIteration = 0
            var evaluationCount = 0
            parameterize {
                val lazyParameter by lazyParameterFunction(this@parameterize) {
                    evaluationCount++
                    LazyArguments { (1..10).iterator() }
                }

                assertEquals(currentIteration + 1, lazyParameter, "Iteration #$currentIteration")

                assertEquals(1, evaluationCount)
                currentIteration++
            }
        }
    }

    @Test
    fun string_representation_should_show_used_parameter_arguments_in_declaration_order() = runTestCC {
        parameterize {
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
    }

    @Test
    fun parameter_delegate_string_representation_when_declared_should_equal_that_of_the_current_argument() = runTestCC {
        parameterize {
            var delegate: ParameterDelegate<String>? = null
            val argument = parameterOf("argument")
            val parameter by PropertyDelegateProvider { thisRef: Nothing?, property ->
                argument
                    .provideDelegate(thisRef, property)
                    .also { delegate = it } // intercept delegate
            }

            assertSame(parameter, delegate!!.toString())
        }
    }
}
