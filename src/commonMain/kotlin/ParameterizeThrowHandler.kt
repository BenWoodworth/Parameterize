package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

public typealias ParameterizeThrowHandler = ParameterizeThrowHandlerScope.(thrown: Throwable) -> Unit

public class ParameterizeThrowHandlerScope internal constructor(
    private val context: ParameterizeContext
) {
    public class ParameterArgument internal constructor(
        public val parameter: KProperty<*>,
        public val argument: Any?
    )

    public val parameters: List<ParameterArgument> by lazy {
        context.getReadParameters().map { (parameter, argument) ->
            ParameterArgument(parameter, argument)
        }
    }
}
