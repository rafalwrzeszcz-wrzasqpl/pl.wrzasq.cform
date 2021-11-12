/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.pipeline.types

import pl.wrzasq.cform.macro.pipeline.PipelineAction
import pl.wrzasq.cform.macro.pipeline.conditional
import pl.wrzasq.cform.macro.template.asMapAlways
import pl.wrzasq.cform.macro.template.mapSelected

private const val OPTION_INPUTARTIFACTS = "InputArtifacts"
private const val OPTION_OUTPUTARTIFACTS = "OutputArtifacts"

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
        properties[OPTION_INPUTARTIFACTS]?.let { readArtifacts(it, inputs) }
        properties[OPTION_OUTPUTARTIFACTS]?.let { readArtifacts(it, outputs) }
    }

    /**
     * Generates `ActionTypeId` clause.
     *
     * @return Template fragment.
     */
    abstract fun buildActionTypeId(): Map<String, Any>

    /**
     * Action configuration hook.
     *
     * @param configuration Initial configuration structure.
     */
    open fun buildConfiguration(configuration: MutableMap<String, Any>) {}

    override fun buildDefinition(): Map<String, Any> {
        val input = mutableMapOf(
            "Name" to name,
            "ActionTypeId" to buildActionTypeId()
        )

        val configuration = asMapAlways(properties["Configuration"]).toMutableMap()
        buildConfiguration(configuration)

        // we do these as conditional `.put()` calls as these stuff might be in `.properties` when defined by hand
        namespace?.let { input["Namespace"] = it }
        runOrder?.let { input["RunOrder"] = it }
        if (inputs.isNotEmpty()) {
            input[OPTION_INPUTARTIFACTS] = inputs.sorted()
        }
        if (outputs.isNotEmpty()) {
            input[OPTION_OUTPUTARTIFACTS] = outputs.sorted()
        }
        if (configuration.isNotEmpty()) {
            input["Configuration"] = configuration
        }

        val definition = (properties + input).mapSelected(
            OPTION_INPUTARTIFACTS to ::buildArtifacts,
            OPTION_OUTPUTARTIFACTS to ::buildArtifacts
        )

        return conditional(definition, condition)
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
