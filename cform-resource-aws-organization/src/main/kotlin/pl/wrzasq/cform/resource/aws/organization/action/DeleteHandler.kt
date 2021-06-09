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
import pl.wrzasq.cform.resource.aws.organization.model.toDeleteRequest
import pl.wrzasq.cform.resource.aws.organization.model.toReadRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnResourceConflictException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Resource deletion handler.
 *
 * @param factory Dependent resource factory.
 */
class DeleteHandler(
    private val factory: ResourcesFactory
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
                    "WrzasqPl-AWS-Organization::Delete::PreDeletionCheck",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toReadRequest)
                    .makeServiceCall { awsRequest, client ->
                        val response = client.injectCredentialsAndInvokeV2(
                            awsRequest,
                            client.client()::describeOrganization
                        ).also {
                            logger.log("${ResourceModel.TYPE_NAME} has successfully been read.")
                        }

                        val realId = response.organization().id()
                        val physicalId = it.resourceModel.id

                        if (realId != physicalId) {
                            throw CfnResourceConflictException(
                                ResourceModel.TYPE_NAME,
                                request.logicalResourceIdentifier,
                                "Can not delete Organization - ID $realId doesn't match CloudFormation-provided" +
                                    " resource ID ${physicalId}."
                            )
                        }
                    }
                    .progress()
            }
            // step 2 - delete/stabilize progress chain - required for resource deletion
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-Organization::Delete",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toDeleteRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(awsRequest, client.client()::deleteOrganization).also {
                                logger.log("${ResourceModel.TYPE_NAME} successfully deleted.")
                            }
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .progress()
            }
            .then { ProgressEvent.defaultSuccessHandler(null) }
    }
}
