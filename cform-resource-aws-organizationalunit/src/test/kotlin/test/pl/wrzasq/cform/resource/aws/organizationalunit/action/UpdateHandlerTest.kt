/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.organizationalunit.action

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.organizationalunit.action.ReadHandler
import pl.wrzasq.cform.resource.aws.organizationalunit.action.UpdateHandler
import pl.wrzasq.cform.resource.aws.organizationalunit.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.organizationalunit.model.ResourceModel
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationalUnitRequest
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationalUnitResponse
import software.amazon.awssdk.services.organizations.model.DuplicateOrganizationalUnitException
import software.amazon.awssdk.services.organizations.model.OrganizationalUnitNotFoundException
import software.amazon.awssdk.services.organizations.model.TagResourceRequest
import software.amazon.awssdk.services.organizations.model.TagResourceResponse
import software.amazon.awssdk.services.organizations.model.UntagResourceRequest
import software.amazon.awssdk.services.organizations.model.UntagResourceResponse
import software.amazon.awssdk.services.organizations.model.UpdateOrganizationalUnitRequest
import software.amazon.awssdk.services.organizations.model.UpdateOrganizationalUnitResponse
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnResourceConflictException
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
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeOrganizationalUnitRequest::class),
                any<Function<DescribeOrganizationalUnitRequest, DescribeOrganizationalUnitResponse>>()
            )
        } returns DescribeOrganizationalUnitResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateOrganizationalUnitRequest::class),
                any<Function<UpdateOrganizationalUnitRequest, UpdateOrganizationalUnitResponse>>()
            )
        } returns UpdateOrganizationalUnitResponse.builder().build()

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
    fun handleRequestChangeTags() {
        val model = ResourceModel().apply {
            id = ID
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
                ofType(DescribeOrganizationalUnitRequest::class),
                any<Function<DescribeOrganizationalUnitRequest, DescribeOrganizationalUnitResponse>>()
            )
        } returns DescribeOrganizationalUnitResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateOrganizationalUnitRequest::class),
                any<Function<UpdateOrganizationalUnitRequest, UpdateOrganizationalUnitResponse>>()
            )
        } returns UpdateOrganizationalUnitResponse.builder().build()

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
                ofType(DescribeOrganizationalUnitRequest::class),
                any<Function<DescribeOrganizationalUnitRequest, DescribeOrganizationalUnitResponse>>()
            )
        } throws exception

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { readHandler wasNot called }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateOrganizationalUnitRequest::class),
                any<Function<UpdateOrganizationalUnitRequest, UpdateOrganizationalUnitResponse>>()
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
                ofType(DescribeOrganizationalUnitRequest::class),
                any<Function<DescribeOrganizationalUnitRequest, DescribeOrganizationalUnitResponse>>()
            )
        } throws OrganizationalUnitNotFoundException.builder().build()

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { readHandler wasNot called }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateOrganizationalUnitRequest::class),
                any<Function<UpdateOrganizationalUnitRequest, UpdateOrganizationalUnitResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestAlreadyExists() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeOrganizationalUnitRequest::class),
                any<Function<DescribeOrganizationalUnitRequest, DescribeOrganizationalUnitResponse>>()
            )
        } returns DescribeOrganizationalUnitResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateOrganizationalUnitRequest::class),
                any<Function<UpdateOrganizationalUnitRequest, UpdateOrganizationalUnitResponse>>()
            )
        } throws DuplicateOrganizationalUnitException.builder().build()

        assertThrows<CfnResourceConflictException> {
            UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify { readHandler wasNot called }
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
                ofType(DescribeOrganizationalUnitRequest::class),
                any<Function<DescribeOrganizationalUnitRequest, DescribeOrganizationalUnitResponse>>()
            )
        } returns DescribeOrganizationalUnitResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateOrganizationalUnitRequest::class),
                any<Function<UpdateOrganizationalUnitRequest, UpdateOrganizationalUnitResponse>>()
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
                ofType(DescribeOrganizationalUnitRequest::class),
                any<Function<DescribeOrganizationalUnitRequest, DescribeOrganizationalUnitResponse>>()
            )
        } returns DescribeOrganizationalUnitResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateOrganizationalUnitRequest::class),
                any<Function<UpdateOrganizationalUnitRequest, UpdateOrganizationalUnitResponse>>()
            )
        } returns UpdateOrganizationalUnitResponse.builder().build()

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
