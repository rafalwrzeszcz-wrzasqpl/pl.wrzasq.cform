/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.apigateway

import pl.wrzasq.cform.macro.model.ResourceDefinition

/**
 * API Gateway request validator definition.
 *
 * @param api Container API.
 * @param id Resource logical ID.
 * @param validateRequestBody Payload validation flag.
 * @param validateParameters Parameters validation flag.
 */
class ApiRequestValidator(
    private val api: ApiGateway,
    val id: String,
    private val validateRequestBody: Boolean,
    private val validateParameters: Boolean,
) : ApiTemplateResource {
    override val resourceId
        get() = "${api.resourceId}Validator$id"

    /**
     * Builds resource definition.
     *
     * @return Resource model.
     */
    fun generateResource() = ResourceDefinition(
        id = resourceId,
        type = "AWS::ApiGateway::RequestValidator",
        properties = mapOf(
            "RestApiId" to api.ref(),
            "ValidateRequestBody" to validateRequestBody,
            "ValidateRequestParameters" to validateParameters,
        ),
    )
}
