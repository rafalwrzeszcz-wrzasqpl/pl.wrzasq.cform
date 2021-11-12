/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.apigateway

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.popProperty

/**
 * API Gateway authorizer definition.
 *
 * @param api Container API.
 * @param id Resource logical ID.
 * @param input Resource specification.
 */
class ApiAuthorizer(
    private val api: ApiGateway,
    val id: String,
    input: Map<String, Any>
) : ApiTemplateResource {
    private val properties: Map<String, Any>
    private val type: String

    init {
        val computed = mutableMapOf<String, Any>()
        val leftover = input.toMutableMap().apply {
            // default values
            putIfAbsent("Name", id)
        }
            .popProperty("Lambda", { computed["AuthorizerUri"] = Fn.wrapSub(it, ::lambdaUri) })

        properties = leftover + computed
        type = properties["Type"].toString()
    }

    /**
     * Integration authorization type.
     */
    val authorizationType = if (type == "COGNITO_USER_POOLS") type else "CUSTOM"

    override val resourceId
        get() = "${api.resourceId}Authorizer$id"

    /**
     * Builds resource definition.
     *
     * @return Resource model.
     */
    fun generateResource() = ResourceDefinition(
        id = resourceId,
        type = "AWS::ApiGateway::Authorizer",
        properties = properties + mapOf(
            "RestApiId" to api.ref()
        )
    )
}
