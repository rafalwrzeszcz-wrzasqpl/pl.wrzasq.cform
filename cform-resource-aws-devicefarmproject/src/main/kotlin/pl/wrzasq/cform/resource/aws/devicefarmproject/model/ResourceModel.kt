/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.devicefarmproject.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import software.amazon.awssdk.services.devicefarm.model.CreateTestGridProjectRequest
import software.amazon.awssdk.services.devicefarm.model.DeleteTestGridProjectRequest
import software.amazon.awssdk.services.devicefarm.model.GetTestGridProjectRequest
import software.amazon.awssdk.services.devicefarm.model.GetTestGridProjectResponse
import software.amazon.awssdk.services.devicefarm.model.UpdateTestGridProjectRequest

/**
 * AWS DeviceFarm TestGrid project resource description.
 */
class ResourceModel {
    /**
     * Project ARN.
     */
    @JsonProperty("Arn")
    var arn: String? = null

    /**
     * Project name.
     */
    @JsonProperty("Name")
    var name: String? = null

    /**
     * Description.
     */
    @JsonProperty("Description")
    var description: String? = null

    /**
     * Primary identifier of the resource.
     */
    @get:JsonIgnore
    val primaryIdentifier: JSONObject?
        get() {
            val identifier = JSONObject()

            if (arn != null) {
                identifier.put(IDENTIFIER_KEY_ARN, arn)
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
        val TYPE_NAME = "WrzasqPl::AWS::DeviceFarmProject"

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
 * @return AWS service request to read resource.
 */
fun ResourceModel.toReadRequest(): GetTestGridProjectRequest = GetTestGridProjectRequest.builder()
    .projectArn(arn)
    .build()

/**
 * Request to create a resource.
 *
 * @return AWS service request to create a resource.
 */
fun ResourceModel.toCreateRequest(): CreateTestGridProjectRequest = CreateTestGridProjectRequest.builder()
    .name(name)
    .description(description)
    .build()

/**
 * Request to update a resource.
 *
 * @return AWS service request to update a resource.
 */
fun ResourceModel.toUpdateRequest(): UpdateTestGridProjectRequest = UpdateTestGridProjectRequest.builder()
    .projectArn(arn)
    .name(name)
    .description(description)
    .build()

/**
 * Request to delete a resource.
 *
 * @return AWS service request to delete a resource.
 */
fun ResourceModel.toDeleteRequest(): DeleteTestGridProjectRequest = DeleteTestGridProjectRequest.builder()
    .projectArn(arn)
    .build()

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse AWS service describe resource response.
 * @return Resource model.
 */
fun fromReadResponse(describeResponse: GetTestGridProjectResponse) = ResourceModel().apply {
    arn = describeResponse.testGridProject().arn()
    name = describeResponse.testGridProject().name()
    description = describeResponse.testGridProject().description()
}
