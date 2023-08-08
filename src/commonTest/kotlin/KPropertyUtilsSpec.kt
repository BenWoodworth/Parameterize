package com.benwoodworth.parameterize

import kotlin.reflect.KProperty
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KPropertyUtilsSpec {
    @Test
    fun property_should_be_same_through_different_delegates() {
        class PropertyReference {
            operator fun getValue(thisRef: Any?, variable: KProperty<*>) = variable
        }

        val variable by PropertyReference()

        val reference1 = variable
        val reference2 = variable

        assertTrue(reference1.isSameAs(reference2))
    }

    @Test
    fun property_should_not_be_same_as_another() {
        class PropertyReference {
            operator fun getValue(thisRef: Any?, variable: KProperty<*>) = variable
        }

        val variable1 by PropertyReference()
        val variable2 by PropertyReference()

        val reference1 = variable1
        val reference2 = variable2

        assertFalse(reference1.isSameAs(reference2))
    }
}
