/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.devicefarmproject.action

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.devicefarmproject.action.CreateHandler
import pl.wrzasq.cform.resource.aws.devicefarmproject.action.ReadHandler
import pl.wrzasq.cform.resource.aws.devicefarmproject.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.devicefarmproject.model.ResourceModel
import software.amazon.awssdk.services.devicefarm.DeviceFarmClient
import software.amazon.awssdk.services.devicefarm.model.CreateTestGridProjectRequest
import software.amazon.awssdk.services.devicefarm.model.CreateTestGridProjectResponse
import software.amazon.awssdk.services.devicefarm.model.TestGridProject
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
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
    lateinit var readHandler: ReadHandler

    @MockK
    lateinit var proxyClient: ProxyClient<DeviceFarmClient>

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
                ofType(CreateTestGridProjectRequest::class),
                any<Function<CreateTestGridProjectRequest, CreateTestGridProjectResponse>>()
            )
        } returns CreateTestGridProjectResponse.builder()
            .testGridProject(
                TestGridProject.builder()
                    .arn(ARN)
                    .name(NAME)
                    .description(DESCRIPTION)
                    .build()
            )
            .build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(ARN, request.desiredResourceState?.arn)
    }

    @Test
    fun handleRequestErrorOnCreate() {
        val model = ResourceModel().apply {
            arn = ARN
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateTestGridProjectRequest::class),
                any<Function<CreateTestGridProjectRequest, CreateTestGridProjectResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            CreateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }

    @Test
    fun handleRequestNull() {
        val request = ResourceHandlerRequest<ResourceModel?>()

        val proxy = initializeProxy(logger, factory, proxyClient)

        assertThrows<IllegalArgumentException> {
            CreateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }
}
