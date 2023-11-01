package com.benwoodworth.parameterize

import kotlin.test.Ignore

actual typealias NativeIgnore = Ignore

// Currently not possible on native: https://youtrack.jetbrains.com/issue/KT-59017/
actual val Throwable.stackTraceLines: List<String>
    get() = throw UnsupportedOperationException("Not supported on native")
