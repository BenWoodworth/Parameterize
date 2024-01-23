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

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private val canCompareWithEquals = run {
    val propertyReference by ReadOnlyProperty { _, property -> property }

    propertyReference == propertyReference
}

// Kotlin/JS `KProperty.equals()` does not work: https://youtrack.jetbrains.com/issue/KT-59866/
internal fun KProperty<*>.equalsProperty(other: KProperty<*>): Boolean =
    when {
        this === other -> true
        canCompareWithEquals -> this == other
        else -> this.name == other.name
    }
