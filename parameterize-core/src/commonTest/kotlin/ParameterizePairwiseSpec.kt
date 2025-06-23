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

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class ParameterizePairwiseSpec {
    private val abcArguments = sequenceOf('a', 'b', 'c')
    private val pqrArguments = sequenceOf('p', 'q', 'r')
    private val xyzArguments = sequenceOf('x', 'y', 'z')

    private val iterations = buildList {
        parameterizePairwise {
            val abc by parameter(abcArguments)
            val pqr by parameter(pqrArguments)
            val xyz by parameter(xyzArguments)

            add("$abc$pqr$xyz")
        }
    }

    private val pairwiseCombinations = buildList {
        for (abc in abcArguments) for (pqr in pqrArguments) add("$abc$pqr")
        for (abc in abcArguments) for (xyz in xyzArguments) add("$abc$xyz")
        for (pqr in pqrArguments) for (xyz in xyzArguments) add("$pqr$xyz")
    }

    private val pairwiseCombinationIterations = pairwiseCombinations
        .associateWith { pairwiseCombination ->
            fun String.includesPairwiseCombination(): Boolean =
                pairwiseCombination.all { it in this }

            iterations.filter { it.includesPairwiseCombination() }
        }

    @Test
    fun pairwise_combinations_should_all_be_iterated() {
        val pairwiseCombinationsIterated = pairwiseCombinationIterations
            .filterValues { it.isNotEmpty() }
            .keys

        assertContentEquals(pairwiseCombinations, pairwiseCombinationsIterated, "Pairwise combinations iterated")
    }

    @Test
    fun pairwise_combinations_should_not_appear_in_multiple_iterations() {
        val pairwiseCombinationsIteratedMoreThanOnce = pairwiseCombinationIterations
            .filterValues { it.size > 1 }

        val messageIterations = pairwiseCombinationsIteratedMoreThanOnce
            .map { it.toString() }
            .joinToString()

        assertTrue(
            pairwiseCombinationsIteratedMoreThanOnce.isEmpty(),
            "Pairwise combinations appeared in multiple iterations: $messageIterations"
        )
    }
}
