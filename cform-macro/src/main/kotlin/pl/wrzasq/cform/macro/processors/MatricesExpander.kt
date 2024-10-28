/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2023 - 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors

import pl.wrzasq.cform.macro.matrix.MatricesManager
import pl.wrzasq.cform.macro.template.CallsExpander
import pl.wrzasq.cform.macro.template.SECTION_RESOURCES
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.mapSelected
import pl.wrzasq.cform.macro.template.popProperty

private const val KEY_MATRIX = "Matrix"

/**
 * Handles expanding matrices into collections of resources.
 */
class MatricesExpander {
    /**
     * Handles input template.
     *
     * @param input Current template state.
     * @param params Template parameter values.
     * @return Processed template.
     */
    fun expand(input: Map<String, Any>, params: Map<String, Any>): Map<String, Any> {
        val manager = MatricesManager()
        val rest = input.mapSelected(SECTION_RESOURCES) {
            asMap(it).filter { (id, element) ->
                val resource = asMap(element)

                // for now, we exclude matrix-resources - will be inserted once resolved
                if (resource.containsKey(KEY_MATRIX)) {
                    resource.popProperty(KEY_MATRIX, { matrix ->
                        manager.buildMatrix(id, resource.filterKeys { key -> key != KEY_MATRIX }, asMap(matrix), params)
                    })
                    false
                } else {
                    true
                }
            }
        }

        // avoid unnecessary work and resources
        return if (manager.isEmpty()) {
            input
        } else {
            val callsExpander = CallsExpander(manager)

            callsExpander.processTemplate(
                rest.mapSelected(SECTION_RESOURCES) {
                    asMap(it) + manager.generateResources()
                },
                params,
            )
        }
    }
}
