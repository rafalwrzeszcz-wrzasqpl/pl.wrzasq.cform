/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.commons.action

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.commons.action.NotUpdatableHandler
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Logger
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

@ExtendWith(MockKExtension::class)
class NotUpdatableHandlerTest {
    @MockK
    lateinit var proxy: AmazonWebServicesClientProxy

    @MockK
    lateinit var request: ResourceHandlerRequest<Any?>

    @MockK
    lateinit var callbackContext: StdCallbackContext

    @MockK
    lateinit var logger: Logger

    @Test
    fun handleRequest() {
        every { request.logicalResourceIdentifier } returns ""

        assertThrows<CfnNotUpdatableException> {
            NotUpdatableHandler<Any>("test").handleRequest(proxy, request, callbackContext, logger)
        }
    }
}
