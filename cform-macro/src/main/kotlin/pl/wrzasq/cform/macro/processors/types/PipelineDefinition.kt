/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.types

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.pipeline.PipelineManager
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.mapSelected

/**
 * Simplified CodePipeline pipeline definition.
 */
class PipelineDefinition : ResourceHandler {
    override fun handledResourceTypes() = listOf("AWS::CodePipeline::Pipeline")

    override fun handle(entry: ResourceDefinition) = entry.properties.mapSelected(
        "ArtifactStore" to ::handleStore,
        "ArtifactStores" to ::handleStores,
        "Stages" to ::processPipeline,
    )

    private fun handleStore(input: Any) = if (input !is Map<*, *> || input.size == 1) {
        // converts plain values (including calls) into full definition
        mapOf(
            "Type" to "S3",
            "Location" to input,
        )
    } else {
        input
    }

    private fun handleStores(input: Any) = if (input is List<*>) {
        input.filterNotNull().map { asMap(it).mapSelected("ArtifactStore", ::handleStore) }
    } else {
        input
    }

    private fun processPipeline(input: Any) = if (input is List<*>) {
        val manager = PipelineManager()

        if (input.filterNotNull().map(::asMap).all(manager::handleStage)) {
            manager.compile()
            manager.buildDefinition()
        } else {
            // if we fail to handle pipeline tree we should just return as-is plain template
            input
        }
    } else {
        input
    }
}
