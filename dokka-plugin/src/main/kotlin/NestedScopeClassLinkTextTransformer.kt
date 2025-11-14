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

package com.benwoodworth.parameterize.dokka

import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

/**
 * Shortens link text for nested scope classes by removing the outer classes that qualify them.
 *
 * For example:
 * ```
 * // Before
 * decorator: suspend ParameterizeConfiguration.DecoratorScope.(
 *     iteration: suspend ParameterizeConfiguration.DecoratorScope.() -> Unit)
 *
 * // After:
 * decorator: suspend DecoratorScope.(iteration: suspend DecoratorScope.() -> Unit)
 * ```
 */
class NestedScopeClassLinkTextTransformer(
    @Suppress("unused")
    context: DokkaContext
) : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode =
        input.transformContentPagesTree { page ->
            page.modified(
                content = recursivelyTrimOuterClassPrecedingScopeInLinkText(page.content)
            )
        }

    private fun recursivelyTrimOuterClassPrecedingScopeInLinkText(contentNode: ContentNode): ContentNode =
        when {
            TextStyle.Paragraph in contentNode.style -> contentNode // Don't trim links within KDoc Markdown

            contentNode is ContentDRILink -> trimOuterClassPrecedingScopeInLinkText(contentNode)

            contentNode is ContentComposite -> contentNode.transformChildren { child ->
                recursivelyTrimOuterClassPrecedingScopeInLinkText(child)
            }

            else -> contentNode
        }

    private val qualifiedScopeClassNamesRegex = Regex(""".*\.(.*Scope\b.*)""")

    private fun trimOuterClassPrecedingScopeInLinkText(link: ContentDRILink): ContentDRILink {
        val (packageName, classNames, callable) = link.address
        if (classNames == null) return link // No class name to be trimmed

        val qualifiedScopeClassNamesMatch = qualifiedScopeClassNamesRegex.matchEntire(classNames)
            ?: return link // No qualified scope name to trim

        val trimmedScopeClassNames = qualifiedScopeClassNamesMatch.groupValues[1]

        val linkContent = link.children.single() as ContentText
        return when (linkContent.text) {
            callable?.name -> link // No class in link text

            trimmedScopeClassNames -> link // Already trimmed

            listOfNotNull(classNames, callable?.name).joinToString(".") -> {
                val trimmedLinkText = listOfNotNull(trimmedScopeClassNames, callable?.name).joinToString(".")
                val trimmedLinkContent = linkContent.copy(text = trimmedLinkText)
                link.copy(children = listOf(trimmedLinkContent))
            }

            else -> error(
                "Unhandled link text while trimming preceding outer class names " +
                        "from $qualifiedScopeClassNamesMatch link: '${linkContent.text}'"
            )
        }
    }
}
