package com.benwoodworth.parameterize

/**
 * Create a subclass of [Throwable] without a `stack` being captured.
 *
 * For classes extending [Throwable], Kotlin/JS checks if `captureStackTrace` is defined, and if so, uses that to
 * capture/set the [Throwable]'s `stack` property. So, this function temporarily defines `captureStackTrace`, and has
 * it set `stack` to `null`, instead of taking time to collect the actual stack.
 *
 * In Kotlin 2.0.21:
 * ```
 * // The JS Constructor produced for subclasses of `Throwable`:
 * function ThrowableSubclass() {
 *   extendThrowable(this);
 *   captureStack(this, ThrowableSubclass);
 * }
 *
 * // The kotlin-stdlib-js code checking/calling `captureStackTrace`:
 * internal fun captureStack(instance: Throwable, constructorFunction: Any) {
 *     if (js("Error").captureStackTrace != null) {
 *         js("Error").captureStackTrace(instance, constructorFunction)
 *     } else {
 *         instance.asDynamic().stack = js("new Error()").stack
 *     }
 * }
 * ```
 */
internal inline fun <T : Throwable> createThrowableSubclassWithoutStack(create: () -> T): T {
    val originalCaptureStackTrace = js("Error").captureStackTrace

    return try {
        js("Error").captureStackTrace = captureNullStackTrace
        create()
    } finally {
        js("Error").captureStackTrace = originalCaptureStackTrace
    }
}

private val captureNullStackTrace = js(
    "function captureNullStackTrace(instance) { instance.stack = null }"
)
