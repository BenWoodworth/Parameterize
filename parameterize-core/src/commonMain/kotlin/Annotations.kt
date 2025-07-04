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

import kotlin.annotation.AnnotationTarget.*

/**
 * Used to prevent [parameterize] functions from being used in the wrong scope.
 *
 * @suppress
 * @see DslMarker
 * @see LazyParameterScope.provideDelegate
 */
@DslMarker
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, TYPE, TYPEALIAS)
@Deprecated(
    "Deprecated in favor of shadowing `Parameter.provideDelegate()`. See `LazyParameterScope.provideDelegate()`.",
    level = DeprecationLevel.ERROR
)
public annotation class ParameterizeDsl
