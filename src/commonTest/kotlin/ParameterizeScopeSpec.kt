@file:Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION")

package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeScope.ParameterDelegate
import kotlin.properties.PropertyDelegateProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ParameterizeScopeSpec {
    @Test
    fun string_representation_should_show_used_parameter_arguments_in_declaration_order() = parameterize {
        val a by parameterOf(1)
        val unused1 by parameterOf<Nothing>()
        val b by parameterOf(2)
        val unused2 by parameterOf<Nothing>()
        val c by parameterOf(3)

        // used in a different order
        useParameter(c)
        useParameter(b)
        useParameter(a)

        assertEquals("${ParameterizeScope::class.simpleName}(a = $a, b = $b, c = $c)", this.toString())
    }

    @Test
    fun parameter_delegate_string_representation_when_not_initialized_should_match_message_from_lazy() = parameterize {
        lateinit var delegate: ParameterDelegate<Nothing>

        val parameter by PropertyDelegateProvider { thisRef: Nothing?, property ->
            parameterOf<Nothing>()
                .provideDelegate(thisRef, property)
                .also { delegate = it } // intercept delegate
        }

        val messageFromLazy = lazy { "unused" }
            .toString()
            .replace("Lazy value", "Parameter argument")

        assertEquals(messageFromLazy, delegate.toString())
    }

    @Test
    fun parameter_delegate_string_representation_when_initialized_should_equal_that_of_the_current_argument() = parameterize {
        lateinit var delegate: ParameterDelegate<String>

        val parameter by PropertyDelegateProvider { thisRef: Nothing?, property ->
            parameterOf("argument")
                .provideDelegate(thisRef, property)
                .also { delegate = it } // intercept delegate
        }

        useParameter(parameter)

        assertSame(parameter, delegate.toString())
    }
}
