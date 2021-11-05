/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.types

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.mapSelected

private val fallback = mapOf(
    "Artifacts" to mapOf("Type" to "CODEPIPELINE"),
    "Source" to mapOf("Type" to "CODEPIPELINE"),
)

/**
 * Simplifies CodeBuild specification.
 */
class CodeBuildSetup : ResourceHandler {
    override fun handledResourceTypes() = listOf("AWS::CodeBuild::Project")

    override fun handle(entry: ResourceDefinition) = fallback + entry.properties.mapSelected(
        "Artifacts" to { value -> handleArtifacts(asMap(value)) },
        "Cache" to ::expandCache,
        "Environment" to { value -> handleEnvironment(asMap(value)) }
    )

    private fun handleArtifacts(input: Map<String, Any>): Any {
        val output = input.toMutableMap()

        // only S3 uses Location, specifying it manually is redundant
        if ("Location" in input) {
            output.putIfAbsent("Type", "S3")
        }

        return output
    }

    private fun handleEnvironment(input: Map<String, Any>): Any {
        val output = input.toMutableMap()

        // default setup
        output.putIfAbsent("Type", "LINUX_CONTAINER")
        output.putIfAbsent("ComputeType", "BUILD_GENERAL1_SMALL")

        return output
    }

    private fun expandCache(input: Any) = if (input !is Map<*, *> || input.size == 1) {
        // converts plain values (including calls) into full definition
        mapOf(
            "Type" to "S3",
            "Location" to input
        )
    } else {
        input
    }
}
