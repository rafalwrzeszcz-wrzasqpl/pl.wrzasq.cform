/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2023 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.processors

import pl.wrzasq.cform.macro.processors.loggroups.DefaultLogGroupHandler
import pl.wrzasq.cform.macro.template.SECTION_RESOURCES
import pl.wrzasq.cform.macro.template.asDefinition
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.build
import pl.wrzasq.cform.macro.template.mapSelected
import pl.wrzasq.cform.macro.template.popProperty

private val logGroupsHandlers = mapOf(
    "AWS::Lambda::Function" to DefaultLogGroupHandler("lambda"),
    "AWS::CodeBuild::Project" to DefaultLogGroupHandler("codebuild"),
    "AWS::Serverless::Function" to DefaultLogGroupHandler("lambda")
)

/**
 * Automatically declares LogGroups for certain types of resources.
 *
 * Currently Lambda functions (including Serverless transform) and CodeBuild projects are handled.
 */
class AutomaticLogGroups {
    /**
     * Handles input template.
     *
     * @param input Current template state.
     * @param params Template parameter values.
     * @return Processed template.
     */
    fun process(input: Map<String, Any>, params: Map<String, Any>) = input.mapSelected(SECTION_RESOURCES) {
        processResources(asMap(it))
    }

    private fun processResources(input: Map<String, Any>): Map<String, Any> {
        // we keep old resources anyway, will just add more to it
        val resources = mutableMapOf<String, Any>()

        for ((id, resource) in input) {
            val entry = asDefinition(id, resource)
            val handler = logGroupsHandlers[entry.type]

            // check if resource type is handled
            if (handler != null && entry.properties.containsKey(handler.propertyName)) {
                // create copy of old resource but drop the non-standard property
                resources[id] = entry.popProperty(handler.propertyName, {
                    resources += handler.buildLogGroup(id, it).build()
                })
                    .build()
                    .second
            } else {
                // just keep original resource
                resources[id] = resource
            }
        }

        return resources
    }
}
