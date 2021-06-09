/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.commons

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.commons.action.ActionHandler
import pl.wrzasq.cform.commons.config.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.cloudformation.Action
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.HandlerRequest
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.RequestData
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext

@ExtendWith(MockKExtension::class)
class ResourceLambdaHandlerTest {
    @MockK
    lateinit var configuration: Configuration

    @MockK
    lateinit var proxy: AmazonWebServicesClientProxy

    @MockK
    lateinit var handler: ActionHandler<Any>

    @MockK
    lateinit var logger: LoggerProxy

    @Test
    fun provideResourceSchemaJsonObject() {
        val json = JSONObject()

        every { configuration.resourceSchema } returns json

        val wrapper = TestResourceLambdaHandler(configuration, emptyMap(), logger)

        assertSame(json, wrapper.provideResourceSchemaJSONObject())
    }

    @Test
    fun transform() {
        val wrapper = TestResourceLambdaHandler(configuration, emptyMap(), logger)

        val data = RequestData<Any?>()

        val request = HandlerRequest<Any?, StdCallbackContext>().apply {
            requestData = data
            region = Region.EU_CENTRAL_1.id()
        }

        val result = wrapper.transform(request)

        assertNull(result.systemTags)
    }

    @Test
    fun invokeHandler() {
        val request = ResourceHandlerRequest<Any?>()
        val callbackContext = StdCallbackContext()
        val response = ProgressEvent<Any?, StdCallbackContext>()

        every { handler.handleRequest(proxy, request, callbackContext, any()) } returns response
        every { logger.log(any()) } just runs

        val wrapper = TestResourceLambdaHandler(configuration, mapOf(Action.CREATE to handler), logger)
        val result = wrapper.invokeHandler(proxy, request, Action.CREATE, callbackContext)

        verify { handler.handleRequest(proxy, request, callbackContext, any()) }

        assertSame(response, result)
    }

    @Test
    fun invokeHandleNullCallback() {
        val request = ResourceHandlerRequest<Any?>()
        val response = ProgressEvent<Any?, StdCallbackContext>()

        every { handler.handleRequest(proxy, request, any(), any()) } returns response
        every { logger.log(any()) } just runs

        val wrapper = TestResourceLambdaHandler(configuration, mapOf(Action.CREATE to handler), logger)
        val result = wrapper.invokeHandler(proxy, request, Action.CREATE, null)

        verify { handler.handleRequest(proxy, request, any(), any()) }

        assertSame(response, result)
    }

    @Test
    fun invokeHandlerNullProxy() {
        val wrapper = TestResourceLambdaHandler(configuration, emptyMap(), logger)

        assertThrows<IllegalArgumentException> { wrapper.invokeHandler(null, ResourceHandlerRequest(), null, null) }
    }

    @Test
    fun invokeHandlerNoHandler() {
        val wrapper = TestResourceLambdaHandler(configuration, emptyMap(), logger)

        assertThrows<RuntimeException> { wrapper.invokeHandler(proxy, ResourceHandlerRequest(), Action.CREATE, null) }
    }
}
