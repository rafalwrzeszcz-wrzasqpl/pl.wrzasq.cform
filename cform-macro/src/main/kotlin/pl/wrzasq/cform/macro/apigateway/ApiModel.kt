/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.apigateway

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.popProperty

/**
 * API Gateway model definition.
 *
 * @param api Container API.
 * @param id Resource logical ID.
 * @param input Resource specification.
 */
class ApiModel(
    private val api: ApiGateway,
    val id: String,
    input: Map<String, Any>
) : ApiTemplateResource {
    private val properties: Map<String, Any>

    init {
        val computed = mutableMapOf<String, Any>()
        val leftover = input.toMutableMap().apply {
            putIfAbsent("ContentType", "application/json")
        }
            .popProperty("Schema", {
                val schema = asMap(it).toMutableMap()
                if ("\$schema" !in schema) {
                    schema["\$schema"] = "http://json-schema.org/draft-04/schema#"
                }
                if ("title" !in schema) {
                    schema["title"] = id
                }

                computed["Schema"] = schema
            })

        properties = leftover + computed
    }

    override val resourceId
        get() = "${api.resourceId}Model$id"

    /**
     * Builds resource definition.
     *
     * @return Resource model.
     */
    fun generateResource() = ResourceDefinition(
        id = resourceId,
        type = "AWS::ApiGateway::Model",
        properties = properties + mapOf("RestApiId" to api.ref())
    )
}
