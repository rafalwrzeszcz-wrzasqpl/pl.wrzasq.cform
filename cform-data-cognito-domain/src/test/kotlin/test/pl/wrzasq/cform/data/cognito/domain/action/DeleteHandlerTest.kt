/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.data.cognito.domain.action

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.data.cognito.domain.action.DeleteHandler
import pl.wrzasq.cform.data.cognito.domain.model.ResourceModel
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

@ExtendWith(MockKExtension::class)
class DeleteHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var proxy: AmazonWebServicesClientProxy

    @Test
    fun handleRequest() {
        val result = DeleteHandler().handleRequest(
            proxy,
            ResourceHandlerRequest<ResourceModel?>(),
            StdCallbackContext(),
            logger
        )

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertNull(result.resourceModel)
    }
}
