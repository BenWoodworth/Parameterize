package com.benwoodworth.parameterize

actual val Throwable.stackTraceLines: List<String>
    get() = (this.asDynamic().stack as? String)
        ?.removeSuffix("\n")?.lines()
        ?: emptyList()
