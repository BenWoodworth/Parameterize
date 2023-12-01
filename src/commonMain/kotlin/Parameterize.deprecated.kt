@file:JvmMultifileClass
@file:JvmName("ParameterizeKt")

package com.benwoodworth.parameterize

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
@Suppress("unused")
private const val binaryCompatibilityMessage = "Binary compatibility"
