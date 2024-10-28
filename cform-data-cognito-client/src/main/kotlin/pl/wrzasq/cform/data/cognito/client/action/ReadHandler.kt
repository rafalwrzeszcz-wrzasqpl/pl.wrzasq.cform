/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.data.cognito.client.action

import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.data.cognito.client.config.ResourcesFactory
import pl.wrzasq.cform.data.cognito.client.model.ResourceModel
import pl.wrzasq.cform.data.cognito.client.model.fromReadResponse
import pl.wrzasq.cform.data.cognito.client.model.toReadRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnNotFoundException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Resource fetching handler.
 *
 * @param factory Dependent resource factory.
 */
class ReadHandler(
    private val factory: ResourcesFactory,
) : ActionHandler<ResourceModel> {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel?>,
        callbackContext: StdCallbackContext,
        logger: Logger,
    ): ProgressEvent<ResourceModel?, StdCallbackContext> {
        val proxyClient = factory.getClient(proxy)

        return proxy.initiate(
            "WrzasqPl-Cognito-ClientData::Read",
            proxyClient,
            requireNotNull(request.desiredResourceState),
            callbackContext,
        )
            // step 1 - construct a body of a request
            .translateToServiceRequest(ResourceModel::toReadRequest)
            // step 2 - make an API call
            .makeServiceCall { awsRequest, client ->
                try {
                    client.injectCredentialsAndInvokeV2(
                        awsRequest,
                        client.client()::describeUserPoolClient,
                    ).also {
                        logger.log("${ResourceModel.TYPE_NAME} has successfully been read.")
                    }
                } catch (error: ResourceNotFoundException) {
                    throw CfnNotFoundException(ResourceModel.TYPE_NAME, awsRequest.clientId(), error)
                } catch (error: AwsServiceException) {
                    throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                }
            }
            // step 3 - gather all properties of the resource
            .done { awsResponse ->
                ProgressEvent.defaultSuccessHandler(fromReadResponse(awsResponse))
            }
    }
}
