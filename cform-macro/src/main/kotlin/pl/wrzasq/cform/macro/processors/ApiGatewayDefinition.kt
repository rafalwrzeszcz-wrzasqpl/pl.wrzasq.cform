/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors

import pl.wrzasq.cform.macro.apigateway.ApiGatewayManager
import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.CallsExpander
import pl.wrzasq.cform.macro.template.SECTION_RESOURCES
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.build
import pl.wrzasq.cform.macro.template.mapSelected
import pl.wrzasq.cform.macro.template.popProperty

/**
 * Simplified API Gateway structure definition.
 */
class ApiGatewayDefinition {
    /**
     * Handles input template.
     *
     * @param input Current template state.
     * @return Processed template.
     */
    fun process(input: Map<String, Any>): Map<String, Any> {
        val manager = ApiGatewayManager()
        val rest = input.popProperty("RestApis", {
            for ((id, api) in asMap(it)) {
                manager.buildApi(id, asMap(api))
            }
        })

        // avoid unnecessary work and resources
        return if (manager.isEmpty()) {
            input
        } else {
            val callsExpander = CallsExpander(manager)

            // take rest of the template and append all of the generated API resources
            callsExpander.processTemplate(
                rest.mapSelected(SECTION_RESOURCES) {
                    asMap(it) + manager.generateResources().map(ResourceDefinition::build)
                }
            )
        }
    }
}
