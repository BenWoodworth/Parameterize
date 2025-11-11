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

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    // https://github.com/gradle/gradle/issues/17963#issuecomment-1600751553
    fun plugin(provider: Provider<PluginDependency>): Provider<String> =
        provider.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

    implementation(plugin(libs.plugins.kotlin.multiplatform))
    implementation(plugin(libs.plugins.dokka))
    implementation(plugin(libs.plugins.binary.compatibility.validator))
    implementation(plugin(libs.plugins.vanniktech.maven.publish))
}
