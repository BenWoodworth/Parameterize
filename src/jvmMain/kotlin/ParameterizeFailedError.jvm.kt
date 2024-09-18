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

import org.opentest4j.MultipleFailuresError

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-20641/
public actual class ParameterizeFailedError internal actual constructor(
    internal actual val recordedFailures: List<ParameterizeFailure>,
    internal actual val failureCount: Long,
    internal actual val passCount: Long,
    internal actual val completedEarly: Boolean
) : MultipleFailuresError(null, emptyList()) {
    public actual companion object;

    init {
        commonInit()
    }

    public actual override val message: String
        get() = commonMessage

    // Usually won't be used, so compute as needed (instead of always providing it to `MultipleFailuresError` up front)
    @Deprecated("Exists for MultipleFailuresError tooling, and is not API", level = DeprecationLevel.HIDDEN)
    override fun getFailures(): List<Throwable> =
        recordedFailures.map { it.failure }

    @Deprecated("Exists for MultipleFailuresError tooling, and is not API", level = DeprecationLevel.HIDDEN)
    override fun hasFailures(): Boolean =
        recordedFailures.isNotEmpty()
}

internal actual fun Throwable.clearStackTrace() {
    stackTrace = emptyArray()
}
