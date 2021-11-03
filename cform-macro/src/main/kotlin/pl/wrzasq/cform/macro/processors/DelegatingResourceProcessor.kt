/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors

import pl.wrzasq.cform.macro.processors.types.ResourceHandler
import pl.wrzasq.cform.macro.template.SECTION_RESOURCES
import pl.wrzasq.cform.macro.template.asDefinition
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.mapSelected
import pl.wrzasq.cform.macro.template.rebuildResource

/**
 * Single visitor that delegates resource processing by type.
 *
 * @param handlers Resource types handlers.
 */
class DelegatingResourceProcessor(vararg handlers: ResourceHandler) {
    private val typeHandles = mutableMapOf<String, ResourceHandler>()

    init {
        handlers.forEach(::register)
    }

    /**
     * Handles input template.
     *
     * @param input Current template state.
     * @return Processed template.
     */
    fun process(input: Map<String, Any>) = input.mapSelected(SECTION_RESOURCES) { processResources(asMap(it)) }

    // all the changes should be in-place scoped to single resource
    private fun processResources(input: Map<String, Any>) = input.mapValues {
        val entry = asDefinition(it.key, it.value)

        typeHandles[entry.type]?.let { handler -> rebuildResource(it.value, handler.handle(entry)) } ?: it.value
    }

    private fun register(handler: ResourceHandler) {
        handler.handledResourceTypes().forEach { typeHandles[it] = handler }
    }
}
