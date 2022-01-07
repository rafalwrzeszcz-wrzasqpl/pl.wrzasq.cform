/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.edgedeploy.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.json.JSONObject
import pl.wrzasq.cform.commons.model.Tag
import pl.wrzasq.cform.resource.aws.edgedeploy.zip.toEdgePackage
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse
import software.amazon.awssdk.services.lambda.model.ListVersionsByFunctionRequest
import software.amazon.awssdk.services.lambda.model.PublishVersionRequest
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeRequest
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.ProxyClient

private const val DEFAULT_CONFIG_FILE = "config.json"

/**
 * AWS Lambda function resource description.
 */
class ResourceModel {
    /**
     * Function name.
     */
    @JsonProperty("Name")
    var name: String? = null

    /**
     * Function ARN.
     */
    @JsonProperty("Arn")
    var arn: String? = null

    /**
     * Lambda function description.
     */
    @JsonProperty("Description")
    var description: String? = null

    /**
     * ARN of Lambda execution role.
     */
    @JsonProperty("RoleArn")
    var roleArn: String? = null

    /**
     * Runtime for running the Lambda (note that Lambda@Edge has reduced set of supported runtimes).
     */
    @JsonProperty("Runtime")
    var runtime: String? = null

    /**
     * Lambda entry point.
     */
    @JsonProperty("Handler")
    var handler: String? = null

    /**
     * Memory size (in MB) for the Lambda.
     */
    @JsonProperty("Memory")
    var memory: Int? = null

    /**
     * Lambda timeout (in seconds).
     */
    @JsonProperty("Timeout")
    var timeout: Int? = null

    /**
     * Package S3 bucket.
     */
    @JsonProperty("PackageBucket")
    var packageBucket: String? = null

    /**
     * Package S3 key.
     */
    @JsonProperty("PackageKey")
    var packageKey: String? = null

    /**
     * Filename for the injected configuration.
     */
    @JsonProperty("ConfigFile")
    var configFile: String = DEFAULT_CONFIG_FILE

    /**
     * Custom configuration to bundle with the package.
     */
    @JsonProperty("Config")
    var config: Map<String, Any> = emptyMap()

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

            if (name != null) {
                identifier.put(IDENTIFIER_KEY_NAME, name)
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
        val TYPE_NAME = "WrzasqPl::AWS::EdgeDeploy"

        /**
         * Property path to name.
         */
        @JsonIgnore
        val IDENTIFIER_KEY_NAME = "/properties/Name"

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
 * @return AWS service request to read resource properties.
 */
fun ResourceModel.toReadRequest(): GetFunctionRequest = GetFunctionRequest.builder()
    .functionName(name)
    .build()

/**
 * Request to read a resource metadata.
 *
 * @return AWS service request to read resource configuration.
 */
fun ResourceModel.toReadConfigurationRequest()
: GetFunctionConfigurationRequest = GetFunctionConfigurationRequest.builder()
        .functionName(name)
        .build()

/**
 * Request to read function versions.
 *
 * @return AWS service request to read resource versions.
 */
fun ResourceModel.toReadVersionsRequest(): ListVersionsByFunctionRequest = ListVersionsByFunctionRequest.builder()
    .functionName(name)
    .build()

/**
 * Request to create a resource.
 *
 * @return AWS service request to create a resource.
 */
fun ResourceModel.toCreateRequest(): CreateFunctionRequest = CreateFunctionRequest.builder()
    .functionName(name)
    .description(description)
    .runtime(runtime)
    .handler(handler)
    .memorySize(memory)
    .timeout(timeout)
    .role(roleArn)
    .tags(tags?.associate { it.key to it.value })
    .build()

/**
 * Request to publish new version of a resource.
 *
 * @return AWS service request to publish new version.
 */
fun ResourceModel.toPublishVersionRequest(): PublishVersionRequest = PublishVersionRequest.builder()
    .functionName(name)
    .build()

/**
 * Request to update a resource.
 *
 * @return AWS service request to update a resource.
 */
fun ResourceModel.toUpdateRequest(): UpdateFunctionConfigurationRequest = UpdateFunctionConfigurationRequest.builder()
    .functionName(name)
    .description(description)
    .runtime(runtime)
    .handler(handler)
    .memorySize(memory)
    .timeout(timeout)
    .role(roleArn)
    .build()

/**
 * Request to update a resource code.
 *
 * @return AWS service request to update a resource.
 */
fun ResourceModel.toUpdateCodeRequest(): UpdateFunctionCodeRequest =
    UpdateFunctionCodeRequest.builder()
        .functionName(name)
        .build()

/**
 * Request to delete a resource.
 *
 * @return AWS service request to delete a resource.
 */
fun ResourceModel.toDeleteRequest(): DeleteFunctionRequest = DeleteFunctionRequest.builder()
    .functionName(name)
    .build()

/**
 * Builds deployable package.
 *
 * @param s3Proxy Proxy for S3 client.
 * @param objectMapper JSON serializer.
 * @return ZIP package content.
 */
fun ResourceModel.buildPackage(s3Proxy: ProxyClient<S3Client>, objectMapper: ObjectMapper): ByteArray {
    val stream = s3Proxy.injectCredentialsAndInvokeV2InputStream(
        GetObjectRequest.builder()
            .bucket(packageBucket)
            .key(packageKey)
            .build()
    ) { awsRequest ->
        try {
            s3Proxy.client().getObject(awsRequest)
        } catch (error: AwsServiceException) {
            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
        }
    }

    return stream.toEdgePackage(configFile, config, objectMapper)
}

/**
 * Translates resource object from SDK into a resource model.
 *
 * @param describeResponse AWS service describe resource response.
 * @param listVersionsResponse AWS service versions list response.
 * @param model Input model for identity properties.
 * @return Resource model.
 */
fun fromReadResponse(
    describeResponse: GetFunctionResponse,
    listVersionsResponse: Collection<FunctionConfiguration>,
    model: ResourceModel
) = ResourceModel().apply {
    name = describeResponse.configuration().functionName()
    // -1 is lover than any first version
    arn = (listVersionsResponse.maxByOrNull { it.version().toIntOrNull() ?: -1 } ?: describeResponse.configuration())
        .functionArn()
    description = describeResponse.configuration().description()
    roleArn = describeResponse.configuration().role()
    runtime = describeResponse.configuration().runtimeAsString()
    handler = describeResponse.configuration().handler()
    memory = describeResponse.configuration().memorySize()
    timeout = describeResponse.configuration().timeout()
    packageBucket = model.packageBucket
    packageKey = model.packageKey
    tags = describeResponse.tags().map {
        Tag().apply {
            key = it.key
            value = it.value
        }
    }
}
