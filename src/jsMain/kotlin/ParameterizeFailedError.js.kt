/*
 * Copyright 2024 Ben Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.benwoodworth.parameterize

public actual class ParameterizeFailedError internal actual constructor(
    internal actual val recordedFailures: List<ParameterizeFailure>,
    internal actual val failureCount: Long,
    internal actual val iterationCount: Long,
    internal actual val completedEarly: Boolean
) : AssertionError() {
    public actual companion object;

    init {
        commonInit()
    }

    public actual override val message: String
        get() = commonMessage
}

internal actual fun Throwable.clearStackTrace() {
    // The `stack` property is non-standard, but supported on all major browsers/servers:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error/stack#browser_compatibility
    this.asDynamic().stack = null
}
