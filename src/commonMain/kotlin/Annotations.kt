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
 * Marks declarations that are still **experimental** in Parameterize, which means that the design of the corresponding
 * declarations has open issues which may (or may not) lead to their changes in the future. Roughly speaking, there is a
 * chance that those declarations will be deprecated in the near future or the semantics of their behavior may change in
 * some way that may break some code.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    CLASS, ANNOTATION_CLASS, TYPEALIAS,
    PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER,
    CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER
)
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
public annotation class ExperimentalParameterizeApi

/**
 * Used to prevent [parameterize] functions from being used in the wrong scope.
 *
 * @see DslMarker
 */
@DslMarker
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, TYPE, TYPEALIAS)
public annotation class ParameterizeDsl
