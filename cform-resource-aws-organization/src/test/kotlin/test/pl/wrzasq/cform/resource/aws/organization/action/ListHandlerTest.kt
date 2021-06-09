/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.organization.action

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.organization.action.ListHandler
import pl.wrzasq.cform.resource.aws.organization.model.ResourceModel
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

@ExtendWith(MockKExtension::class)
class ListHandlerTest {
    @MockK
    lateinit var proxy: AmazonWebServicesClientProxy

    @MockK
    lateinit var request: ResourceHandlerRequest<ResourceModel?>

    @MockK
    lateinit var callbackContext: StdCallbackContext

    @MockK
    lateinit var logger: Logger

    @Test
    fun handleRequest() {
        every { logger.log(any()) } just runs

        val result = ListHandler().handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
    }
}
