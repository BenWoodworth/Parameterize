package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeScope.ParameterDelegate
import kotlin.properties.PropertyDelegateProvider
import kotlin.test.*

/**
 * Specifies the [parameterize] DSL and its syntax.
 */
class ParameterizeScopeSpec {
    @Test
    fun parameter_from_iterable_should_be_constructed_with_the_same_arguments_instance() = parameterize {
        val iterable = emptyList<Nothing>()
        val parameter = parameter(iterable)

        assertSame(iterable, parameter.arguments)
    }

    @Test
    fun parameter_of_listed_arguments_should_have_the_correct_arguments() = parameterize {
        data class UniqueClass(val argument: String)

        val listedArguments = listOf(
            UniqueClass("A"),
            UniqueClass("B"),
            UniqueClass("C")
        )

        val parameter = parameterOf(*listedArguments.toTypedArray())

        assertContentEquals(listedArguments.asIterable(), parameter.arguments)
    }

    @Test
    fun parameter_from_lazy_arguments_should_have_the_correct_arguments() = parameterize {
        val lazyParameter = parameter { 'a'..'z' }

        assertEquals(('a'..'z').toList(), lazyParameter.arguments.toList())
    }

    @Test
    fun parameter_from_lazy_arguments_should_not_be_computed_before_declaring() = parameterize {
        /*val undeclared by*/ parameter<Nothing> { fail("computed") }
    }

    @Test
    fun parameter_from_lazy_arguments_should_only_be_computed_once() = parameterize {
        var evaluationCount = 0

        val lazyParameter = parameter {
            evaluationCount++
            1..10
        }

        repeat(5) { i ->
            val arguments = lazyParameter.arguments.toList()
            assertEquals((1..10).toList(), arguments, "Iteration #$i")
        }

        assertEquals(1, evaluationCount)
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
    fun parameter_delegate_string_representation_when_declared_should_equal_that_of_the_current_argument() = parameterize {
        lateinit var delegate: ParameterDelegate<String>

        val parameter by PropertyDelegateProvider { thisRef: Nothing?, property ->
            parameterOf("argument")
                .provideDelegate(thisRef, property)
                .also { delegate = it } // intercept delegate
        }

        assertSame(parameter, delegate.toString())
    }
}
