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
import pl.wrzasq.cform.resource.aws.organizationalunit.model.toCreateRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.organizations.model.DuplicateOrganizationalUnitException
import software.amazon.awssdk.services.organizations.model.Tag
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
            // step 1 - create/stabilize progress chain - required for resource creation
            .then {
                proxy.initiate(
                    "WrzasqPl-AWS-OrganizationalUnit::Create",
                    proxyClient,
                    it.resourceModel,
                    it.callbackContext
                )
                    .translateToServiceRequest(ResourceModel::toCreateRequest)
                    .makeServiceCall { awsRequest, client ->
                        try {
                            client.injectCredentialsAndInvokeV2(
                                awsRequest.copy { copy ->
                                    copy.tags(convertTagsToAwsModel(request.desiredResourceTags))
                                },
                                client.client()::createOrganizationalUnit
                            ).also { awsResponse ->
                                logger.log("${ResourceModel.TYPE_NAME} successfully created.")
                                // any more idiomatic way?
                                request.desiredResourceState?.id = awsResponse.organizationalUnit().id()
                            }
                        } catch (error: DuplicateOrganizationalUnitException) {
                            throw CfnAlreadyExistsException(ResourceModel.TYPE_NAME, it.resourceModel.name, error)
                        } catch (error: AwsServiceException) {
                            throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                        }
                    }
                    .progress()
            }
            // step 2 - return the resource model
            .then { readHandler.handleRequest(proxy, request, callbackContext, logger) }
    }
}

/**
 * Converts resource model tags to AWS service call model.
 *
 * @param tags Tags map.
 * @return AWS request model.
 */
fun convertTagsToAwsModel(tags: Map<String, String>) = tags.map {
    Tag.builder()
        .key(it.key)
        .value(it.value)
        .build()
}
