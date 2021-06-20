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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.devicefarmproject.action.DeleteHandler
import pl.wrzasq.cform.resource.aws.devicefarmproject.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.devicefarmproject.model.ResourceModel
import software.amazon.awssdk.services.devicefarm.DeviceFarmClient
import software.amazon.awssdk.services.devicefarm.model.DeleteTestGridProjectRequest
import software.amazon.awssdk.services.devicefarm.model.DeleteTestGridProjectResponse
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

@ExtendWith(MockKExtension::class)
class DeleteHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var factory: ResourcesFactory

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

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DeleteTestGridProjectRequest::class),
                any<Function<DeleteTestGridProjectRequest, DeleteTestGridProjectResponse>>()
            )
        } returns DeleteTestGridProjectResponse.builder().build()

        val result = DeleteHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertNull(result.resourceModel)
    }

    @Test
    fun handleRequestFailed() {
        val model = ResourceModel().apply {
            arn = ARN
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DeleteTestGridProjectRequest::class),
                any<Function<DeleteTestGridProjectRequest, DeleteTestGridProjectResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            DeleteHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }

    @Test
    fun handleRequestNull() {
        val request = ResourceHandlerRequest<ResourceModel?>()

        val proxy = initializeProxy(logger, factory, proxyClient)

        assertThrows<IllegalArgumentException> {
            DeleteHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }
}
