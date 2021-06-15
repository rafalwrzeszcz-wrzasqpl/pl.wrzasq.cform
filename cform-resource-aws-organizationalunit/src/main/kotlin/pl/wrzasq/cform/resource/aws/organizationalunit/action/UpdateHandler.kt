/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package pl.wrzasq.cform.resource.aws.organizationalunit.action

import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.resource.aws.organizationalunit.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.organizationalunit.model.ResourceModel
import pl.wrzasq.cform.resource.aws.organizationalunit.model.toReadRequest
import pl.wrzasq.cform.resource.aws.organizationalunit.model.toUpdateRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.organizations.model.DuplicateOrganizationalUnitException
import software.amazon.awssdk.services.organizations.model.OrganizationalUnitNotFoundException
import software.amazon.awssdk.services.organizations.model.TagResourceRequest
import software.amazon.awssdk.services.organizations.model.UntagResourceRequest
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnNotFoundException
import software.amazon.cloudformation.exceptions.CfnResourceConflictException
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
                    "WrzasqPl-AWS-OrganizationalUnit::Update::PreExistanceCheck",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toReadRequest)
                    .makeServiceCall { awsRequest, client ->
                        client.injectCredentialsAndInvokeV2(
                            awsRequest,
                            client.client()::describeOrganizationalUnit
                        ).also {
                            logger.log("${ResourceModel.TYPE_NAME} has successfully been read.")
                        }
                    }
                    .handleError { _, exception, _, _, _ ->
                        if (exception is OrganizationalUnitNotFoundException) {
                            throw CfnNotFoundException(ResourceModel.TYPE_NAME, it.resourceModel.id, exception)
                        } else {
                            throw exception
                        }
                    }
                    .progress()
            }
            // step 2 - create/stabilize progress chain - required for resource creation
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-OrganizationalUnit::Update",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toUpdateRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(
                                awsRequest,
                                client.client()::updateOrganizationalUnit
                            ).also {
                                logger.log("${ResourceModel.TYPE_NAME} successfully updated.")
                            }
                        } catch (error: DuplicateOrganizationalUnitException) {
                            throw CfnResourceConflictException(
                                ResourceModel.TYPE_NAME,
                                it.resourceModel.id,
                                "Organizational unit with name ${it.resourceModel.name} already exists.",
                                error
                            )
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .progress()
            }
            // step 3 - manage tags
            .then {
                if (request.desiredResourceTags == request.previousResourceTags) {
                    it
                } else {
                    val remove = request.previousResourceTags.keys - request.desiredResourceTags.keys

                    proxy.initiate(
                        "WrzasqPl-AWS-OrganizationalUnit::Update::Tag",
                        proxyClient,
                        it.resourceModel,
                        it.callbackContext
                    )
                        .translateToServiceRequest { model ->
                            Pair(
                                if (remove.isEmpty()) null else {
                                    UntagResourceRequest.builder()
                                        .resourceId(model.id)
                                        .tagKeys(remove)
                                        .build()
                                },
                                TagResourceRequest.builder()
                                    .resourceId(model.id)
                                    .tags(convertTagsToAwsModel(request.desiredResourceTags))
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
            // step 4 - return the resource model
            .then { readHandler.handleRequest(proxy, request, callbackContext, logger) }
    }
}
