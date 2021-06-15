/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.organizationalunit.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import pl.wrzasq.cform.commons.model.Tag
import software.amazon.awssdk.services.organizations.model.CreateOrganizationalUnitRequest
import software.amazon.awssdk.services.organizations.model.DeleteOrganizationalUnitRequest
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationalUnitRequest
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationalUnitResponse
import software.amazon.awssdk.services.organizations.model.ListParentsRequest
import software.amazon.awssdk.services.organizations.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.organizations.model.Parent
import software.amazon.awssdk.services.organizations.model.UpdateOrganizationalUnitRequest
import software.amazon.awssdk.services.organizations.model.Tag as AwsTag

/**
 * AWS Organizational Unit resource description.
 */
class ResourceModel {
    /**
     * Organizational unit ID.
     */
    @JsonProperty("Id")
    var id: String? = null

    /**
     * Organizational unit ARN.
     */
    @JsonProperty("Arn")
    var arn: String? = null

    /**
     * Organizational unit name.
     */
    @JsonProperty("Name")
    var name: String? = null

    /**
     * Parent unit (or root) ID.
     */
    @JsonProperty("ParentId")
    var parentId: String? = null

    /**
     * Resource tags.
     */
    @JsonProperty("Tags")
    var tags: List<Tag>? = null

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
        val TYPE_NAME = "WrzasqPl::AWS::OrganizationalUnit"

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
 * @return AWS service request to read resource tgs.
 */
fun ResourceModel.toReadRequest(): DescribeOrganizationalUnitRequest = DescribeOrganizationalUnitRequest.builder()
    .organizationalUnitId(id)
    .build()

/**
 * Request to read parent nodes of a resource.
 *
 * @return AWS service request to read parents.
 */
fun ResourceModel.toReadParentsRequest(): ListParentsRequest = ListParentsRequest.builder()
    .childId(id)
    .build()

/**
 * Request to read tags of a resource.
 *
 * @return AWS service request to read a resource.
 */
fun ResourceModel.toReadTagsRequest(): ListTagsForResourceRequest = ListTagsForResourceRequest.builder()
    .resourceId(id)
    .build()

/**
 * Request to create a resource.
 *
 * @return AWS service request to create a resource.
 */
fun ResourceModel.toCreateRequest(): CreateOrganizationalUnitRequest = CreateOrganizationalUnitRequest.builder()
    .name(name)
    .parentId(parentId)
    .build()

/**
 * Request to update a resource.
 *
 * @return AWS service request to update a resource.
 */
fun ResourceModel.toUpdateRequest(): UpdateOrganizationalUnitRequest = UpdateOrganizationalUnitRequest.builder()
    .organizationalUnitId(id)
    .name(name)
    .build()

/**
 * Request to delete a resource.
 *
 * @return AWS service request to delete a resource.
 */
fun ResourceModel.toDeleteRequest(): DeleteOrganizationalUnitRequest = DeleteOrganizationalUnitRequest.builder()
    .organizationalUnitId(id)
    .build()

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse AWS service describe resource response.
 * @param listParentsResponse AWS service list parents response.
 * @param listTagsResponse AWS service list tags response.
 * @return Resource model.
 */
fun fromReadResponse(
    describeResponse: DescribeOrganizationalUnitResponse,
    listParentsResponse: Collection<Parent>,
    listTagsResponse: Collection<AwsTag>
) = ResourceModel().apply {
    id = describeResponse.organizationalUnit().id()
    arn = describeResponse.organizationalUnit().arn()
    name = describeResponse.organizationalUnit().name()
    parentId = listParentsResponse.first().id()
    tags = listTagsResponse.map {
        Tag().apply {
            key = it.key()
            value = it.value()
        }
    }
}
