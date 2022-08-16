/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.dynamodbitem.action

import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.resource.aws.dynamodbitem.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.dynamodbitem.model.ResourceModel
import pl.wrzasq.cform.resource.aws.dynamodbitem.model.toCreateRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Resource creation handler.
 *
 * @param factory Dependent resource factory.
 * @param readHandler Resource read handler.
 */
class CreateHandler(
    private val factory: ResourcesFactory,
    private val readHandler: ActionHandler<ResourceModel>
) : ActionHandler<ResourceModel> {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel?>,
        callbackContext: StdCallbackContext,
        logger: Logger
    ): ProgressEvent<ResourceModel?, StdCallbackContext> {
        val proxyClient = factory.getClient(proxy)

        return ProgressEvent.progress(
            requireNotNull(request.desiredResourceState),
            callbackContext
        )
            // step 1 - create DB entry
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-DynamoDbItem::Create",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toCreateRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(
                                awsRequest,
                                client.client()::putItem
                            )
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .progress()
            }
            // step 2 - return the resource model
            .then { readHandler.handleRequest(proxy, request, callbackContext, logger) }
    }
}
