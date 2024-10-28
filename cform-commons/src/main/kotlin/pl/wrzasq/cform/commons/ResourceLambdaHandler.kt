/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.commons

import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.commons.config.Configuration
import software.amazon.awssdk.regions.PartitionMetadata
import software.amazon.awssdk.regions.Region
import software.amazon.cloudformation.Action
import software.amazon.cloudformation.LambdaWrapper
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerRequest
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Generic handler for Lambda invocations.
 *
 * @param configuration Handler configuration.
 * @param handlers Mapping of action handlers.
 */
abstract class ResourceLambdaHandler<ResourceType, ConfigurationType>(
    private val configuration: Configuration,
    private val handlers: Map<Action, ActionHandler<ResourceType>>,
) : LambdaWrapper<ResourceType?, StdCallbackContext, ConfigurationType>() {
    override fun provideResourceSchemaJSONObject() = configuration.resourceSchema

    override fun provideResourceDefinedTags(resourceModel: ResourceType?): Map<String, String>? = null

    override fun transform(
        request: HandlerRequest<ResourceType?, StdCallbackContext, ConfigurationType>,
    ): ResourceHandlerRequest<ResourceType?> {
        val requestData = request.requestData
        return ResourceHandlerRequest.builder<ResourceType?>()
            .clientRequestToken(request.bearerToken)
            .desiredResourceState(requestData.resourceProperties)
            .previousResourceState(requestData.previousResourceProperties)
            .desiredResourceTags(getDesiredResourceTags(request))
            .systemTags(request.requestData.systemTags)
            .awsAccountId(request.awsAccountId)
            .logicalResourceIdentifier(request.requestData.logicalResourceId)
            .nextToken(request.nextToken)
            .region(request.region)
            .awsPartition(PartitionMetadata.of(Region.of(request.region)).id())
            .build()
    }

    override fun invokeHandler(
        proxy: AmazonWebServicesClientProxy?,
        request: ResourceHandlerRequest<ResourceType?>,
        action: Action?,
        callbackContext: StdCallbackContext?,
        typeConfiguration: ConfigurationType,
    ): ProgressEvent<ResourceType?, StdCallbackContext> {
        requireNotNull(proxy)
        val actionName = action.toString()
        val handler = handlers[action] ?: throw RuntimeException("Unknown action $actionName")
        loggerProxy.log("[$actionName] invoking handler…")

        return handler.handleRequest(
            proxy,
            request,
            callbackContext ?: StdCallbackContext(),
            loggerProxy,
        ).also {
            loggerProxy.log("[$actionName] handler invoked")
        }
    }
}
