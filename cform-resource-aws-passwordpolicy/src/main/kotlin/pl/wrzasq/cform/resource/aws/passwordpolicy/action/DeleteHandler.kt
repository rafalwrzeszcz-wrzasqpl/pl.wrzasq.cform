/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.passwordpolicy.action

import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.resource.aws.passwordpolicy.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.passwordpolicy.model.ResourceModel
import pl.wrzasq.cform.resource.aws.passwordpolicy.model.toDeleteRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.iam.model.NoSuchEntityException
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
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
    private val factory: ResourcesFactory,
) : ActionHandler<ResourceModel> {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel?>,
        callbackContext: StdCallbackContext,
        logger: Logger,
    ): ProgressEvent<ResourceModel?, StdCallbackContext> {
        val proxyClient = factory.getClient(proxy)

        return ProgressEvent.progress(
            requireNotNull(request.desiredResourceState),
            callbackContext,
        )
            // step 1 - delete/stabilize progress chain - required for resource deletion
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-PasswordPolicy::Delete",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext,
                )
                    .translateToServiceRequest(ResourceModel::toDeleteRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(
                                awsRequest,
                                client.client()::deleteAccountPasswordPolicy,
                            ).also {
                                logger.log("${ResourceModel.TYPE_NAME} successfully deleted.")
                            }
                        } catch (error: NoSuchEntityException) {
                            logger.log("Password policy doesn't exist - nothing to delete.")
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .progress()
            }
            .then { ProgressEvent.defaultSuccessHandler(null) }
    }
}
