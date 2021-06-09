/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.organization.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import software.amazon.awssdk.services.organizations.model.CreateOrganizationRequest
import software.amazon.awssdk.services.organizations.model.DeleteOrganizationRequest
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationRequest
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationResponse
import software.amazon.awssdk.services.organizations.model.ListRootsRequest
import software.amazon.awssdk.services.organizations.model.ListRootsResponse

/**
 * AWS Organization resource description.
 */
class ResourceModel {
    /**
     * Organization ID.
     */
    @JsonProperty("Id")
    var id: String? = null

    /**
     * Organization ARN.
     */
    @JsonProperty("Arn")
    var arn: String? = null

    /**
     * Root organization unit ID.
     */
    @JsonProperty("RootId")
    var rootId: String? = null

    /**
     * Enabled feature set.
     */
    @JsonProperty("FeatureSet")
    var featureSet: String? = null

    /**
     * Primary identifier of the resource.
     */
    @get:JsonIgnore
    val primaryIdentifier: JSONObject?
        get() {
            val identifier = JSONObject()

            if (id != null) {
                identifier.put(IDENTIFIER_KEY_ID, id)
            }

            // only return the identifier if it can be used, i.e. if all components are present
            return if (identifier.isEmpty) null else identifier
        }

    /**
     * All of the unique identifiers.
     */
    @get:JsonIgnore
    val additionalIdentifiers: List<Any>?
        get() {
            val identifiers = mutableListOf<JSONObject>()

            if (arn != null) {
                val identifier = JSONObject()
                identifier.put(IDENTIFIER_KEY_ARN, arn)
                identifiers.add(identifier)
            }

            // only return the identifiers if any can be used
            return if (identifiers.isEmpty()) null else identifiers
        }

    companion object {
        /**
         * CloudFormation resource type.
         */
        @JsonIgnore
        val TYPE_NAME = "WrzasqPl::AWS::Organization"

        /**
         * Property path to ID.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_ID = "/properties/Id"

        /**
         * Property path to ARN.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_ARN = "/properties/Arn"
    }
}

/**
 * Request to read a resource.
 *
 * @return AWS service request to read a resource.
 */
fun ResourceModel.toReadRequest(): DescribeOrganizationRequest = DescribeOrganizationRequest.builder()
    .build()

/**
 * Request to read a resource roots.
 *
 * @return AWS service request to read a structure roots.
 */
fun ResourceModel.toReadRootsRequest(): ListRootsRequest = ListRootsRequest.builder()
    .build()

/**
 * Request to create a resource.
 *
 * @return AWS service request to create a resource.
 */
fun ResourceModel.toCreateRequest(): CreateOrganizationRequest = CreateOrganizationRequest.builder()
    .featureSet(featureSet)
    .build()

/**
 * Request to delete a resource.
 *
 * @return AWS service request to delete a resource.
 */
fun ResourceModel.toDeleteRequest(): DeleteOrganizationRequest = DeleteOrganizationRequest.builder()
    .build()

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse The AWS service describe resource response.
 * @param listRootsResponse The AWS service list roots response.
 * @return Resource model.
 */
fun fromReadResponse(
    describeResponse: DescribeOrganizationResponse,
    listRootsResponse: ListRootsResponse
) = ResourceModel().apply {
    id = describeResponse.organization().id()
    arn = describeResponse.organization().arn()
    rootId = listRootsResponse.roots()[0].id()
}
