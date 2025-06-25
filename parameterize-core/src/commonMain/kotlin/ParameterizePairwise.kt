/*
 * Copyright 2025 Ben Woodworth
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

import com.benwoodworth.parameterize.ParameterizeScope.Parameter
import kotlin.reflect.KProperty

// TODO https://github.com/BenWoodworth/Parameterize/issues/38
public inline fun parameterizePairwise(
    block: ParameterizeScope.() -> Unit
) {
    parameterize {
        with(PairwiseParameterizeScope(this, 2)) {
            block()
        }
    }
}

/**
 * [Parameter]s declared through this [ParameterizeScope] will have their arguments iterated through such that all pairs
 * (or [N-wise][nWise] tuples) of parameters have their combinations iterated, instead of all combinations of all
 * parameters.
 *
 * The first iteration is a baseline, with each parameter on its first argument. From there, each iteration will cover
 * one new [N-wise][nWise] parameter combination.
 */
@PublishedApi
internal class PairwiseParameterizeScope(
    private val parameterizeScope: ParameterizeScope,
    private val nWise: Int = 2
) : ParameterizeScope {
    private var pairwiseParametersDeclared = 0

    init {
        require(nWise >= 1) { "N-wise parameter count should be >= 1, but was $nWise" }
    }

    @OptIn(ExperimentalParameterizeApi::class)
    override fun <T> ParameterizeScope.Parameter<T>.provideDelegate(
        thisRef: Nothing?,
        property: KProperty<*>
    ): ParameterizeScope.DeclaredParameter<T> {
        val pairwiseParameter = parameter {
            arguments
                .let { if (pairwiseParametersDeclared == nWise) it.take(1) else it }
                .mapIndexed { index, argument ->
                    PairwiseArgument(argument, index > 0)
                }
        }

        val declaredPairwiseParameter = with(parameterizeScope) {
            pairwiseParameter.provideDelegate(thisRef, property)
        }

        if (declaredPairwiseParameter.argument.isEvaluatingCombinations) {
            pairwiseParametersDeclared++
        }

        return ParameterizeScope.DeclaredParameter(
            declaredPairwiseParameter.property,
            declaredPairwiseParameter.argument.argument
        )
    }

    private class PairwiseArgument<T>(
        val argument: T,
        val isEvaluatingCombinations: Boolean
    )
}
