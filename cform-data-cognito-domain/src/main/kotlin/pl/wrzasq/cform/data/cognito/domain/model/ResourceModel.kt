/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.data.cognito.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolDomainRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolDomainResponse

/**
 * AWS Cognito domain resource description.
 */
class ResourceModel {
    /**
     * Domain name.
     */
    @JsonProperty("Domain")
    var domain: String? = null

    /**
     * CloudFront distribution DNS name.
     */
    @JsonProperty("CloudFrontDistribution")
    var cloudFrontDistribution: String? = null

    /**
     * Primary identifier of the resource.
     */
    @get:JsonIgnore
    val primaryIdentifier: JSONObject?
        get() {
            val identifier = JSONObject()

            if (domain != null) {
                identifier.put(IDENTIFIER_KEY_DOMAIN, domain)
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
        val TYPE_NAME = "WrzasqPl::Cognito::DomainData"

        /**
         * Property path to Domain.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_DOMAIN = "/properties/Domain"
    }
}

/**
 * Request to read a resource.
 *
 * @return AWS service request to read a resource.
 */
fun ResourceModel.toReadRequest(): DescribeUserPoolDomainRequest = DescribeUserPoolDomainRequest.builder()
    .domain(domain)
    .build()

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse The AWS service describe resource response.
 * @return Resource model.
 */
fun fromReadResponse(describeResponse: DescribeUserPoolDomainResponse) = ResourceModel().apply {
    domain = describeResponse.domainDescription().domain()
    cloudFrontDistribution = describeResponse.domainDescription().cloudFrontDistribution()
}
