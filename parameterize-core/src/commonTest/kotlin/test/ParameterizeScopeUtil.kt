package com.benwoodworth.parameterize.test

import com.benwoodworth.parameterize.ParameterizeScope
import com.benwoodworth.parameterize.ParameterizeState
import com.benwoodworth.parameterize.SimpleParameterizeScope

internal val ParameterizeScope.parameterizeState: ParameterizeState
    get() {
        check(this is SimpleParameterizeScope) {
            "Expected scope to be a ${SimpleParameterizeScope::class.simpleName}, but was ${this::class.simpleName}"
        }
        return parameterizeState
    }
