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
import pl.wrzasq.cform.resource.aws.account.action.ReadHandler
import pl.wrzasq.cform.resource.aws.account.action.UpdateHandler
import pl.wrzasq.cform.resource.aws.account.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.account.model.ResourceModel
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.awssdk.services.organizations.model.ChildNotFoundException
import software.amazon.awssdk.services.organizations.model.ListParentsRequest
import software.amazon.awssdk.services.organizations.model.ListParentsResponse
import software.amazon.awssdk.services.organizations.model.MoveAccountRequest
import software.amazon.awssdk.services.organizations.model.MoveAccountResponse
import software.amazon.awssdk.services.organizations.model.Parent
import software.amazon.awssdk.services.organizations.model.TagResourceRequest
import software.amazon.awssdk.services.organizations.model.TagResourceResponse
import software.amazon.awssdk.services.organizations.model.UntagResourceRequest
import software.amazon.awssdk.services.organizations.model.UntagResourceResponse
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

@ExtendWith(MockKExtension::class)
class UpdateHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var factory: ResourcesFactory

    @MockK
    lateinit var readHandler: ReadHandler

    @MockK
    lateinit var proxyClient: ProxyClient<OrganizationsClient>

    @Test
    fun handleRequest() {
        val model = ResourceModel().apply {
            id = ID
            ouId = OU_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsResponse>>()
            )
        } returns ListParentsResponse.builder()
            .parents(
                Parent.builder()
                    .id(OU_ID)
                    .build()
            )
            .build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(ID, result.resourceModel?.id)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            ) wasNot called
        }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            ) wasNot called
        }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UntagResourceRequest::class),
                any<Function<UntagResourceRequest, UntagResourceResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestSetOuId() {
        val model = ResourceModel().apply {
            id = ID
            ouId = OU_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsResponse>>()
            )
        } returns ListParentsResponse.builder()
            .parents(
                Parent.builder().build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            )
        } returns MoveAccountResponse.builder().build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            )
        }
    }

    @Test
    fun handleRequestChanges() {
        val model = ResourceModel().apply {
            id = ID
            ouId = OU_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
            previousResourceTags = mapOf(
                TAG_NAME to TAG_VALUE,
                "left" to "right"
            )
            desiredResourceTags = mapOf(
                TAG_NAME to TAG_VALUE,
                "another" to "some"
            )
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsResponse>>()
            )
        } returns ListParentsResponse.builder()
            .parents(
                Parent.builder()
                    .id("${OU_ID}x")
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            )
        } returns MoveAccountResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            )
        } returns TagResourceResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UntagResourceRequest::class),
                any<Function<UntagResourceRequest, UntagResourceResponse>>()
            )
        } returns UntagResourceResponse.builder().build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(ID, result.resourceModel?.id)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            )
        }

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            )
        }

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UntagResourceRequest::class),
                any<Function<UntagResourceRequest, UntagResourceResponse>>()
            )
        }
    }

    @Test
    fun handleRequestErrorOnDescribe() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsResponse>>()
            )
        } throws exception

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { readHandler wasNot called }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestNotFound() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsResponse>>()
            )
        } throws ChildNotFoundException.builder().build()

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { readHandler wasNot called }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestNoOuId() {
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
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsResponse>>()
            )
        } returns ListParentsResponse.builder()
            .parents(
                Parent.builder().build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            )
        } returns MoveAccountResponse.builder().build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(ID, result.resourceModel?.id)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            ) wasNot called
        }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UntagResourceRequest::class),
                any<Function<UntagResourceRequest, UntagResourceResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestErrorOnUpdate() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsResponse>>()
            )
        } returns ListParentsResponse.builder()
            .parents(
                Parent.builder()
                    .id(OU_ID)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify { readHandler wasNot called }
    }

    @Test
    fun handleRequestErrorOnUpdateTags() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
            previousResourceTags = mapOf(
                TAG_NAME to TAG_VALUE
            )
            desiredResourceTags = mapOf(
                TAG_NAME to TAG_VALUE,
                "another" to "some"
            )
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsResponse>>()
            )
        } returns ListParentsResponse.builder()
            .parents(
                Parent.builder()
                    .id(OU_ID)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(MoveAccountRequest::class),
                any<Function<MoveAccountRequest, MoveAccountResponse>>()
            )
        } returns MoveAccountResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify { readHandler wasNot called }
    }

    @Test
    fun handleRequestNull() {
        val request = ResourceHandlerRequest<ResourceModel?>()

        val proxy = initializeProxy(logger, factory, proxyClient)

        assertThrows<IllegalArgumentException> {
            UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }
}
