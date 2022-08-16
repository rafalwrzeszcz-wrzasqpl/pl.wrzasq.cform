/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.dynamodbitem.action

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.dynamodbitem.action.CreateHandler
import pl.wrzasq.cform.resource.aws.dynamodbitem.action.ReadHandler
import pl.wrzasq.cform.resource.aws.dynamodbitem.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.dynamodbitem.model.ResourceModel
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
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
    lateinit var proxyClient: ProxyClient<DynamoDbClient>

    @Test
    fun handleRequest() {
        val model = ResourceModel().apply {
            tableName = TABLE_NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(PutItemRequest::class),
                any<Function<PutItemRequest, PutItemResponse>>()
            )
        } returns PutItemResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetItemRequest::class),
                any<Function<GetItemRequest, GetItemResponse>>()
            )
        } returns GetItemResponse.builder()
            .item(
                mapOf(
                    TENANT_ID_KEY to AttributeValue.builder().s(TENANT_ID).build()
                )
            )
            .build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
    }

    @Test
    fun handleRequestFailed() {
        val model = ResourceModel().apply {
            tableName = TABLE_NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(PutItemRequest::class),
                any<Function<PutItemRequest, PutItemResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            CreateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)
        }

        verify { readHandler wasNot called }
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
