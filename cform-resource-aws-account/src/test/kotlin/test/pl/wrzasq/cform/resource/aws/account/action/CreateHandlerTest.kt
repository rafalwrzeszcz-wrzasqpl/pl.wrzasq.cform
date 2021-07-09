/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.account.action

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.account.action.CreateHandler
import pl.wrzasq.cform.resource.aws.account.action.UpdateHandler
import pl.wrzasq.cform.resource.aws.account.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.account.model.ResourceModel
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.awssdk.services.organizations.model.CreateAccountRequest
import software.amazon.awssdk.services.organizations.model.CreateAccountResponse
import software.amazon.awssdk.services.organizations.model.CreateAccountState
import software.amazon.awssdk.services.organizations.model.CreateAccountStatus
import software.amazon.awssdk.services.organizations.model.DescribeCreateAccountStatusRequest
import software.amazon.awssdk.services.organizations.model.DescribeCreateAccountStatusResponse
import software.amazon.awssdk.services.organizations.model.DescribeHandshakeRequest
import software.amazon.awssdk.services.organizations.model.DescribeHandshakeResponse
import software.amazon.awssdk.services.organizations.model.Handshake
import software.amazon.awssdk.services.organizations.model.HandshakeState
import software.amazon.awssdk.services.organizations.model.InviteAccountToOrganizationRequest
import software.amazon.awssdk.services.organizations.model.InviteAccountToOrganizationResponse
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

@ExtendWith(MockKExtension::class)
class CreateHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var factory: ResourcesFactory

    @MockK
    lateinit var updateHandler: UpdateHandler

    @MockK
    lateinit var proxyClient: ProxyClient<OrganizationsClient>

    @Test
    fun handleRequest() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateAccountRequest::class),
                any<Function<CreateAccountRequest, CreateAccountResponse>>()
            )
        } returns CreateAccountResponse.builder()
            .createAccountStatus(
                CreateAccountStatus.builder()
                    .state(CreateAccountState.IN_PROGRESS)
                    .id(ID)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeCreateAccountStatusRequest::class),
                any<Function<DescribeCreateAccountStatusRequest, DescribeCreateAccountStatusResponse>>()
            )
        } returns DescribeCreateAccountStatusResponse.builder()
            .createAccountStatus(
                CreateAccountStatus.builder()
                    .state(CreateAccountState.IN_PROGRESS)
                    .accountId(ID)
                    .build()
            )
            .build()

        every {
            updateHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, updateHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.IN_PROGRESS, result.status)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(InviteAccountToOrganizationRequest::class),
                any<Function<InviteAccountToOrganizationRequest, InviteAccountToOrganizationResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestExisting() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(InviteAccountToOrganizationRequest::class),
                any<Function<InviteAccountToOrganizationRequest, InviteAccountToOrganizationResponse>>()
            )
        } returns InviteAccountToOrganizationResponse.builder()
            .handshake(
                Handshake.builder()
                    .state(HandshakeState.REQUESTED)
                    .id(OU_ID)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeHandshakeRequest::class),
                any<Function<DescribeHandshakeRequest, DescribeHandshakeResponse>>()
            )
        } returns DescribeHandshakeResponse.builder()
            .handshake(
                Handshake.builder()
                    .state(HandshakeState.REQUESTED)
                    .build()
            )
            .build()

        every {
            updateHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, updateHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.IN_PROGRESS, result.status)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateAccountRequest::class),
                any<Function<CreateAccountRequest, CreateAccountResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestDone() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateAccountRequest::class),
                any<Function<CreateAccountRequest, CreateAccountResponse>>()
            )
        } returns CreateAccountResponse.builder()
            .createAccountStatus(
                CreateAccountStatus.builder()
                    .state(CreateAccountState.IN_PROGRESS)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeCreateAccountStatusRequest::class),
                any<Function<DescribeCreateAccountStatusRequest, DescribeCreateAccountStatusResponse>>()
            )
        } returnsMany listOf(
            DescribeCreateAccountStatusResponse.builder()
                .createAccountStatus(
                    CreateAccountStatus.builder()
                        .state(CreateAccountState.IN_PROGRESS)
                        .build()
                )
                .build(),
            DescribeCreateAccountStatusResponse.builder()
                .createAccountStatus(
                    CreateAccountStatus.builder()
                        .state(CreateAccountState.SUCCEEDED)
                        .accountId(ID)
                        .build()
                )
                .build()
        )

        every {
            updateHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, updateHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(ID, request.desiredResourceState?.id)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(InviteAccountToOrganizationRequest::class),
                any<Function<InviteAccountToOrganizationRequest, InviteAccountToOrganizationResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestExistingDone() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(InviteAccountToOrganizationRequest::class),
                any<Function<InviteAccountToOrganizationRequest, InviteAccountToOrganizationResponse>>()
            )
        } returns InviteAccountToOrganizationResponse.builder()
            .handshake(
                Handshake.builder()
                    .state(HandshakeState.REQUESTED)
                    .id(OU_ID)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeHandshakeRequest::class),
                any<Function<DescribeHandshakeRequest, DescribeHandshakeResponse>>()
            )
        } returns DescribeHandshakeResponse.builder()
            .handshake(
                Handshake.builder()
                    .state(HandshakeState.ACCEPTED)
                    .build()
            )
            .build()

        every {
            updateHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, updateHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateAccountRequest::class),
                any<Function<CreateAccountRequest, CreateAccountResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestErrorOnCreate() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateAccountRequest::class),
                any<Function<CreateAccountRequest, CreateAccountResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            CreateHandler(factory, updateHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify { updateHandler wasNot called }
    }

    @Test
    fun handleRequestErrorOnInvite() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(InviteAccountToOrganizationRequest::class),
                any<Function<InviteAccountToOrganizationRequest, InviteAccountToOrganizationResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            CreateHandler(factory, updateHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify { updateHandler wasNot called }
    }

    @Test
    fun handleRequestErrorOnCreateStabilize() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateAccountRequest::class),
                any<Function<CreateAccountRequest, CreateAccountResponse>>()
            )
        } returns CreateAccountResponse.builder()
            .createAccountStatus(
                CreateAccountStatus.builder()
                    .state(CreateAccountState.IN_PROGRESS)
                    .id(ID)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeCreateAccountStatusRequest::class),
                any<Function<DescribeCreateAccountStatusRequest, DescribeCreateAccountStatusResponse>>()
            )
        } throws exception

        val result = CreateHandler(factory, updateHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { updateHandler wasNot called }
    }

    @Test
    fun handleRequestErrorOnInviteStabilize() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(InviteAccountToOrganizationRequest::class),
                any<Function<InviteAccountToOrganizationRequest, InviteAccountToOrganizationResponse>>()
            )
        } returns InviteAccountToOrganizationResponse.builder()
            .handshake(
                Handshake.builder()
                    .state(HandshakeState.REQUESTED)
                    .id(ID)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeHandshakeRequest::class),
                any<Function<DescribeHandshakeRequest, DescribeHandshakeResponse>>()
            )
        } throws exception

        val result = CreateHandler(factory, updateHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { updateHandler wasNot called }
    }

    @Test
    fun handleRequestFailureOnCreateStabilize() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateAccountRequest::class),
                any<Function<CreateAccountRequest, CreateAccountResponse>>()
            )
        } returns CreateAccountResponse.builder()
            .createAccountStatus(
                CreateAccountStatus.builder()
                    .state(CreateAccountState.IN_PROGRESS)
                    .id(ID)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeCreateAccountStatusRequest::class),
                any<Function<DescribeCreateAccountStatusRequest, DescribeCreateAccountStatusResponse>>()
            )
        } returns DescribeCreateAccountStatusResponse.builder()
            .createAccountStatus(
                CreateAccountStatus.builder()
                    .state(CreateAccountState.FAILED)
                    .build()
            )
            .build()

        assertThrows<CfnNotStabilizedException> {
            CreateHandler(factory, updateHandler).handleRequest(proxy, request, callbackContext, logger)
        }

        verify { updateHandler wasNot called }
    }

    @Test
    fun handleRequestFailureOnInviteStabilize() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(InviteAccountToOrganizationRequest::class),
                any<Function<InviteAccountToOrganizationRequest, InviteAccountToOrganizationResponse>>()
            )
        } returns InviteAccountToOrganizationResponse.builder()
            .handshake(
                Handshake.builder()
                    .state(HandshakeState.REQUESTED)
                    .id(ID)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeHandshakeRequest::class),
                any<Function<DescribeHandshakeRequest, DescribeHandshakeResponse>>()
            )
        } returns DescribeHandshakeResponse.builder()
            .handshake(
                Handshake.builder()
                    .state(HandshakeState.DECLINED)
                    .build()
            )
            .build()

        assertThrows<CfnNotStabilizedException> {
            val result = CreateHandler(factory, updateHandler).handleRequest(proxy, request, callbackContext, logger)
            println(result)
        }

        verify { updateHandler wasNot called }
    }

    @Test
    fun handleRequestNull() {
        val request = ResourceHandlerRequest<ResourceModel?>()

        val proxy = initializeProxy(logger, factory, proxyClient)

        assertThrows<IllegalArgumentException> {
            CreateHandler(factory, updateHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }
}
