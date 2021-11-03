/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.apigateway

import pl.wrzasq.cform.macro.model.ResourceDefinition

/**
 * API Gateway resource definition.
 *
 * @param api Container API.
 * @param id Resource logical ID.
 * @param parent Parent resource reference.
 * @param path Current scope part.
 */
class ApiResource(
    private val api: ApiGateway,
    val id: String,
    private val parent: Any,
    private val path: String
) : ApiTemplateResource {
    override val resourceId = "${api.resourceId}Resource$id"

    /**
     * Builds resource definition.
     *
     * @return Resource model.
     */
    fun generateResource() = ResourceDefinition(
        id = resourceId,
        type = "AWS::ApiGateway::Resource",
        properties = mapOf(
            "RestApiId" to api.ref(),
            "ParentId" to parent,
            "PathPart" to path
        )
    )
}
