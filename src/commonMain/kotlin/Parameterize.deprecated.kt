@file:JvmMultifileClass
@file:JvmName("ParameterizeKt")

package com.benwoodworth.parameterize

import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Binary compatibility for [parameterize] after adding new options.
 *
 * When adding new arguments to [parameterize]:
 * - Copy the old [parameterize] signature to this file, marking it as deprecated and hidden,
 *   and have it delegate to the new [parameterize] function signature.
 * - Update the [ParameterizeConfiguration] spec:
 *     - Update the list of configuration options
 *     - Add a test for the option being applied
 *     - Add a test for the default behaviour
 * - Dump API, and confirm that the old signatures are the same (but with an added `synthetic` modifier)
 */
@Suppress("unused")
private const val binaryCompatibilityMessage = "Binary compatibility"
