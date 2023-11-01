package com.benwoodworth.parameterize

actual val Throwable.stackTraceLines: List<String>
    get() = stackTrace.map { "at $it" }
