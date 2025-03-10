/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.apigateway

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.Fn
import pl.wrzasq.cform.macro.template.asMap
import pl.wrzasq.cform.macro.template.asMapAlways
import pl.wrzasq.cform.macro.template.mapSelected
import pl.wrzasq.cform.macro.template.mapValuesOnly
import pl.wrzasq.cform.macro.template.popProperty

private const val KEY_METHODRESPONSES = "MethodResponses"

/**
 * API Gateway method definition.
 *
 * @param api Container API.
 * @param id Resource logical ID.
 * @param parent Parent resource reference.
 * @param method HTTP method.
 * @param input Resource specification.
 */
class ApiMethod(
    private val api: ApiGateway,
    val id: String,
    private val parent: Any,
    private val method: String,
    input: Map<String, Any>,
) : ApiTemplateResource {
    private val properties: Map<String, Any>

    init {
        val computed = mutableMapOf<String, Any>()
        val leftover = input
            .popProperty("RequestValidator", {
                computed["RequestValidatorId"] = api.getValidator(it.toString()).ref()
            })
            .popProperty("Authorizer", {
                val reference = it.toString()
                val authorizer = checkNotNull(api.authorizers[reference]) {
                    "No authorizer with ID $reference is defined."
                }

                computed["AuthorizerId"] = authorizer.ref()
                computed["AuthorizationType"] = authorizer.authorizationType
            })
            .mapSelected(
                "Integration" to { initIntegration(asMap(it)) },
                KEY_METHODRESPONSES to ::unfoldResponses,
            )

        // at this point responses are already `unfolded`
        if (!leftover.containsKey(KEY_METHODRESPONSES)) {
            asMapAlways(leftover["Integration"])["IntegrationResponses"]?.let {
                computed[KEY_METHODRESPONSES] = buildMethodResponses(it)
            }
        }

        properties = leftover + computed
    }

    override val resourceId
        get() = "${api.resourceId}Method$id"

    /**
     * Builds resource definition.
     *
     * @return Resource model.
     */
    fun generateResource() = ResourceDefinition(
        id = resourceId,
        type = "AWS::ApiGateway::Method",
        properties = properties + mapOf(
            "RestApiId" to api.ref(),
            "ResourceId" to parent,
            "HttpMethod" to method,
        ),
    )

    private fun initIntegration(input: Map<String, Any>): Map<String, Any> {
        val computed = mutableMapOf<String, Any>()
        val leftover = input.toMutableMap().apply {
            // some "sensible defaults"
            putIfAbsent("Type", "AWS")
            putIfAbsent("IntegrationHttpMethod", "POST")
        }
            // common cases simplification
            .popProperty("Lambda", { computed["Uri"] = Fn.wrapSub(it, ::lambdaUri) })
            .mapSelected("IntegrationResponses", ::unfoldResponses)

        return leftover + computed
    }
}

private fun buildMethodResponses(input: Any) = if (input is List<*>) {
    input.map {
        asMapAlways(it)
            .filterKeys { key -> key in setOf("StatusCode", "ResponseParameters") }
            .mapSelected("ResponseParameters") { response ->
                asMap(response).mapValuesOnly { true }
            }
    }
} else {
    input
}

private fun unfoldResponses(input: Any) = if (input is Map<*, *>) {
    asMap(input).toSortedMap().map {
        asMapAlways(it.value) + mapOf("StatusCode" to it.key)
    }
} else {
    input
}
