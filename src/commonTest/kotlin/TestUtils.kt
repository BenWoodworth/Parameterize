package com.benwoodworth.parameterize

import kotlin.test.Ignore

/**
 * [Ignore] on native targets.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class NativeIgnore()

@Suppress("UNUSED_PARAMETER")
fun <T> useParameter(parameter: T) {
}
