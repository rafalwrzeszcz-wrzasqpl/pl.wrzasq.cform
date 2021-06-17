/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.data.cognito.domain.action

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.data.cognito.domain.action.CreateHandler
import pl.wrzasq.cform.data.cognito.domain.action.ReadHandler
import pl.wrzasq.cform.data.cognito.domain.model.ResourceModel
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

@ExtendWith(MockKExtension::class)
class CreateHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var proxy: AmazonWebServicesClientProxy

    @MockK
    lateinit var readHandler: ReadHandler

    @Test
    fun handleRequest() {
        val request = ResourceHandlerRequest<ResourceModel?>()
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success<ResourceModel?, StdCallbackContext>(null, callbackContext)

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(readHandler).handleRequest(proxy, request, callbackContext, logger)

        assertSame(event, result)
    }
}
