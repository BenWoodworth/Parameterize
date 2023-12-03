package com.benwoodworth.parameterize.test

import com.benwoodworth.parameterize.ParameterizeContinue
import com.benwoodworth.parameterize.ParameterizeException
import com.benwoodworth.parameterize.ParameterizeState
import com.benwoodworth.parameterize.parameterize

internal object EdgeCases {
    val iterationFailures = listOf<Pair<String, (ParameterizeState) -> Throwable>>(
        "ParameterizeContinue" to {
            ParameterizeContinue
        },
        "ParameterizeException for same parameterize" to { parameterizeState ->
            ParameterizeException(parameterizeState, "same parameterize")
        },
        "ParameterizeException for different parameterize" to {
            lateinit var differentParameterizeState: ParameterizeState

            parameterize {
                differentParameterizeState = parameterizeState
            }

            ParameterizeException(differentParameterizeState, "different parameterize")
        },
        "Throwable" to {
            Throwable()
        }
    )
}
