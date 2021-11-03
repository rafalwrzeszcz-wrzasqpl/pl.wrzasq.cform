/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

import pl.wrzasq.cform.macro.pipeline.PipelineAction
import pl.wrzasq.cform.macro.pipeline.conditional
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.mapSelected

/**
 * Common setup for action types.
 *
 * @param name Action name.
 * @param input Input properties.
 * @param condition Condition name.
 */
abstract class BaseAction(
    override val name: String,
    input: Map<String, Any>,
    private val condition: String?
) : PipelineAction {
    protected val properties = input.toMutableMap()
    override val inputs = mutableSetOf<String>()
    override val outputs = mutableSetOf<String>()
    override val dependencies = mutableSetOf<String>()

    // we compute these stuff but might have been defined already in the template

    override var namespace = properties["Namespace"]?.toString()
    override var runOrder: Int? = properties["RunOrder"]?.toString()?.toInt()

    init {
        properties["InputArtifacts"]?.let { readArtifacts(it, inputs) }
        properties["OutputArtifacts"]?.let { readArtifacts(it, outputs) }
    }

    abstract fun buildActionTypeId(): Map<String, Any>

    open fun buildConfiguration(configuration: MutableMap<String, Any>) {}

    override fun buildDefinition(): Map<String, Any> {
        val input = mutableMapOf(
            "Name" to name,
            "ActionTypeId" to buildActionTypeId()
        )

        val configuration = asMap(properties["Configuration"] ?: emptyMap<String, Any>()).toMutableMap()
        buildConfiguration(configuration)

        // we do these as conditional `.put()` calls as these stuff might be in `.properties` when defined by hand
        namespace?.let { input["Namespace"] = it }
        runOrder?.let { input["RunOrder"] = it }
        if (inputs.isNotEmpty()) {
            input["InputArtifacts"] = inputs.sorted()
        }
        if (outputs.isNotEmpty()) {
            input["OutputArtifacts"] = outputs.sorted()
        }
        if (configuration.isNotEmpty()) {
            input["Configuration"] = configuration
        }

        input.mapSelected(
            mapOf(
                "InputArtifacts" to ::buildArtifacts,
                "OutputArtifacts" to ::buildArtifacts
            )
        )

        return conditional(properties + input, condition)
    }
}

private fun readArtifacts(input: Any, target: MutableSet<String>) {
    if (input is List<*>) {
        for (artifact in input) {
            if (artifact is String) {
                target.add(artifact)
            } else if (artifact is Map<*, *> && "Name" in artifact) {
                target.add(artifact["Name"].toString())
            }
        }
    }
}

private fun buildArtifacts(input: Any) = if (input is List<*>) {
    input.map { if (it is String) mapOf("Name" to it) else it }
} else {
    input
}

/**
 * Builds action type definition for default AWS actions.
 *
 * @param category Category of action.
 * @param provider Providing service.
 * @param owner Action vendor.
 * @param version Definition version.
 */
fun buildAwsActionTypeId(category: String, provider: String, owner: String = "AWS", version: String = "1") = mapOf(
    "Category" to category,
    "Owner" to owner,
    "Provider" to provider,
    "Version" to version
)
