/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.passwordpolicy.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.json.JSONObject
import software.amazon.awssdk.services.iam.model.DeleteAccountPasswordPolicyRequest
import software.amazon.awssdk.services.iam.model.GetAccountPasswordPolicyRequest
import software.amazon.awssdk.services.iam.model.GetAccountPasswordPolicyResponse
import software.amazon.awssdk.services.iam.model.UpdateAccountPasswordPolicyRequest

/**
 * Account password policy resource description.
 */
class ResourceModel {
    /**
     * Physical resource identifier.
     */
    @JsonProperty("PhysicalId")
    var physicalId: String? = null

    /**
     * Minimum user password.
     */
    @JsonProperty("MinimumPasswordLength")
    var minimumPasswordLength: Int? = null

    /**
     * Lowercase characters requirement.
     */
    @JsonProperty("RequireLowercaseCharacters")
    var requireLowercaseCharacters: Boolean? = null

    /**
     * Uppercase characters requirement.
     */
    @JsonProperty("RequireUppercaseCharacters")
    var requireUppercaseCharacters: Boolean? = null

    /**
     * Numeric characters requirement.
     */
    @JsonProperty("RequireNumbers")
    var requireNumbers: Boolean? = null

    /**
     * Non-alphanumeric characters requirement.
     */
    @JsonProperty("RequireSymbols")
    var requireSymbols: Boolean? = null

    /**
     * Allowing users to change their passwords.
     */
    @JsonProperty("AllowUsersToChangePassword")
    var allowUsersToChangePassword: Boolean? = null

    /**
     * Number of previous password that are checked.
     */
    @JsonProperty("PasswordReusePrevention")
    var passwordReusePrevention: Int? = null

    /**
     * Maximum password age days.
     */
    @JsonProperty("MaxPasswordAge")
    var maxPasswordAge: Int? = null

    /**
     * Hard expiration flag.
     */
    @JsonProperty("HardExpiry")
    var hardExpiry: Boolean? = null

    /**
     * Primary identifier of the resource.
     */
    @get:JsonIgnore
    val primaryIdentifier: JSONObject?
        get() {
            val identifier = JSONObject()

            if (physicalId != null) {
                identifier.put(IDENTIFIER_KEY_PHYSICAL_ID, physicalId)
            }

            // only return the identifier if it can be used, i.e. if all components are present
            return if (identifier.isEmpty) null else identifier
        }

    /**
     * All the unique identifiers.
     */
    @get:JsonIgnore
    val additionalIdentifiers: List<Any>? = null

    /**
     * Set of resource-related constants.
     */
    companion object {
        /**
         * CloudFormation resource type.
         */
        @JsonIgnore
        val TYPE_NAME = "WrzasqPl::AWS::PasswordPolicy"

        /**
         * Property path to physical ID.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_PHYSICAL_ID = "/properties/PhysicalId"
    }
}

/**
 * Request to read a resource.
 *
 * @return AWS service request to read a resource.
 */
fun ResourceModel.toReadRequest(): GetAccountPasswordPolicyRequest = GetAccountPasswordPolicyRequest.builder()
    .build()

/**
 * Request to create a resource.
 *
 * @return AWS service request to create a resource.
 */
fun ResourceModel.toCreateRequest(): UpdateAccountPasswordPolicyRequest = UpdateAccountPasswordPolicyRequest.builder()
    .minimumPasswordLength(minimumPasswordLength)
    .requireLowercaseCharacters(requireLowercaseCharacters)
    .requireUppercaseCharacters(requireUppercaseCharacters)
    .requireNumbers(requireNumbers)
    .requireSymbols(requireSymbols)
    .allowUsersToChangePassword(allowUsersToChangePassword)
    .passwordReusePrevention(passwordReusePrevention)
    .maxPasswordAge(maxPasswordAge)
    .hardExpiry(hardExpiry)
    .build()

/**
 * Request to delete a resource.
 *
 * @return AWS service request to delete a resource.
 */
fun ResourceModel.toDeleteRequest(): DeleteAccountPasswordPolicyRequest = DeleteAccountPasswordPolicyRequest.builder()
    .build()

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse The AWS service describe resource response.
 * @return Resource model.
 */
fun fromReadResponse(describeResponse: GetAccountPasswordPolicyResponse) = ResourceModel().apply {
    physicalId = "password-policy"
    minimumPasswordLength = describeResponse.passwordPolicy().minimumPasswordLength()
    requireLowercaseCharacters = describeResponse.passwordPolicy().requireLowercaseCharacters()
    requireUppercaseCharacters = describeResponse.passwordPolicy().requireUppercaseCharacters()
    requireNumbers = describeResponse.passwordPolicy().requireNumbers()
    requireSymbols = describeResponse.passwordPolicy().requireSymbols()
    allowUsersToChangePassword = describeResponse.passwordPolicy().allowUsersToChangePassword()
    passwordReusePrevention = describeResponse.passwordPolicy().passwordReusePrevention()
    maxPasswordAge = describeResponse.passwordPolicy().maxPasswordAge()
    hardExpiry = describeResponse.passwordPolicy().hardExpiry()
}
