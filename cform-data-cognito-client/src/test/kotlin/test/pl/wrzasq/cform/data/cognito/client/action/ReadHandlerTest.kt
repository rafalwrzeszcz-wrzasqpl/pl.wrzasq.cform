/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022, 2024 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.data.cognito.client.action

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.data.cognito.client.action.ReadHandler
import pl.wrzasq.cform.data.cognito.client.config.ResourcesFactory
import pl.wrzasq.cform.data.cognito.client.model.ResourceModel
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType
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

const val USER_POOL_ID = "eu-central-1_test"
const val CLIENT_ID = "abcd"
const val CLIENT_SECRET = "test1"

private val CREDENTIALS = Credentials("ID", "secret", "test")

val EXCEPTION: AwsServiceException = AwsServiceException.builder()
    .awsErrorDetails(
        AwsErrorDetails.builder()
            .sdkHttpResponse(
                SdkHttpResponse.builder()
                    .build(),
            )
            .build(),
    )
    .build()

@ExtendWith(MockKExtension::class)
class ReadHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var factory: ResourcesFactory

    @MockK
    lateinit var proxyClient: ProxyClient<CognitoIdentityProviderClient>

    @Test
    fun handleRequest() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeUserPoolClientRequest::class),
                any<Function<DescribeUserPoolClientRequest, DescribeUserPoolClientResponse>>(),
            )
        } returns DescribeUserPoolClientResponse.builder()
            .userPoolClient(
                UserPoolClientType.builder()
                    .userPoolId(USER_POOL_ID)
                    .clientId(CLIENT_ID)
                    .clientSecret(CLIENT_SECRET)
                    .build(),
            )
            .build()

        val result = ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(USER_POOL_ID, result.resourceModel?.userPoolId)
        assertEquals(CLIENT_ID, result.resourceModel?.clientId)
        assertEquals(CLIENT_SECRET, result.resourceModel?.clientSecret)
    }

    @Test
    fun handleRequestNotFound() {
        val model = ResourceModel().apply {
            userPoolId = USER_POOL_ID
            clientId = CLIENT_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeUserPoolClientRequest::class),
                any<Function<DescribeUserPoolClientRequest, DescribeUserPoolClientResponse>>(),
            )
        } throws ResourceNotFoundException.builder().build()

        assertThrows<CfnNotFoundException> {
            ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }

    @Test
    fun handleRequestFailed() {
        val model = ResourceModel().apply {
            userPoolId = USER_POOL_ID
            clientId = CLIENT_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeUserPoolClientRequest::class),
                any<Function<DescribeUserPoolClientRequest, DescribeUserPoolClientResponse>>(),
            )
        } throws EXCEPTION

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
    proxyClient: ProxyClient<CognitoIdentityProviderClient>,
): AmazonWebServicesClientProxy {
    val proxy = AmazonWebServicesClientProxy(logger, CREDENTIALS) { 1L }

    every { logger.log(any()) } just runs
    every { factory.getClient(proxy) } returns proxyClient
    every { proxyClient.client().serviceName() } returns "cognito-idp"

    return proxy
}
