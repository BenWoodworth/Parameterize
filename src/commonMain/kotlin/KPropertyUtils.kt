package com.benwoodworth.parameterize

import kotlin.reflect.KProperty

private val canCompareWithEquals = run {
    class PropertyReference {
        operator fun getValue(thisRef: Any?, property: KProperty<*>) = property
    }

    val property by PropertyReference()

    property == property
}

// Kotlin/JS `KProperty.equals()` does not work: https://youtrack.jetbrains.com/issue/KT-59866/
internal fun KProperty<*>.isSameAs(other: KProperty<*>): Boolean =
    when {
        this === other -> true
        canCompareWithEquals -> this == other
        else -> this.name == other.name
    }
