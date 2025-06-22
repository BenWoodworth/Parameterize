package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeScope.DeclaredParameter
import com.benwoodworth.parameterize.ParameterizeScope.Parameter
import kotlin.reflect.KProperty

/**
 * ```
 * parameterize {
 *     val case = object : PairwiseParameters(this) {
 *         val abc by parameterOf("a", "b", "c")
 *         val xyz by parameterOf("x", "y", "z")
 *     }
 *
 *     println(case.abc + case.xyz) // "ax", "by", "cz"
 * }
 * ```
 */
public abstract class PairwiseParameters(
    private val parameterizeScope: ParameterizeScope
) {
    private var previousArgument: PairwiseArgument<*>? = null

    protected operator fun <T> Parameter<T>.provideDelegate(
        thisRef: PairwiseParameters,
        property: KProperty<*>
    ): PairwiseDeclaredParameter<T> = with(parameterizeScope) {
        val parameter = this@provideDelegate

        val previousArgument = this@PairwiseParameters.previousArgument

        val pairwiseArguments = if (previousArgument == null) {
            Sequence { PairwiseArgumentIterator(parameter, property) }
        } else {
            val iterator = if (previousArgument.isFirst) {
                PairwiseArgumentIterator(parameter, property)
                    .also { previousArgument.iterator.nextIterator = it }
            } else {
                val nextIterator = previousArgument.iterator.nextIterator

                requireNotNull(nextIterator) {
                    "Pairwise parameter '${property.name}' was declared when it previously wasn't."
                }

                require(property.name == nextIterator.property.name) {
                    "Expected to be declaring '${nextIterator.property.name}', but declared '${property.name}'."
                }

                @Suppress("UNCHECKED_CAST") // Checked that it's for the same property
                nextIterator as PairwiseArgumentIterator<T>
            }

            require(iterator.hasNext()) {
                "Pairwise parameter '${property.name}' has fewer arguments than '${previousArgument.property.name}'."
            }

            val pairwiseArgument = iterator.next()

            if (!previousArgument.iterator.hasNext()) require(!iterator.hasNext()) {
                "Pairwise parameter '${property.name}' has more arguments than '${previousArgument.property.name}'."
            }

            sequenceOf(pairwiseArgument)
        }

        val declaredParameter = with (parameterizeScope) {
            parameter(pairwiseArguments).provideDelegate(null, property)
        }

        return PairwiseDeclaredParameter(declaredParameter)
            .also { this@PairwiseParameters.previousArgument = declaredParameter.argument }
    }

    /**
     * TODO describe intuition
     *
     * A [Parameter]'s argument iterator persists between parameterize iterations, so that makes it a reliable place for
     * the state of the pairwise iteration to be maintained.
     */
    internal class PairwiseArgumentIterator<T>(
        val parameter: Parameter<T>,
        val property: KProperty<*>
    ) : Iterator<PairwiseArgument<T>> {
        private val iterator = parameter.arguments.iterator()
        private var isFirst = true

        var nextIterator: PairwiseArgumentIterator<*>? = null

        override fun hasNext(): Boolean = iterator.hasNext()

        override fun next(): PairwiseArgument<T> = PairwiseArgument(
            property = property,
            argument = iterator.next(),
            isFirst = isFirst.also { isFirst = false },
            isLast = !iterator.hasNext(),
            this
        )
    }

    internal open class PairwiseArgument<T>(
        val property: KProperty<*>,
        val argument: T,
        val isFirst: Boolean,
        val isLast: Boolean,
        val iterator: PairwiseArgumentIterator<T>
    ) {
        override fun toString(): String = argument.toString()
    }

    protected class PairwiseDeclaredParameter<T> internal constructor(
        private val declaredParameter: DeclaredParameter<PairwiseArgument<T>>
    ) {
        public operator fun getValue(thisRef: PairwiseParameters, property: KProperty<*>): T =
            declaredParameter.argument.argument

        override fun toString(): String = declaredParameter.toString()
    }
}
