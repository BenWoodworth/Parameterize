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
