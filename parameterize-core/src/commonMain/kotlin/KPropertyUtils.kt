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

import kotlin.reflect.KProperty

/**
 * Indicates whether some [other] property is equal to [this] one.
 *
 * Necessary since not all platforms support checking that two property references are from the same property in the
 * source code. In those cases, this function compares their names so that there's some confidence that they're not
 * unequal.
 */
internal expect inline fun KProperty<*>.equalsProperty(other: KProperty<*>): Boolean
