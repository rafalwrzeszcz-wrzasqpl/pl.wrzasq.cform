/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.data.cognito.client.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientResponse

/**
 * AWS Cognito client resource description.
 */
class ResourceModel {
    /**
     * User pool ID.
     */
    @JsonProperty("UserPoolId")
    var userPoolId: String? = null

    /**
     * Client ID.
     */
    @JsonProperty("ClientId")
    var clientId: String? = null

    /**
     * OAuth client secret.
     */
    @JsonProperty("ClientSecret")
    var clientSecret: String? = null

    /**
     * Primary identifier of the resource.
     */
    @get:JsonIgnore
    val primaryIdentifier: JSONObject?
        get() {
            val identifier = JSONObject()

            if (userPoolId != null) {
                identifier.put(IDENTIFIER_KEY_USER_POOL_ID, userPoolId)
            }
            if (clientId != null) {
                identifier.put(IDENTIFIER_KEY_CLIENT_ID, clientId)
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
        val TYPE_NAME = "WrzasqPl::Cognito::ClientData"

        /**
         * Property path to UserPoolId.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_USER_POOL_ID = "/properties/UserPoolId"

        /**
         * Property path to ClientId.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_CLIENT_ID = "/properties/ClientId"
    }
}

/**
 * Request to read a resource.
 *
 * @return AWS service request to read a resource.
 */
fun ResourceModel.toReadRequest(): DescribeUserPoolClientRequest = DescribeUserPoolClientRequest.builder()
    .userPoolId(userPoolId)
    .clientId(clientId)
    .build()

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse The AWS service describe resource response.
 * @return Resource model.
 */
fun fromReadResponse(describeResponse: DescribeUserPoolClientResponse) = ResourceModel().apply {
    userPoolId = describeResponse.userPoolClient().userPoolId()
    clientId = describeResponse.userPoolClient().clientId()
    clientSecret = describeResponse.userPoolClient().clientSecret()
}
