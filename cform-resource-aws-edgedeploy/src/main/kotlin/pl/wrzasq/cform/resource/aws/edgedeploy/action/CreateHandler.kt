/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.edgedeploy.action

import com.fasterxml.jackson.databind.ObjectMapper
import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.resource.aws.edgedeploy.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.edgedeploy.model.ResourceModel
import pl.wrzasq.cform.resource.aws.edgedeploy.model.buildPackage
import pl.wrzasq.cform.resource.aws.edgedeploy.model.toCreateRequest
import pl.wrzasq.cform.resource.aws.edgedeploy.model.toPublishVersionRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.model.FunctionCode
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
 * @param objectMapper JSON serializer.
 * @param readHandler Resource fetching handler.
 */
class CreateHandler(
    private val factory: ResourcesFactory,
    private val objectMapper: ObjectMapper,
    private val readHandler: ActionHandler<ResourceModel>
) : ActionHandler<ResourceModel> {
    override fun handleRequest(
        proxy: AmazonWebServicesClientProxy,
        request: ResourceHandlerRequest<ResourceModel?>,
        callbackContext: StdCallbackContext,
        logger: Logger
    ): ProgressEvent<ResourceModel?, StdCallbackContext> {
        val proxyClient = factory.getLambdaClient(proxy)
        val proxyS3 = factory.getS3Client(proxy)

        return ProgressEvent.progress(
            requireNotNull(request.desiredResourceState),
            callbackContext
        )
            // step 1 - create/stabilize progress chain - required for resource creation
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-EdgeDeploy::Create",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toCreateRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(
                                awsRequest.copy { copy ->
                                    copy.code(
                                        FunctionCode.builder()
                                            .zipFile(
                                                SdkBytes.fromByteArray(
                                                    it.resourceModel.buildPackage(proxyS3, objectMapper)
                                                )
                                            )
                                            .build()
                                    )
                                },
                                client.client()::createFunction
                            )
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .wait(logger)
                    .progress()
            }
            // step 2 - publish new function version
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-EdgeDeploy::Create::Publish",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toPublishVersionRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(
                                awsRequest,
                                client.client()::publishVersion
                            )
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .progress()
            }
            // step 4 - return the resource model
            .then { readHandler.handleRequest(proxy, request, callbackContext, logger) }
    }
}
