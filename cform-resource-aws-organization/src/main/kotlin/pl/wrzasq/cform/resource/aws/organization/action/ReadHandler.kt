/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.organization.action

import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.resource.aws.organization.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.organization.model.ResourceModel
import pl.wrzasq.cform.resource.aws.organization.model.fromReadResponse
import pl.wrzasq.cform.resource.aws.organization.model.toReadRequest
import pl.wrzasq.cform.resource.aws.organization.model.toReadRootsRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
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
    private val factory: ResourcesFactory
) : ActionHandler<ResourceModel> {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel?>,
        callbackContext: StdCallbackContext,
        logger: Logger
    ): ProgressEvent<ResourceModel?, StdCallbackContext> {
        val proxyClient = factory.getClient(proxy)

        return proxy.initiate(
            "WrzasqPl-AWS-Organization::Read",
            proxyClient,
            requireNotNull(request.desiredResourceState),
            callbackContext
        )
            // step 1 - construct a body of a request
            .translateToServiceRequest { model -> Pair(model.toReadRequest(), model.toReadRootsRequest()) }
            // step 2 - make an API call
            .makeServiceCall { awsRequest, client ->
                try {
                    Pair(
                        client.injectCredentialsAndInvokeV2(
                            awsRequest.first,
                            client.client()::describeOrganization
                        ).also {
                            logger.log("${ResourceModel.TYPE_NAME} has successfully been read.")
                        },
                        client.injectCredentialsAndInvokeV2(awsRequest.second, client.client()::listRoots).also {
                            logger.log("${ResourceModel.TYPE_NAME} roots have successfully been read.")
                        }
                    )
                } catch (error: AwsServiceException) {
                    throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                }
            }
            // step 3 - gather all properties of the resource
            .done { awsResponse ->
                ProgressEvent.defaultSuccessHandler(fromReadResponse(awsResponse.first, awsResponse.second))
            }
    }
}
