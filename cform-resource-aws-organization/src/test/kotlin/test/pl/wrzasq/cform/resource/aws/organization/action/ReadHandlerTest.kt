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
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.organization.action.ReadHandler
import pl.wrzasq.cform.resource.aws.organization.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.organization.model.ResourceModel
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationRequest
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationResponse
import software.amazon.awssdk.services.organizations.model.ListRootsRequest
import software.amazon.awssdk.services.organizations.model.ListRootsResponse
import software.amazon.awssdk.services.organizations.model.Organization
import software.amazon.awssdk.services.organizations.model.Root
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Credentials
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

const val ID = "o-1"
const val ARN = "arn:aws:test"
const val ROOT_ID = "ou-r"

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
    lateinit var proxyClient: ProxyClient<OrganizationsClient>

    @Test
    fun handleRequest() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeOrganizationRequest::class),
                any<Function<DescribeOrganizationRequest, DescribeOrganizationResponse>>()
            )
        } returns DescribeOrganizationResponse.builder()
            .organization(
                Organization.builder()
                    .id(ID)
                    .arn(ARN)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(ListRootsRequest::class),
                any<Function<ListRootsRequest, ListRootsResponse>>()
            )
        } returns ListRootsResponse.builder()
            .roots(
                Root.builder()
                    .id(ROOT_ID)
                    .build()
            )
            .build()

        val result = ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(ID, result.resourceModel?.id)
        assertEquals(ARN, result.resourceModel?.arn)
        assertEquals(ROOT_ID, result.resourceModel?.rootId)
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
                ofType(DescribeOrganizationRequest::class),
                any<Function<DescribeOrganizationRequest, DescribeOrganizationResponse>>()
            )
        } throws exception

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(ListRootsRequest::class),
                any<Function<ListRootsRequest, ListRootsResponse>>()
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
    proxyClient: ProxyClient<OrganizationsClient>
): AmazonWebServicesClientProxy {
    val proxy = AmazonWebServicesClientProxy(logger, credentials) { 1L }

    every { logger.log(any()) } just runs
    every { factory.getClient(proxy) } returns proxyClient
    every { proxyClient.client().serviceName() } returns "organizations"

    return proxy
}
