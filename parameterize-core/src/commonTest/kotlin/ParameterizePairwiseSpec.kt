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

import com.benwoodworth.parameterize.ParameterizeScope.DeclaredParameter
import com.benwoodworth.parameterize.ParameterizeScope.Parameter
import kotlin.reflect.KProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParameterizePairwiseSpec {
    private fun DeclaredParameter<*>.equalsParameter(other: DeclaredParameter<*>): Boolean =
        property.name == other.property.name && argument == other.argument

    private fun Collection<DeclaredParameter<*>>.equalsParameters(others: Collection<DeclaredParameter<*>>): Boolean =
        size == others.size && all { parameter -> others.any { parameter.equalsParameter(it) } }

    private fun iterationHasPairwiseCombination(
        iterationParameters: List<DeclaredParameter<*>>,
        combinationParameters: List<DeclaredParameter<*>>
    ): Boolean =
        combinationParameters.all { combinationParameter ->
            iterationParameters.any { iterationParameter ->
                combinationParameter.equalsParameter(iterationParameter)
            }
        }


    private fun ParameterizeScope.declareParameters() {
        val abc by parameterOf('a', 'b', 'c')
        val pqr by parameterOf('p', 'q', 'r')
        val xyz by parameterOf('x', 'y', 'z')
    }

    private val firstIterationParameters = buildList {
        val interceptingScope = object : ParameterizeScope {
            override fun <T> Parameter<T>.provideDelegate(
                thisRef: Nothing?,
                property: KProperty<*>
            ): DeclaredParameter<T> {
                val parameter = this@provideDelegate

                @OptIn(ExperimentalParameterizeApi::class)
                return DeclaredParameter(property, arguments.first())
                    .also { add(property to parameter as Parameter<*>) } // TODO Compiler error if the cast isn't used to hide T
            }
        }

        with(interceptingScope) {
            declareParameters()
        }
    }

    @OptIn(ExperimentalParameterizeApi::class)
    private val pairwiseCombinations = buildSet {
        for (i in 0..firstIterationParameters.lastIndex - 1) {
            val (property1, parameter1) = firstIterationParameters[i]

            for (j in i + 1..firstIterationParameters.lastIndex) {
                val (property2, parameter2) = firstIterationParameters[j]

                for (argument1 in parameter1.arguments) {
                    val declaredParameter1 = DeclaredParameter(property1, argument1)

                    for (argument2 in parameter2.arguments) {
                        val declaredParameter2 = DeclaredParameter(property2, argument2)

                        add(listOf(declaredParameter1, declaredParameter2))
                    }
                }
            }
        }
    }

    private val pairwiseIterations = buildList {
        parameterizePairwise {
            val iterationParameters = buildList {
                val interceptingScope = object : ParameterizeScope {
                    override fun <T> Parameter<T>.provideDelegate(
                        thisRef: Nothing?,
                        property: KProperty<*>
                    ): DeclaredParameter<T> {
                        return with(this@parameterizePairwise) { provideDelegate(thisRef, property) }
                            .also { add(it as DeclaredParameter<*>) } // TODO Compiler error if the cast isn't used to hide T
                    }
                }

                interceptingScope.declareParameters()
            }

            add(iterationParameters)
        }
    }

    private val exhaustiveIterations = buildList {
        parameterize {
            val iterationParameters = buildList {
                val interceptingScope = object : ParameterizeScope {
                    override fun <T> Parameter<T>.provideDelegate(
                        thisRef: Nothing?,
                        property: KProperty<*>
                    ): DeclaredParameter<T> {
                        return with(this@parameterize) { provideDelegate(thisRef, property) }
                            .also { add(it as DeclaredParameter<*>) } // TODO Compiler error if the cast isn't used to hide T
                    }
                }

                interceptingScope.declareParameters()
            }

            add(iterationParameters)
        }
    }

    private val pairwiseCombinationIterations = pairwiseCombinations
        .associateWith { pairwiseCombination ->
            pairwiseIterations.filter { pairwiseIteration ->
                iterationHasPairwiseCombination(pairwiseIteration, pairwiseCombination)
            }
        }

    @Test
    fun pairwise_combinations_should_all_be_iterated() {
        val pairwiseCombinationsIterated = pairwiseCombinationIterations
            .filterValues { it.isNotEmpty() }
            .keys

        assertEquals(pairwiseCombinations, pairwiseCombinationsIterated, "Pairwise combinations iterated")
    }

    @Test
    fun pairwise_combinations_should_not_appear_in_multiple_iterations() {
        val pairwiseCombinationsIteratedMoreThanOnce = pairwiseCombinationIterations
            .filterValues { it.size > 1 }

        assertTrue(
            pairwiseCombinationsIteratedMoreThanOnce.isEmpty(),
            "Pairwise combinations appeared in multiple iterations: " +
                    pairwiseCombinationsIteratedMoreThanOnce.entries.joinToString()
        )
    }

    /**
     * A weaker constraint than iterating through each combination exactly once, since this allows pairs of first
     * arguments to appear multiple times, but this is still exponentially better than reaching every argument.
     */
    @Test
    fun pairwise_combinations_should_not_appear_in_multiple_iterations_unless_the_combination_has_a_first_argument() {
        @OptIn(ExperimentalParameterizeApi::class)
        val firstParameterArguments = firstIterationParameters
            .map { (property, parameter) -> DeclaredParameter(property, parameter.arguments.first()) }

        fun DeclaredParameter<*>.isFirstArgument(): Boolean =
            firstParameterArguments.any { this.equalsParameter(it) }

        val pairwiseCombinationsIteratedMoreThanOnce = pairwiseCombinationIterations
            .filterValues { it.size > 1 }
            .filterKeys { combination ->
                combination.none { it.isFirstArgument() }
            }

        assertTrue(
            pairwiseCombinationsIteratedMoreThanOnce.isEmpty(),
            "Pairwise combinations appeared in multiple iterations: " +
                    pairwiseCombinationsIteratedMoreThanOnce.entries.joinToString()
        )
    }

    @Test
    fun countCombinations() {
        println("Ideal:      ${pairwiseCombinations.size / 3}") // Since each iteration can cover 3 combinations
        println("Pairwise:   ${pairwiseIterations.size}")
        println("Exhaustive: ${exhaustiveIterations.size}")
    }

    @Test
    fun listPotentialIterations() {
        println("\n\nKeep iteration if first with a pairwise combination:")
        run {
            val reachedPairwiseCombinations = mutableSetOf<List<DeclaredParameter<*>>>()
            val lines = mutableListOf<String>()

            pairwiseIterations.forEachIndexed { index, iteration ->
                val iterationPairwiseCombinations = pairwiseCombinations
                    .filter { iterationHasPairwiseCombination(iteration, it) }

                val duplicatedCombinations = iterationPairwiseCombinations
                    .filter { combination ->
                        reachedPairwiseCombinations.any { it.equalsParameters(combination) }
                    }

                val include = duplicatedCombinations.none()
//                        && index != 0  //
//                        && index != 4 //
//                        && index != 8 //
//                        && index != 10 //
//                        && index != 12 //
//                        && index != 16 //
//                        && index != 18 //

//                        && index != 0 && index != 1 //
//                        && index != 0 && index != 3 //
//                        && index != 0 && index != 8 //
//                        && index != 0 && index != 9 //
//                        && index != 0 && index != 16 //
//                        && index != 0 && index != 18 //

//                    && iteration != "apx" //
//                    && !(iteration[0].argument == 'a' && iteration[1].argument == 'r' && iteration[2].argument == 'z')// != "aqy" // <- works
//                    && iteration != "arz" //
//                    && iteration != "bpy" // <- works
//                    && iteration != "bqx" // <- works
//                    && iteration != "cpz" //
//                    && iteration != "crx" //

//                    && iteration != "apx" && iteration != "apy" // <- works
//                    && iteration != "apx" && iteration != "aqx" // <- works
//                    && iteration != "apx" && iteration != "arz" //
//                    && iteration != "apx" && iteration != "bpx" // <- works
//                    && iteration != "apx" && iteration != "bqy" // <- works
//                    && iteration != "apx" && iteration != "cpz" //
//                    && iteration != "apx" && iteration != "crx" //

                if (include) {
                    reachedPairwiseCombinations += iterationPairwiseCombinations
                }

                lines += buildString {
                    val indexString = index.toString().padStart(2, ' ')
                    append("$indexString:  $iteration (${iterationPairwiseCombinations.joinToString()})")

                    if (!include) {
                        append("  <- skip")
                        if (duplicatedCombinations.any()) {
                            append(": duplicates ${duplicatedCombinations.joinToString()}")
                        }
                    }
                }
            }

            val missedCombinations = pairwiseCombinations - reachedPairwiseCombinations
            println("Missed combinations: ${missedCombinations.joinToString()}")
            println(lines.joinToString("\n"))

            assertTrue { missedCombinations.isEmpty() }
        }

//        println("\n\nKeep iteration if last with a pairwise combination:")
//        run {
//            val reachedPairwiseCombinations = mutableSetOf<String>()
//            val lines = mutableListOf<String>()
//
//            regularIterations.reversed().forEach { iteration ->
//                val iterationPairwiseCombinations = pairwiseCombinations
//                    .filter { iteration.hasPairwiseCombination(it) }
//
//                val duplicatedCombinations = iterationPairwiseCombinations
//                    .filter { it in reachedPairwiseCombinations }
//
//                lines += buildString {
//                    append("$iteration (${iterationPairwiseCombinations.joinToString()})")
//
//                    if (duplicatedCombinations.none()) {
//                        reachedPairwiseCombinations += iterationPairwiseCombinations
//                    } else {
//                        append("  <- skip: would be duplicated with ${duplicatedCombinations.joinToString()}")
//                    }
//                }
//            }
//
//            lines.reverse()
//
//            println("Missed combinations: ${(pairwiseCombinations - reachedPairwiseCombinations).joinToString()}")
//            println(lines.joinToString("\n"))
    }

    @Test
    fun findWorkingCombinations() {
        fun search(
            unusedIterations: List<String>,
            usedPairwiseCombinations: Set<String>,
        ): List<String>? {
            fun String.hasPairwiseCombination(combination: String): Boolean =
                combination.all { it in this }

            for (iteration in unusedIterations) {
                val pairwiseCombinations = buildSet {
                    for (i in 0..iteration.lastIndex - 1) {
                        for (j in i..iteration.lastIndex) {
                            add("${iteration[i]}${iteration[j]}")
                        }
                    }
                }

                if (pairwiseCombinations.none { it in usedPairwiseCombinations }) {
                    val newUnusedIterations = unusedIterations - iteration

                    val searchSkippingThisIteration = search(newUnusedIterations, usedPairwiseCombinations)
                    if (searchSkippingThisIteration != null) return searchSkippingThisIteration

                    val searchUsingThisIteration =
                        search(newUnusedIterations, usedPairwiseCombinations - pairwiseCombinations)
                    if (searchUsingThisIteration != null) return searchUsingThisIteration
                }
            }

            return null
        }

        val searchResult = search(
            unusedIterations = buildList {
                for (abc in "abc") for (pqr in "pqr") for (xyz in "xyz") {
                    add("$abc$pqr$xyz")
                }
            },
            usedPairwiseCombinations = emptySet()
        )

        println(searchResult)
    }

    @Test
    fun findWorkingCombinations2() {
        fun <T> combinations(input: List<T>, k: Int): Sequence<List<T>> = sequence {
            val n = input.size
            if (k > n) return@sequence

            val indices = IntArray(k) { it }
            while (true) {
                yield(indices.map { input[it] })

                var i = k - 1
                while (i >= 0 && indices[i] == i + n - k) i--
                if (i < 0) break

                indices[i]++
                for (j in i + 1 until k) {
                    indices[j] = indices[j - 1] + 1
                }
            }
        }

        val exhaustiveIterations = buildList {
            for (abc in "abc") for (pqr in "pqr") for (xyz in "xyz") {
                add("$abc$pqr$xyz")
            }
        }

        fun List<String>.hasDuplicateCombinations(): Boolean {
            var reachedCombinationFlags = 0

            for (iteration in this) {
                val abc = iteration[0] - 'a'
                val pqr = iteration[1] - 'p'
                val xyz = iteration[2] - 'x'

                val abcpqrFlag = 1 shl (abc * 3 + pqr)
                if (reachedCombinationFlags and abcpqrFlag != 0) return true

                val abcxyzFlag = 1 shl (abc * 3 + xyz + 9)
                if (reachedCombinationFlags and abcxyzFlag != 0) return true

                val pqrxyzFlag = 1 shl (pqr * 3 + xyz + 18)
                if (reachedCombinationFlags and pqrxyzFlag != 0) return true

                reachedCombinationFlags = reachedCombinationFlags or abcpqrFlag or abcxyzFlag or pqrxyzFlag
            }

            return false
        }

        combinations(exhaustiveIterations, 9)
            .filter { !it.hasDuplicateCombinations() }
            .filter { result ->
                result
                    .map { iteration -> exhaustiveIterations.indexOf(iteration) }
                    .let { it == it.sorted() }
            }
            .forEach { println(it) }

    }

    @Test
    fun calculateIterationCounts() {
        class IterationCalculator(
            val name: String,
            val calculate: (parameters: List<Int>) -> Int
        )

        val calculators = listOf(
            IterationCalculator("Exhaustive") { parameters -> // 27
                parameters.fold(1) { acc, parameter -> acc * parameter }
            },

            IterationCalculator("Pairwise (implemented)") { parameters -> // 19
                var result = 0

                parameterizePairwise {
                    for (parameter in parameters) {
                        val parameter by parameter(0..<parameter)
                    }

                    result++
                }

                result
            },

            IterationCalculator("Pairwise (ideal)") { parameters -> // 9
                val totalCombinations = parameters.fold(1) { acc, parameter -> acc * parameter }

                var totalPairs = 0
                for (parameter1 in 0..parameters.lastIndex - 1) {
                    for (parameter2 in parameter1 + 1..parameters.lastIndex) {
                        totalPairs += parameters[parameter1] * parameters[parameter2]
                    }
                }

                fun nCr(n: Int, r: Int): Long {
                    require(r >= 0 && r <= n) { "r must be between 0 and n" }
                    val k = minOf(r, n - r) // Use symmetry: nCr = nC(n-r)
                    var result = 1L
                    for (i in 1..k) {
                        result = result * (n - i + 1) / i
                    }
                    return result
                }

                val pairsPerIteration = nCr(parameters.size, 2)

                totalPairs / pairsPerIteration.toInt()
            }
        )

        fun calculateFor(vararg parameters: Int) {
            println("Calculating for " + parameters.contentToString())
            calculators.forEach { calculator ->
                val iterations = calculator.calculate(parameters.asList())
                println("${calculator.name}: $iterations")
            }
            println()
        }

        calculateFor(3, 3, 3, 3, 3)
        calculateFor(5, 5, 5, 5, 5)
        calculateFor(2, 3, 4, 5, 6)
        calculateFor(9, 9, 9, 9, 9, 9, 9, 9, 9)
        calculateFor(10, 10, 10, 10, 10, 10, 10, 10, 10, 10)

        parameterizePairwise {
            val a by parameter(0..5)
            val b by parameter(0..5)
            val c by parameter(0..5)
            val d by parameter(0..5)

            println("$a$b$c$d")
        }
    }
}

// Possible ways to optimally iterate pairwise:
// [apx, aqy, arz, bpy, bqz, brx, cpz, cqx, cry]
// [apx, aqy, arz, bpz, bqx, bry, cpy, cqz, crx]
// [apx, aqz, ary, bpy, bqx, brz, cpz, cqy, crx]
// [apx, aqz, ary, bpz, bqy, brx, cpy, cqx, crz]
// [apy, aqx, arz, bpx, bqz, bry, cpz, cqy, crx]
// [apy, aqx, arz, bpz, bqy, brx, cpx, cqz, cry]
// [apy, aqz, arx, bpx, bqy, brz, cpz, cqx, cry]
// [apy, aqz, arx, bpz, bqx, bry, cpx, cqy, crz]
// [apz, aqx, ary, bpx, bqy, brz, cpy, cqz, crx]
// [apz, aqx, ary, bpy, bqz, brx, cpx, cqy, crz]
// [apz, aqy, arx, bpx, bqz, bry, cpy, cqx, crz]
// [apz, aqy, arx, bpy, bqx, brz, cpx, cqz, cry]

//apx
//apy
//apz
//aqx
//aqy
//aqz
//arx
//ary
//arz
//bpx
//bpy
//bpz
//bqx
//bqy
//bqz
//brx
//bry
//brz
//cpx
//cpy
//cpz
//cqx
//cqy
//cqz
//crx
//cry
//crz
