package com.benwoodworth.parameterize

public class ParameterizeFailure internal constructor(
    public val failure: Throwable,
    public val arguments: List<ParameterizeArgument<*>>
) {
    override fun toString(): String =
        "ParameterizeFailure(failure=$failure, arguments=$arguments)"
}
