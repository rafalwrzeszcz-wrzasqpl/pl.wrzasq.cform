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
import pl.wrzasq.cform.resource.aws.account.model.toCreateRequest
import pl.wrzasq.cform.resource.aws.account.model.toInviteRequest
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.organizations.model.CreateAccountState
import software.amazon.awssdk.services.organizations.model.DescribeCreateAccountStatusRequest
import software.amazon.awssdk.services.organizations.model.DescribeHandshakeRequest
import software.amazon.awssdk.services.organizations.model.HandshakeState
import software.amazon.awssdk.services.organizations.model.Tag
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import software.amazon.cloudformation.proxy.delay.Constant
import java.time.Duration

private val delay = Constant.of()
    .delay(Duration.ofSeconds(60))
    .timeout(Duration.ofHours(4))
    .build()

/**
 * Resource creation handler.
 *
 * @param factory Dependent resource factory.
 * @param updateHandler Resource update handler.
 */
class CreateHandler(
    private val factory: ResourcesFactory,
    private val updateHandler: ActionHandler<ResourceModel>
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
                if (it.resourceModel.id == null) {
                    proxy.initiate(
                        "WrzasqPl-AWS-Account::Create",
                        proxyClient,
                        it.resourceModel,
                        it.callbackContext
                    )
                        .translateToServiceRequest(ResourceModel::toCreateRequest)
                        .backoffDelay(delay)
                        .makeServiceCall { awsRequest, client ->
                            try {
                                client.injectCredentialsAndInvokeV2(
                                    awsRequest,
                                    client.client()::createAccount
                                )
                            } catch (error: AwsServiceException) {
                                throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                            }
                        }
                        .stabilize { _, response, client, model, _ ->
                            var status = client.injectCredentialsAndInvokeV2(
                                DescribeCreateAccountStatusRequest.builder()
                                    .createAccountRequestId(response.createAccountStatus().id())
                                    .build(),
                                client.client()::describeCreateAccountStatus
                            ).createAccountStatus()

                            // we need to loop for it now, before we return first status as we need primary identifier
                            while (status.accountId() == null && status.state() == CreateAccountState.IN_PROGRESS) {
                                status = client.injectCredentialsAndInvokeV2(
                                    DescribeCreateAccountStatusRequest.builder()
                                        .createAccountRequestId(response.createAccountStatus().id())
                                        .build(),
                                    client.client()::describeCreateAccountStatus
                                ).createAccountStatus()
                            }
                            model.id = status.accountId()

                            when (status.state()) {
                                CreateAccountState.SUCCEEDED -> {
                                    // pass account ID to read handler
                                    request.desiredResourceState?.id = status.accountId()
                                    true
                                }
                                CreateAccountState.IN_PROGRESS -> false
                                else -> {
                                    logger.log("Failed to create account for ${model.email} (${model.name}) - reason:" +
                                        " ${status.failureReasonAsString()}")
                                    throw CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.email)
                                }
                            }
                        }
                } else {
                    proxy.initiate(
                        "WrzasqPl-AWS-Account::Invite",
                        proxyClient,
                        it.resourceModel,
                        it.callbackContext
                    )
                        .translateToServiceRequest(ResourceModel::toInviteRequest)
                        .backoffDelay(delay)
                        .makeServiceCall { awsRequest, client ->
                            try {
                                client.injectCredentialsAndInvokeV2(
                                    awsRequest,
                                    client.client()::inviteAccountToOrganization
                                )
                            } catch (error: AwsServiceException) {
                                throw CfnGeneralServiceException(ResourceModel.TYPE_NAME, error)
                            }
                        }
                        .stabilize { _, response, client, model, _ ->
                            val handshake = client.injectCredentialsAndInvokeV2(
                                DescribeHandshakeRequest.builder()
                                    .handshakeId(response.handshake().id())
                                    .build(),
                                client.client()::describeHandshake
                            ).handshake()

                            when (handshake.state()) {
                                HandshakeState.ACCEPTED -> true
                                HandshakeState.OPEN,
                                HandshakeState.REQUESTED -> false
                                else -> {
                                    logger.log("Failed to invite account ${model.id} - status: ${handshake.state()}.")
                                    throw CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.id)
                                }
                            }
                        }
                }
                    .progress()
            }
            // step 2 - return the resource model
            .then { updateHandler.handleRequest(proxy, request, callbackContext, logger) }
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
