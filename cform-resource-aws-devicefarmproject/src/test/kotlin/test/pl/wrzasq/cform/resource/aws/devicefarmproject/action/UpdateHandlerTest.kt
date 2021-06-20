/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.devicefarmproject.action

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.devicefarmproject.action.ReadHandler
import pl.wrzasq.cform.resource.aws.devicefarmproject.action.UpdateHandler
import pl.wrzasq.cform.resource.aws.devicefarmproject.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.devicefarmproject.model.ResourceModel
import software.amazon.awssdk.services.devicefarm.DeviceFarmClient
import software.amazon.awssdk.services.devicefarm.model.GetTestGridProjectRequest
import software.amazon.awssdk.services.devicefarm.model.GetTestGridProjectResponse
import software.amazon.awssdk.services.devicefarm.model.NotFoundException
import software.amazon.awssdk.services.devicefarm.model.UpdateTestGridProjectRequest
import software.amazon.awssdk.services.devicefarm.model.UpdateTestGridProjectResponse
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
    lateinit var proxyClient: ProxyClient<DeviceFarmClient>

    @Test
    fun handleRequest() {
        val model = ResourceModel().apply {
            arn = ARN
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetTestGridProjectRequest::class),
                any<Function<GetTestGridProjectRequest, GetTestGridProjectResponse>>()
            )
        } returns GetTestGridProjectResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateTestGridProjectRequest::class),
                any<Function<UpdateTestGridProjectRequest, UpdateTestGridProjectResponse>>()
            )
        } returns UpdateTestGridProjectResponse.builder().build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(ARN, result.resourceModel?.arn)
    }

    @Test
    fun handleRequestErrorOnDescribe() {
        val model = ResourceModel().apply {
            arn = ARN
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetTestGridProjectRequest::class),
                any<Function<GetTestGridProjectRequest, GetTestGridProjectResponse>>()
            )
        } throws exception

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { readHandler wasNot called }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateTestGridProjectRequest::class),
                any<Function<UpdateTestGridProjectRequest, UpdateTestGridProjectResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestNotFound() {
        val model = ResourceModel().apply {
            arn = ARN
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetTestGridProjectRequest::class),
                any<Function<GetTestGridProjectRequest, GetTestGridProjectResponse>>()
            )
        } throws NotFoundException.builder().build()

        val result = UpdateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { readHandler wasNot called }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateTestGridProjectRequest::class),
                any<Function<UpdateTestGridProjectRequest, UpdateTestGridProjectResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestErrorOnUpdate() {
        val model = ResourceModel().apply {
            arn = ARN
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetTestGridProjectRequest::class),
                any<Function<GetTestGridProjectRequest, GetTestGridProjectResponse>>()
            )
        } returns GetTestGridProjectResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateTestGridProjectRequest::class),
                any<Function<UpdateTestGridProjectRequest, UpdateTestGridProjectResponse>>()
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
