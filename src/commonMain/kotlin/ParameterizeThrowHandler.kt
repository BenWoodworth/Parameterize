package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

public typealias ParameterizeThrowHandler = ParameterizeThrowHandlerScope.(cause: Throwable) -> Unit

public class ParameterizeThrowHandlerScope internal constructor(
    private val context: ParameterizeContext,
    private val cause: Throwable
) {
    public val arguments: List<Pair<KProperty<*>, *>> by lazy {
        context.getReadParameters()
    }

    public operator fun ParameterizeFailedError.Companion.invoke(): ParameterizeFailedError =
        ParameterizeFailedError(arguments, cause)
}
