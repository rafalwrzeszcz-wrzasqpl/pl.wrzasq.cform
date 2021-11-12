/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.macro.apigateway

import pl.wrzasq.cform.macro.model.ResourceDefinition
import pl.wrzasq.cform.macro.template.ExpansionHandler
import pl.wrzasq.cform.macro.template.Fn

// final dot allows to make !GetAtt and !Sub calls to attributes directly in expression
private const val REF_PATTERN = "RestApi:([^.}]+)(\\.[^}]+)?"
private val REF_MATCH_FULL = Regex("^${REF_PATTERN}\$")
private val REF_MATCH_PATTERN = Regex("\\\$\\{${REF_PATTERN}}")

/**
 * Manages API Gateway resources within template scope.
 */
class ApiGatewayManager : ExpansionHandler {
    private val apis: MutableMap<String, ApiGateway> = mutableMapOf()

    /**
     * Builds new API definition.
     *
     * @param id API identifier.
     * @param input API setup.
     */
    fun buildApi(id: String, input: Map<String, Any>) {
        apis[id] = ApiGateway(id, input)
    }

    /**
     * Checks if there is any API defined in current scope.
     *
     * @return State, whether there is any API definition.
     */
    fun isEmpty() = apis.isEmpty()

    /**
     * Generates list of all resources defined in current scope.
     *
     * @return List of resource models.
     */
    fun generateResources(): List<ResourceDefinition> {
        // generic resources
        val role = ResourceDefinition(
            id = "ApiGatewayAccountRole",
            type = "AWS::IAM::Role",
            properties = mapOf(
                "AssumeRolePolicyDocument" to listOf(
                    mapOf(
                        "Action" to listOf("sts:AssumeRole"),
                        "Effect" to "Allow",
                        "Principal" to mapOf(
                            "Service" to listOf("apigateway.amazonaws.com")
                        )
                    )
                ),
                "ManagedPolicyArns" to listOf(
                    "arn:aws:iam::aws:policy/service-role/AmazonAPIGatewayPushToCloudWatchLogs"
                )
            )
        )
        val account = ResourceDefinition(
            id = "ApiAccount",
            type = "AWS::ApiGateway::Account",
            // this is needed only for initial deploy, but doesn't hurt to keep it
            dependsOn = apis.values.map(ApiGateway::resourceId),
            properties = mapOf("CloudWatchRoleArn" to Fn.getAtt(role.id, "Arn"))
        )

        return apis.values.flatMap(ApiGateway::generateResources) + listOf(role, account)
    }

    override fun canHandle(function: String) = function == "Ref" || function == "Fn::Sub" || function == "Fn::GetAtt"

    override fun expand(input: Pair<String, Any>): Map<String, Any> {
        val params = input.second
        // all of these calls are either single-string arguments or two-element list
        val value = if (params is List<*> && params[0] is String) {
            listOf(
                expandString(params[0].toString()),
                params[1]
            )
        } else if (params is String) {
            expandString(params)
        } else {
            params
        }

        return mapOf(input.first to value)
    }

    // if any of the cases match, we don't need further tests
    private fun expandString(input: String) = when {
        REF_MATCH_FULL.matches(input) -> REF_MATCH_FULL.replace(input) { resolve(it.groupValues) }
        REF_MATCH_PATTERN.containsMatchIn(input) -> REF_MATCH_PATTERN.replace(input) {
            "\${${resolve(it.groupValues)}}"
        }
        else -> input
    }

    private fun resolve(match: List<String?>) = "${resolve(match[1] ?: "")}${match[2] ?: ""}"

    private fun resolve(match: String): String {
        // using `:` is safer than `.` - dot is used by existing intrinsic functions like !GetAtt and !Sub and there
        // are cases when nested dots are possible, eg. nested stacks modules etc. - would be super hard to reliably
        // distinguish between our references and other cases
        val parts = match.split(":")
        val apiId = parts.first()
        val api = checkNotNull(apis[apiId]) { "Reference to `$match` of unknown API ID `$apiId`" }

        return api.resolve(parts.drop(1))
    }
}
