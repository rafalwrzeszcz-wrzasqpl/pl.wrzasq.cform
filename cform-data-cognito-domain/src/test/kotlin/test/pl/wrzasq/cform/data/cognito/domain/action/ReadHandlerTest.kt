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
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.data.cognito.domain.action.ReadHandler
import pl.wrzasq.cform.data.cognito.domain.config.ResourcesFactory
import pl.wrzasq.cform.data.cognito.domain.model.ResourceModel
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolDomainRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolDomainResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.DomainDescriptionType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResourceNotFoundException
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

const val DOMAIN = "wrzasq.pl"
const val CLOUDFRONT_DISTRIBUTION = "https://abcdf.cloudfront.amazonaws.com/"

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
                ofType(DescribeUserPoolDomainRequest::class),
                any<Function<DescribeUserPoolDomainRequest, DescribeUserPoolDomainResponse>>()
            )
        } returns DescribeUserPoolDomainResponse.builder()
            .domainDescription(
                DomainDescriptionType.builder()
                    .domain(DOMAIN)
                    .cloudFrontDistribution(CLOUDFRONT_DISTRIBUTION)
                    .build()
            )
            .build()

        val result = ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(DOMAIN, result.resourceModel?.domain)
        assertEquals(CLOUDFRONT_DISTRIBUTION, result.resourceModel?.cloudFrontDistribution)
    }

    @Test
    fun handleRequestNotFound() {
        val model = ResourceModel().apply {
            domain = DOMAIN
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeUserPoolDomainRequest::class),
                any<Function<DescribeUserPoolDomainRequest, DescribeUserPoolDomainResponse>>()
            )
        } throws ResourceNotFoundException.builder().build()

        assertThrows<CfnNotFoundException> {
            ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }

    @Test
    fun handleRequestFailed() {
        val model = ResourceModel().apply {
            domain = DOMAIN
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeUserPoolDomainRequest::class),
                any<Function<DescribeUserPoolDomainRequest, DescribeUserPoolDomainResponse>>()
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
    proxyClient: ProxyClient<CognitoIdentityProviderClient>
): AmazonWebServicesClientProxy {
    val proxy = AmazonWebServicesClientProxy(logger, credentials) { 1L }

    every { logger.log(any()) } just runs
    every { factory.getClient(proxy) } returns proxyClient
    every { proxyClient.client().serviceName() } returns "cognito-idp"

    return proxy
}
