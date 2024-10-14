package com.benwoodworth.parameterize.test

public inline fun <R> probeThrow(throws: MutableList<Throwable?>, block: () -> R): R { // TODO Remove
    return try {
        block()
    } catch (thrown: Throwable) {
        throws += thrown
        throw thrown
    }
}
