/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.devicefarmproject.action

import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.resource.aws.devicefarmproject.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.devicefarmproject.model.ResourceModel
import pl.wrzasq.cform.resource.aws.devicefarmproject.model.toReadRequest
import pl.wrzasq.cform.resource.aws.devicefarmproject.model.toUpdateRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.devicefarm.model.NotFoundException
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnNotFoundException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Resource modification handler.
 *
 * @param factory Dependent resource factory.
 * @param readHandler Resource reading handler.
 */
class UpdateHandler(
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
            // step 1 - check if resource already exists
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-DeviceFarmProject::Update::PreExistanceCheck",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toReadRequest)
                    .makeServiceCall { awsRequest, client ->
                        client.injectCredentialsAndInvokeV2(
                            awsRequest,
                            client.client()::getTestGridProject
                        ).also {
                            logger.log("${ResourceModel.TYPE_NAME} has successfully been read.")
                        }
                    }
                    .handleError { _, exception, _, _, _ ->
                        if (exception is NotFoundException) {
                            throw CfnNotFoundException(ResourceModel.TYPE_NAME, it.resourceModel.arn, exception)
                        } else {
                            throw exception
                        }
                    }
                    .progress()
            }
            // step 2 - create/stabilize progress chain - required for resource creation
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-DeviceFarmProject::Update",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toUpdateRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(
                                awsRequest,
                                client.client()::updateTestGridProject
                            ).also {
                                logger.log("${ResourceModel.TYPE_NAME} successfully updated.")
                            }
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .progress()
            }
            // step 3 - return the resource model
            .then { readHandler.handleRequest(proxy, request, callbackContext, logger) }
    }
}
