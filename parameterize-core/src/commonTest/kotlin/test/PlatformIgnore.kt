/*
 * Copyright 2025 Ben Woodworth
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

package com.benwoodworth.parameterize.test

import kotlin.test.Ignore

/**
 * [Ignore] on native targets.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class NativeIgnore()

/**
 * [Ignore] on wasm js targets.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class WasmJsIgnore()

/**
 * [Ignore] on wasm wasi targets.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class WasmWasiIgnore()
