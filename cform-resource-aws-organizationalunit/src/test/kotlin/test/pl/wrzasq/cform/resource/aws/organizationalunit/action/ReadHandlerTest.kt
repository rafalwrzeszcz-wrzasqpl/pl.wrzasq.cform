/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.organizationalunit.action

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.organizationalunit.action.ReadHandler
import pl.wrzasq.cform.resource.aws.organizationalunit.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.organizationalunit.model.ResourceModel
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationalUnitRequest
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationalUnitResponse
import software.amazon.awssdk.services.organizations.model.ListParentsRequest
import software.amazon.awssdk.services.organizations.model.ListParentsResponse
import software.amazon.awssdk.services.organizations.model.ListTagsForResourceRequest
import software.amazon.awssdk.services.organizations.model.ListTagsForResourceResponse
import software.amazon.awssdk.services.organizations.model.OrganizationalUnit
import software.amazon.awssdk.services.organizations.model.Parent
import software.amazon.awssdk.services.organizations.paginators.ListParentsIterable
import software.amazon.awssdk.services.organizations.paginators.ListTagsForResourceIterable
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy
import software.amazon.cloudformation.proxy.Credentials
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function
import software.amazon.awssdk.services.organizations.model.Tag as AwsTag

const val ID = "ou-1-3"
const val ARN = "arn:aws:test"
const val NAME = "Test"
const val PARENT_ID = "ou-1-4"

const val TAG_NAME = "Foo"
const val TAG_VALUE = "Bar"

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

    @MockK
    lateinit var listParents: ListParentsIterable

    @MockK
    lateinit var listTagsForResource: ListTagsForResourceIterable

    @Test
    fun handleRequest() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeOrganizationalUnitRequest::class),
                any<Function<DescribeOrganizationalUnitRequest, DescribeOrganizationalUnitResponse>>()
            )
        } returns DescribeOrganizationalUnitResponse.builder()
            .organizationalUnit(
                OrganizationalUnit.builder()
                    .id(ID)
                    .arn(ARN)
                    .name(NAME)
                    .build()
            )
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeIterableV2(
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsIterable>>()
            )
        } returns listParents

        every {
            listParents.iterator()
        } returns mutableListOf(
            ListParentsResponse.builder()
                .parents(
                    Parent.builder()
                        .id(PARENT_ID)
                        .build()
                )
                .build()
        ).iterator()

        every {
            proxyClient.injectCredentialsAndInvokeIterableV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceIterable>>()
            )
        } returns listTagsForResource

        every {
            listTagsForResource.iterator()
        } returns mutableListOf(
            ListTagsForResourceResponse.builder()
                .tags(
                    AwsTag.builder()
                        .key(TAG_NAME)
                        .value(TAG_VALUE)
                        .build()
                )
                .build()
        ).iterator()

        val result = ReadHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(ID, result.resourceModel?.id)
        assertEquals(ARN, result.resourceModel?.arn)
        assertEquals(NAME, result.resourceModel?.name)
        assertEquals(PARENT_ID, result.resourceModel?.parentId)
        assertNotNull(result.resourceModel?.tags?.find { it.key == TAG_NAME && it.value == TAG_VALUE })
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
                ofType(DescribeOrganizationalUnitRequest::class),
                any<Function<DescribeOrganizationalUnitRequest, DescribeOrganizationalUnitResponse>>()
            )
        } throws exception

        every {
            proxyClient.injectCredentialsAndInvokeIterableV2(
                ofType(ListParentsRequest::class),
                any<Function<ListParentsRequest, ListParentsIterable>>()
            )
        } throws exception

        every {
            proxyClient.injectCredentialsAndInvokeIterableV2(
                ofType(ListTagsForResourceRequest::class),
                any<Function<ListTagsForResourceRequest, ListTagsForResourceIterable>>()
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
