/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.account.action

import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.resource.aws.account.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.account.model.ResourceModel
import pl.wrzasq.cform.resource.aws.account.model.fromReadResponse
import pl.wrzasq.cform.resource.aws.account.model.toReadParentsRequest
import pl.wrzasq.cform.resource.aws.account.model.toReadRequest
import pl.wrzasq.cform.resource.aws.account.model.toReadTagsRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.organizations.model.ListParentsResponse
import software.amazon.awssdk.services.organizations.model.ListTagsForResourceResponse
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
        val desiredState = requireNotNull(request.desiredResourceState)

        return proxy.initiate(
            "WrzasqPl-AWS-Account::Read",
            proxyClient,
            desiredState,
            callbackContext
        )
            // step 1 - construct a body of a request
            .translateToServiceRequest { model ->
                Triple(model.toReadRequest(), model.toReadParentsRequest(), model.toReadTagsRequest())
            }
            // step 2 - make an API call
            .makeServiceCall { awsRequest, client ->
                try {
                    Triple(
                        client.injectCredentialsAndInvokeV2(
                            awsRequest.first,
                            client.client()::describeAccount
                        ).also {
                            logger.log("${ResourceModel.TYPE_NAME} has successfully been read.")
                        },
                        client.injectCredentialsAndInvokeIterableV2(
                            awsRequest.second,
                            client.client()::listParentsPaginator
                        ).also {
                            logger.log("${ResourceModel.TYPE_NAME} parents have successfully been read.")
                        },
                        client.injectCredentialsAndInvokeIterableV2(
                            awsRequest.third,
                            client.client()::listTagsForResourcePaginator
                        ).also {
                            logger.log("${ResourceModel.TYPE_NAME} tags have successfully been read.")
                        }
                    )
                } catch (error: AwsServiceException) {
                    throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                }
            }
            // step 3 - gather all properties of the resource
            .done { awsResponse ->
                ProgressEvent.defaultSuccessHandler(
                    fromReadResponse(
                        awsResponse.first,
                        awsResponse.second.flatMap(ListParentsResponse::parents),
                        awsResponse.third.flatMap(ListTagsForResourceResponse::tags),
                        desiredState
                    )
                )
            }
    }
}
