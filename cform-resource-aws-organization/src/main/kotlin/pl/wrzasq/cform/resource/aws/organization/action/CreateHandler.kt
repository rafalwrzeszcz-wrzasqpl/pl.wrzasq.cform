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
import pl.wrzasq.cform.resource.aws.organization.model.toCreateRequest
import pl.wrzasq.cform.resource.aws.organization.model.toReadRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.organizations.model.AlreadyInOrganizationException
import software.amazon.awssdk.services.organizations.model.AwsOrganizationsNotInUseException
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException
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
 * @param readHandler Resource reading handler.
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
            // step 1 - check if resource already exists
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-Organization::Create::PreExistanceCheck",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toReadRequest)
                    .makeServiceCall { awsRequest, client ->
                        client.injectCredentialsAndInvokeV2(
                            awsRequest,
                            client.client()::describeOrganization
                        ).also { response ->
                            logger.log("${ResourceModel.TYPE_NAME} has successfully been read.")
                            throw CfnAlreadyExistsException(ResourceModel.TYPE_NAME, response.organization().id())
                        }
                    }
                    .handleError { _, exception, _, model, context ->
                        if (exception is AwsOrganizationsNotInUseException) {
                            ProgressEvent.progress(model, context)
                        } else {
                            throw exception
                        }
                    }
                    .progress()
            }
            // step 2 - create/stabilize progress chain - required for resource creation
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-Organization::Create",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toCreateRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(awsRequest, client.client()::createOrganization).also {
                                logger.log("${ResourceModel.TYPE_NAME} successfully created.")
                            }
                        } catch (error: AlreadyInOrganizationException) {
                            null
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
