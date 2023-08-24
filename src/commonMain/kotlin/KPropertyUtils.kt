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
