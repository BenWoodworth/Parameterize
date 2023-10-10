package com.benwoodworth.parameterize

internal actual fun Throwable.clearStackTrace() {
    // The `stack` property is non-standard, but supported on all major browsers/servers:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error/stack#browser_compatibility
    this.asDynamic().stack = null
}
