package com.benwoodworth.parameterize

import com.benwoodworth.parameterize.internal.ParameterizeApiFriendModuleApi

/**
 * A non-local control flow signal emitted by [ParameterizeScope]s.
 *
 * This type is an implementation detail of parameterize, and `catch`ing it in third-party code is an error.
 *
 * Since code that catches any [Throwable] can be problematic, [ParameterizeControlFlow] must be explicitly checked for:
 * ```
 *TODO
 * ```
 */
public abstract class ParameterizeControlFlow : Throwable {
    /** @suppress */
    @Suppress("ConvertSecondaryConstructorToPrimary")
    @ParameterizeApiFriendModuleApi // TODO BCV ignore
    public constructor()
}
