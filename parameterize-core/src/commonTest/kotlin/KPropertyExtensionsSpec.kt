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
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KPropertyExtensionsSpec {
    @Test
    fun property_should_equal_the_same_instance() {
        val property by ReadOnlyProperty { _, property -> property }

        val reference = property

        assertTrue(reference.equalsProperty(reference))
    }

    @Test
    fun property_should_equal_the_same_property_through_different_delegate_calls() {
        val property by ReadOnlyProperty { _, property -> property }

        val reference1 = property
        val reference2 = property

        assertTrue(reference1.equalsProperty(reference2))
    }

    @Test
    fun property_should_not_equal_a_different_property() {
        val property1 by ReadOnlyProperty { _, property -> property }
        val property2 by ReadOnlyProperty { _, property -> property }

        val reference1 = property1
        val reference2 = property2

        assertFalse(reference1.equalsProperty(reference2))
    }
}
