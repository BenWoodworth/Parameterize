package com.benwoodworth.parameterize

internal actual fun Throwable.clearStackTrace() {
    // Currently not possible on native: https://youtrack.jetbrains.com/issue/KT-59017/
}
