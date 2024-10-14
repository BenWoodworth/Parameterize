/*
 * Copyright 2024 Ben Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
