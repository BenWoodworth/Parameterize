@file:JvmMultifileClass
@file:JvmName("ParameterizeKt")

package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.ParameterizeConfiguration.OnCompleteScope
import com.benwoodworth.parameterize.ParameterizeConfiguration.OnFailureScope
import kotlin.DeprecationLevel.HIDDEN
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Binary compatibility for the [parameterize] functions after adding new arguments.
 *
 * When adding new arguments to [parameterize]:
 * - Copy the old contextual and non-contextual function signatures to this file, marking them as deprecated and hidden,
 *   and have them delegate to the new [parameterize] function signatures.
 * - Update the [ParameterizeConfiguration] spec:
 *     - Update the list of configuration options
 *     - Add a test for the option being applied
 *     - Add a test for the default behaviour
 * - Dump API, and confirm that the old signatures are the same (but with an added `synthetic` modifier)
 */
private const val binaryCompatibilityMessage = "Binary compatibility"


@Deprecated(binaryCompatibilityMessage, level = HIDDEN)
public fun ParameterizeContext.parameterize(
    onFailure: OnFailureScope.(failure: Throwable) -> Unit = parameterizeConfiguration.onFailure,
    onComplete: OnCompleteScope.() -> Unit = parameterizeConfiguration.onComplete,
    block: ParameterizeScope.() -> Unit
): Unit =
    parameterize(onFailure, onComplete, block = block)

@Deprecated(binaryCompatibilityMessage, level = HIDDEN)
public fun parameterize(
    onFailure: OnFailureScope.(failure: Throwable) -> Unit = DefaultParameterizeContext.parameterizeConfiguration.onFailure,
    onComplete: OnCompleteScope.() -> Unit = DefaultParameterizeContext.parameterizeConfiguration.onComplete,
    block: ParameterizeScope.() -> Unit
): Unit =
    parameterize(onFailure, onComplete, block = block)
