package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeScope.DeclaredParameter
import com.benwoodworth.parameterize.ParameterizeScope.Parameter
import kotlin.properties.PropertyDelegateProvider
import kotlin.reflect.KProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PairwiseParametersTest {
    @Test
    fun should_iterate_parameters_pairwise() {
        val results = mutableListOf<String>()

        parameterize {
            val case = object : PairwiseParameters(this) {
                val abc by parameterOf("a", "b", "c")
                val mno by parameterOf("m", "n", "o")
                val pqr by parameterOf("p", "q", "r")
                val xyz by parameterOf("x", "y", "z")
            }

            results += case.abc + case.mno + case.pqr + case.xyz
        }

        assertEquals(listOf("ampx", "bnqy", "corz"), results)
    }

    @Test
    fun parameters_with_fewer_arguments_should_fail() {
        val failure = assertFailsWith<IllegalArgumentException> {
            parameterize {
                object : PairwiseParameters(this) {
                    val abc by parameterOf("a", "b", "c")
                    val xyz by parameterOf("x", "y")
                }
            }
        }

        assertEquals("Pairwise parameter 'xyz' has fewer arguments than 'abc'.", failure.message)
    }

    @Test
    fun parameters_with_more_arguments_should_fail() {
        val failure = assertFailsWith<IllegalArgumentException> {
            parameterize {
                object : PairwiseParameters(this) {
                    val abc by parameterOf("a", "b", "c")
                    val xyz by parameterOf("x", "y", "z", "extra")
                }
            }
        }

        assertEquals("Pairwise parameter 'xyz' has more arguments than 'abc'.", failure.message)
    }

    /**
     * Ensures that the underlying argument isn't obscured by the pairwise machinery if the parameters
     * are reported.
     */
    @Test
    fun pairwise_argument_string_representation_should_match_the_argument() {
        val argument = "expected"
        var pairwiseArgument: Any? = null

        parameterize {
            val interceptingScope = object : ParameterizeScope {
                override fun <T> Parameter<T>.provideDelegate(
                    thisRef: Nothing?,
                    property: KProperty<*>
                ): DeclaredParameter<T> {
                    return with(this@parameterize) {
                        provideDelegate(thisRef, property)
                            .also { pairwiseArgument = it.argument }
                    }
                }
            }

            with(interceptingScope) {
                object : PairwiseParameters(this) {
                    val parameter by parameterOf(argument)
                }
            }
        }

        assertSame(argument, pairwiseArgument.toString())
    }

    @Test
    fun pairwise_declared_parameter_string_representation_should_match_the_underlying_parameter() {
        val argument = "argument"
        var declaredParameter: Any? = null
        var pairwiseDeclaredParameter: Any? = null

        parameterize {
            val interceptingScope = object : ParameterizeScope {
                override fun <T> Parameter<T>.provideDelegate(
                    thisRef: Nothing?,
                    property: KProperty<*>
                ): DeclaredParameter<T> {
                    return with(this@parameterize) {
                        provideDelegate(thisRef, property)
                            .also { declaredParameter = it }
                    }
                }
            }

            with(interceptingScope) {
                object : PairwiseParameters(this) {
                    val parameter by PropertyDelegateProvider { thisRef: PairwiseParameters, property ->
                        parameterOf(argument).provideDelegate(thisRef, property)
                            .also { pairwiseDeclaredParameter = it }
                    }
                }
            }
        }

        assertSame(declaredParameter.toString(), pairwiseDeclaredParameter.toString())
    }

    // TODO DeclaredParameter equals/hashCode?
}
