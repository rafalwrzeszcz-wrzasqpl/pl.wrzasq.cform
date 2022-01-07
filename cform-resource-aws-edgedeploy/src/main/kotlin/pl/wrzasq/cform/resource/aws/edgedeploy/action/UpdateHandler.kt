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
import pl.wrzasq.cform.resource.aws.edgedeploy.model.fromReadResponse
import pl.wrzasq.cform.resource.aws.edgedeploy.model.toPublishVersionRequest
import pl.wrzasq.cform.resource.aws.edgedeploy.model.toReadConfigurationRequest
import pl.wrzasq.cform.resource.aws.edgedeploy.model.toReadRequest
import pl.wrzasq.cform.resource.aws.edgedeploy.model.toUpdateCodeRequest
import pl.wrzasq.cform.resource.aws.edgedeploy.model.toUpdateRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.LastUpdateStatus
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException
import software.amazon.awssdk.services.lambda.model.State
import software.amazon.awssdk.services.lambda.model.TagResourceRequest
import software.amazon.awssdk.services.lambda.model.UntagResourceRequest
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnNotFoundException
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.CallChain
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

/**
 * Resource modification handler.
 *
 * @param factory Dependent resource factory.
 * @param objectMapper JSON serializer.
 * @param readHandler Resource reading handler.
 */
class UpdateHandler(
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
            // step 1 - check if resource already exists
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-EdgeDeploy::Update::PreExistanceCheck",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toReadRequest)
                    .makeServiceCall { awsRequest, client ->
                        client.injectCredentialsAndInvokeV2(
                            awsRequest,
                            client.client()::getFunction
                        ).also { response ->
                            logger.log("${ResourceModel.TYPE_NAME} has successfully been read.")
                            request.previousResourceState = fromReadResponse(response, emptyList(), it.resourceModel)
                        }
                    }
                    .handleError { _, exception, _, _, _ ->
                        if (exception is ResourceNotFoundException) {
                            throw CfnNotFoundException(ResourceModel.TYPE_NAME, it.resourceModel.name, exception)
                        } else {
                            throw exception
                        }
                    }
                    .progress()
            }
            // step 2 - update code
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-EdgeDeploy::Update::Code",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toUpdateCodeRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(
                                awsRequest.copy { copy ->
                                    copy.zipFile(
                                        SdkBytes.fromByteArray(it.resourceModel.buildPackage(proxyS3, objectMapper))
                                    )
                                },
                                client.client()::updateFunctionCode
                            ).also {
                                logger.log("${ResourceModel.TYPE_NAME} code successfully updated.")
                            }
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .wait(logger)
                    .progress()
            }
            // step 3 - update function configuration
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-EdgeDeploy::Update",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toUpdateRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(
                                awsRequest,
                                client.client()::updateFunctionConfiguration
                            ).also {
                                logger.log("${ResourceModel.TYPE_NAME} successfully updated.")
                            }
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .wait(logger)
                    .progress()
            }
            // step 4 - manage tags
            .then {
                if (request.desiredResourceTags == request.previousResourceTags) {
                    it
                } else {
                    val remove = request.previousResourceTags.keys - request.desiredResourceTags.keys

                    proxy.initiate(
                        "WrzasqPl-AWS-EdgeDeploy::Update::Tag",
                        proxyClient,
                        it.resourceModel,
                        it.callbackContext
                    )
                        .translateToServiceRequest { model ->
                            Pair(
                                if (remove.isEmpty()) null else {
                                    UntagResourceRequest.builder()
                                        .resource(model.name)
                                        .tagKeys(remove)
                                        .build()
                                },
                                TagResourceRequest.builder()
                                    .resource(model.name)
                                    .tags(request.desiredResourceTags)
                                    .build()
                            )
                        }
                        .makeServiceCall { awsRequest, client ->
                            try {
                                Pair(
                                    awsRequest.first?.let { first ->
                                        client.injectCredentialsAndInvokeV2(
                                            first,
                                            client.client()::untagResource
                                        ).also {
                                            logger.log("${ResourceModel.TYPE_NAME} successfully removed tags.")
                                        }
                                    },
                                    client.injectCredentialsAndInvokeV2(
                                        awsRequest.second,
                                        client.client()::tagResource
                                    ).also {
                                        logger.log("${ResourceModel.TYPE_NAME} successfully saved tags.")
                                    }
                                )
                            } catch (error: AwsServiceException) {
                                throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                            }
                        }
                        .progress()
                }
            }
            // step 5 - publish new function version
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-EdgeDeploy::Update::Publish",
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
            // step 6 - return the resource model
            .then { readHandler.handleRequest(proxy, request, callbackContext, logger) }
    }
}

/**
 * Waits for stabilization of current function setup.
 *
 * @param <RequestType> Type of request structure.
 * @param <ResponseType> Type of response structure.
 * @param logger Logger.
 */
fun <RequestType, ResponseType> CallChain.Stabilizer<
    RequestType,
    ResponseType,
    LambdaClient,
    ResourceModel,
    StdCallbackContext
>.wait(
    logger: Logger
): CallChain.Exceptional<
    RequestType,
    ResponseType,
    LambdaClient,
    ResourceModel,
    StdCallbackContext
> = stabilize { _, _, client, model, _ ->
    val configuration = client.injectCredentialsAndInvokeV2(
        model.toReadConfigurationRequest(),
        client.client()::getFunctionConfiguration
    )

    when (configuration.state()) {
        State.ACTIVE -> when (configuration.lastUpdateStatus()) {
            LastUpdateStatus.SUCCESSFUL -> true
            LastUpdateStatus.IN_PROGRESS -> false
            else -> {
                logger.log(
                    "Failed to update function code ${model.name} - status: ${configuration.state()}."
                )
                throw CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.name)
            }
        }
        State.PENDING -> false
        else -> {
            logger.log(
                "Failed to update function ${model.name} - status: ${configuration.state()}."
            )
            throw CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.name)
        }
    }
}
