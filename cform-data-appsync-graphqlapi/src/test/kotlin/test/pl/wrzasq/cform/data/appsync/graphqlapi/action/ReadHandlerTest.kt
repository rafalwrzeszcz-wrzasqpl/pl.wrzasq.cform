/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.data.appsync.graphqlapi.action

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
import pl.wrzasq.cform.data.appsync.graphqlapi.action.ReadHandler
import pl.wrzasq.cform.data.appsync.graphqlapi.config.ResourcesFactory
import pl.wrzasq.cform.data.appsync.graphqlapi.model.ResourceModel
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.appsync.AppSyncClient
import software.amazon.awssdk.services.appsync.model.GetGraphqlApiRequest
import software.amazon.awssdk.services.appsync.model.GetGraphqlApiResponse
import software.amazon.awssdk.services.appsync.model.GraphqlApi
import software.amazon.awssdk.services.appsync.model.NotFoundException
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnNotFoundException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Credentials
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

const val API_ID = "o-1"
const val DOMAIN_NAME = "wrzasq.pl"

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
    lateinit var proxyClient: ProxyClient<AppSyncClient>

    @Test
    fun handleRequest() {
        val model = ResourceModel().apply {
            apiId = API_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetGraphqlApiRequest::class),
                any<Function<GetGraphqlApiRequest, GetGraphqlApiResponse>>()
            )
        } returns GetGraphqlApiResponse.builder()
            .graphqlApi(
                GraphqlApi.builder()
                    .apiId(API_ID)
                    .uris(
                        mapOf(
                            "GRAPHQL" to "https://${DOMAIN_NAME}/graphql"
                        )
                    )
                    .build()
            )
            .build()

        val result = ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(API_ID, result.resourceModel?.apiId)
        assertEquals(DOMAIN_NAME, result.resourceModel?.domainName)
    }

    @Test
    fun handleRequestNoDomain() {
        val model = ResourceModel().apply {
            apiId = API_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetGraphqlApiRequest::class),
                any<Function<GetGraphqlApiRequest, GetGraphqlApiResponse>>()
            )
        } returns GetGraphqlApiResponse.builder()
            .graphqlApi(
                GraphqlApi.builder()
                    .apiId(API_ID)
                    .uris(emptyMap())
                    .build()
            )
            .build()

        val result = ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(API_ID, result.resourceModel?.apiId)
        assertTrue(result.resourceModel?.domainName?.isEmpty() == true)
    }

    @Test
    fun handleRequestNotFound() {
        val model = ResourceModel().apply {
            apiId = API_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetGraphqlApiRequest::class),
                any<Function<GetGraphqlApiRequest, GetGraphqlApiResponse>>()
            )
        } throws NotFoundException.builder().build()

        assertThrows<CfnNotFoundException> {
            ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }

    @Test
    fun handleRequestFailed() {
        val model = ResourceModel().apply {
            apiId = API_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetGraphqlApiRequest::class),
                any<Function<GetGraphqlApiRequest, GetGraphqlApiResponse>>()
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
    proxyClient: ProxyClient<AppSyncClient>
): AmazonWebServicesClientProxy {
    val proxy = AmazonWebServicesClientProxy(logger, credentials) { 1L }

    every { logger.log(any()) } just runs
    every { factory.getClient(proxy) } returns proxyClient
    every { proxyClient.client().serviceName() } returns "appsync"

    return proxy
}
