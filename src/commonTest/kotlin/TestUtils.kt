package com.benwoodworth.parameterize

@Suppress("UNUSED_PARAMETER")
fun <T> readProperty(property: T) {
}

/**
 * Library user may have their own lazy iterator that non-deterministically
 * reads other parameters. This means it can't be assumed that
 * [parameter(lazyArguments)][parameter] is the only way parameter() will be
 * called during another initialization, and that lazy parameters should work
 * without any special internal machinery.
 */
fun <T> ParameterizeScope.customLazyParameter(
    lazyArguments: () -> Iterable<T>
): Parameter<T> {
    val arguments by lazy(lazyArguments)

    class CustomLazyArguments : Iterable<T> {
        override fun iterator(): Iterator<T> = arguments.iterator()
    }

    return parameter(CustomLazyArguments())
}
