/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.dynamodbitem.action

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.dynamodbitem.action.ReadHandler
import pl.wrzasq.cform.resource.aws.dynamodbitem.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.dynamodbitem.model.ResourceModel
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Credentials
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

const val TABLE_NAME = "123456789123"

const val TENANT_ID_KEY = "tenantId"
const val TENANT_ID = "wrzasq.pl"
const val N_KEY = "int"
const val N = "23"
const val NULL_KEY = "nil"

private val credentials = Credentials("ID", "secret", "test")

val exception: AwsServiceException = AwsServiceException.builder()
    .awsErrorDetails(
        AwsErrorDetails.builder()
            .sdkHttpResponse(
                SdkHttpResponse.builder()
                    .build()
            )
            .build()
    )
    .build()

@ExtendWith(MockKExtension::class)
class ReadHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var factory: ResourcesFactory

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

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetItemRequest::class),
                any<Function<GetItemRequest, GetItemResponse>>()
            )
        } returns GetItemResponse.builder()
            .item(
                mapOf(
                    TENANT_ID_KEY to AttributeValue.builder().s(TENANT_ID).build(),
                    N_KEY to AttributeValue.builder().n(N).build(),
                    NULL_KEY to AttributeValue.builder().nul(true).build()
                )
            )
            .build()

        val result = ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        val item = result.resourceModel?.item ?: emptyMap()

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(TABLE_NAME, result.resourceModel?.tableName)
        assertEquals(3, item.size)
        assertTrue(item.containsKey(TENANT_ID_KEY))
        assertTrue(item.containsKey(N_KEY))
        assertTrue(item.containsKey(NULL_KEY))
        assertTrue(item[TENANT_ID_KEY]?.containsKey("S") ?: false)
        assertEquals(TENANT_ID, item[TENANT_ID_KEY]?.get("S"))
        assertEquals(N, item[N_KEY]?.get("N"))
        assertEquals(true, item[NULL_KEY]?.get("NUL") ?: false)
    }

    @Test
    fun handleRequestFailed() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetItemRequest::class),
                any<Function<GetItemRequest, GetItemResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }

    @Test
    fun handleRequestNull() {
        val request = ResourceHandlerRequest<ResourceModel?>()

        val proxy = initializeProxy(logger, factory, proxyClient)

        assertThrows<IllegalArgumentException> {
            ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }
}

fun initializeProxy(
    logger: LoggerProxy,
    factory: ResourcesFactory,
    proxyClient: ProxyClient<DynamoDbClient>
): AmazonWebServicesClientProxy {
    val proxy = AmazonWebServicesClientProxy(logger, credentials) { 1L }

    every { logger.log(any()) } just runs
    every { factory.getClient(proxy) } returns proxyClient
    every { proxyClient.client().serviceName() } returns "dynamodb"

    return proxy
}
