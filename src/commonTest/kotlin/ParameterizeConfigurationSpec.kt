package com.benwoodworth.parameterize

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ParameterizeConfigurationSpec {
    @Test
    fun throw_handler_should_default_to_the_context_throw_handler() {
        var contextThrowHandlerWasUsed = false

        val context = object : ParameterizeContext {
            override val parameterizeConfiguration = ParameterizeConfiguration {
                throwHandler = { contextThrowHandlerWasUsed = true }
            }
        }

        with(context) {
            parameterize {
                error("fail")
            }
        }

        assertTrue(contextThrowHandlerWasUsed)
    }

    @Test
    fun throw_handler_should_be_the_provided_throw_handler() {
        var providedThrowHandlerWasUsed = false

        val context = object : ParameterizeContext {
            override val parameterizeConfiguration = ParameterizeConfiguration {}
        }

        with(context) {
            parameterize(
                throwHandler = { providedThrowHandlerWasUsed = true }
            ) {
                error("fail")
            }
        }

        assertTrue(providedThrowHandlerWasUsed)
    }

    @Test
    fun throw_handler_without_context_should_default_to_throwing_ParameterizeFailedError() {
        assertFailsWith<ParameterizeFailedError> {
            parameterize {
                error("fail")
            }
        }
    }

    @Test
    fun throw_handler_without_context_should_use_the_provided_throw_handler() {
        var providedThrowHandlerWasUsed = false

        parameterize(
            throwHandler = { providedThrowHandlerWasUsed = true }
        ) {
            error("fail")
        }

        assertTrue(providedThrowHandlerWasUsed)
    }
}
