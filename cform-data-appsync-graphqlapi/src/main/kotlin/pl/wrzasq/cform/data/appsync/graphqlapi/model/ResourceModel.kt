/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.data.appsync.graphqlapi.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import software.amazon.awssdk.services.appsync.model.GetGraphqlApiRequest
import software.amazon.awssdk.services.appsync.model.GetGraphqlApiResponse

private const val APPSYNC_API_URI_PREFIX = "https://"
private const val APPSYNC_API_URI_SUFFIX = "/graphql"

/**
 * AWS AppSync GraphQl API resource description.
 */
class ResourceModel {
    /**
     * API ID.
     */
    @JsonProperty("ApiId")
    var apiId: String? = null

    /**
     * Domain name.
     */
    @JsonProperty("DomainName")
    var domainName: String? = null

    /**
     * Primary identifier of the resource.
     */
    @get:JsonIgnore
    val primaryIdentifier: JSONObject?
        get() {
            val identifier = JSONObject()

            if (apiId != null) {
                identifier.put(IDENTIFIER_KEY_API_ID, apiId)
            }

            // only return the identifier if it can be used, i.e. if all components are present
            return if (identifier.isEmpty) null else identifier
        }

    /**
     * All of the unique identifiers.
     */
    @get:JsonIgnore
    val additionalIdentifiers: List<Any>? = null

    companion object {
        /**
         * CloudFormation resource type.
         */
        @JsonIgnore
        val TYPE_NAME = "WrzasqPl::AppSync::GraphQlApiData"

        /**
         * Property path to API ID.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_API_ID = "/properties/ApiId"
    }
}

/**
 * Request to read a resource.
 *
 * @return AWS service request to read a resource.
 */
fun ResourceModel.toReadRequest(): GetGraphqlApiRequest = GetGraphqlApiRequest.builder()
    .apiId(apiId)
    .build()

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse The AWS service describe resource response.
 * @return Resource model.
 */
fun fromReadResponse(describeResponse: GetGraphqlApiResponse) = ResourceModel().apply {
    apiId = describeResponse.graphqlApi().apiId()
    domainName = extractDomainFromUri(describeResponse.graphqlApi().uris()["GRAPHQL"]) ?: ""
}

private fun extractDomainFromUri(uri: String?) = uri?.substring(
    APPSYNC_API_URI_PREFIX.length,
    uri.length - APPSYNC_API_URI_SUFFIX.length
)
