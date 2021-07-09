/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.account.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import pl.wrzasq.cform.commons.model.Tag
import software.amazon.awssdk.services.organizations.model.CreateAccountRequest
import software.amazon.awssdk.services.organizations.model.DescribeAccountRequest
import software.amazon.awssdk.services.organizations.model.DescribeAccountResponse
import software.amazon.awssdk.services.organizations.model.HandshakeParty
import software.amazon.awssdk.services.organizations.model.HandshakePartyType
import software.amazon.awssdk.services.organizations.model.InviteAccountToOrganizationRequest
import software.amazon.awssdk.services.organizations.model.ListParentsRequest
import software.amazon.awssdk.services.organizations.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.organizations.model.MoveAccountRequest
import software.amazon.awssdk.services.organizations.model.Parent
import software.amazon.awssdk.services.organizations.model.RemoveAccountFromOrganizationRequest
import software.amazon.awssdk.services.organizations.model.Tag as AwsTag

/**
 * AWS account resource description.
 */
class ResourceModel {
    /**
     * Account ID.
     */
    @JsonProperty("Id")
    var id: String? = null

    /**
     * Account ARN.
     */
    @JsonProperty("Arn")
    var arn: String? = null

    /**
     * Account name.
     */
    @JsonProperty("Name")
    var name: String? = null

    /**
     * Administrative e-mail address.
     */
    @JsonProperty("Email")
    var email: String? = null

    /**
     * Management role name.
     */
    @JsonProperty("AdministratorRoleName")
    var administratorRoleName: String? = null

    /**
     * Containing OU ID.
     */
    @JsonProperty("OuId")
    var ouId: String? = null

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
        val TYPE_NAME = "WrzasqPl::AWS::Account"

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
fun ResourceModel.toReadRequest(): DescribeAccountRequest = DescribeAccountRequest.builder()
    .accountId(id)
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
fun ResourceModel.toCreateRequest(): CreateAccountRequest = CreateAccountRequest.builder()
    .email(email)
    .accountName(name)
    .roleName(administratorRoleName)
    .build()

/**
 * Request to invite a resource.
 *
 * @return AWS service request to invite an account.
 */
fun ResourceModel.toInviteRequest(): InviteAccountToOrganizationRequest = InviteAccountToOrganizationRequest.builder()
    .target(
        HandshakeParty.builder()
            .type(HandshakePartyType.ACCOUNT)
            .id(id)
            .build()
    )
    .build()

/**
 * Request to update a resource.
 *
 * @param parentId Current parent ID.
 * @return AWS service request to update a resource.
 */
fun ResourceModel.toUpdateRequest(parentId: String): MoveAccountRequest = MoveAccountRequest.builder()
    .accountId(id)
    .sourceParentId(parentId)
    .destinationParentId(ouId)
    .build()

/**
 * Request to delete a resource.
 *
 * @return AWS service request to delete a resource.
 */
fun ResourceModel.toDeleteRequest(): RemoveAccountFromOrganizationRequest =
    RemoveAccountFromOrganizationRequest.builder()
        .accountId(id)
        .build()

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse AWS service describe resource response.
 * @param listParentsResponse AWS service resource parents response.
 * @param listTagsResponse AWS service list tags response.
 * @param model Input model for identity properties.
 * @return Resource model.
 */
fun fromReadResponse(
    describeResponse: DescribeAccountResponse,
    listParentsResponse: Collection<Parent>,
    listTagsResponse: Collection<AwsTag>,
    model: ResourceModel
) = ResourceModel().apply {
    id = describeResponse.account().id()
    arn = describeResponse.account().arn()
    name = describeResponse.account().name()
    email = describeResponse.account().email()
    administratorRoleName = model.administratorRoleName
    ouId = listParentsResponse.first().id()
    tags = listTagsResponse.map {
        Tag().apply {
            key = it.key()
            value = it.value()
        }
    }
}
