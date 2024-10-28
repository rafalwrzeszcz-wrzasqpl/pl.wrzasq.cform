/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors.types

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.CompiledFragment
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.mapSelected

private val DEFAULTS = mapOf(
    "Version" to "2019-10-30",
)

/**
 * Simplifies Amazon Connect ContactFlow specification.
 */
class ConnectContactFlow : ResourceHandler {
    override fun handledResourceTypes() = listOf("AWS::Connect::ContactFlow")

    override fun handle(entry: ResourceDefinition) = entry.properties.mapSelected("Content", ::buildContent)

    private fun buildContent(input: Any) = if (input !is Map<*, *> || input.size == 1) {
        // if it's a plain value - nothing to do here (also applies to top-level intrinsic function calls)
        input
    } else {
        // convert nested structure into plain JSON (possibly with Fn:: call)
        CompiledFragment(DEFAULTS + asMap(input)).raw
    }
}
