package com.benwoodworth.parameterize

internal actual fun Throwable.clearStackTrace() {
    stackTrace = emptyArray()
}
