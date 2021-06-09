/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.organization.action

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.organization.action.DeleteHandler
import pl.wrzasq.cform.resource.aws.organization.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.organization.model.ResourceModel
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.awssdk.services.organizations.model.DeleteOrganizationRequest
import software.amazon.awssdk.services.organizations.model.DeleteOrganizationResponse
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationRequest
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationResponse
import software.amazon.awssdk.services.organizations.model.Organization
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnResourceConflictException
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

@ExtendWith(MockKExtension::class)
class DeleteHandlerTest {
    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var factory: ResourcesFactory

    @MockK
    lateinit var proxyClient: ProxyClient<OrganizationsClient>

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
                ofType(DeleteOrganizationRequest::class),
                any<Function<DeleteOrganizationRequest, DeleteOrganizationResponse>>()
            )
        } returns DeleteOrganizationResponse.builder()
            .build()

        val result = DeleteHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)

        Assertions.assertEquals(OperationStatus.SUCCESS, result.status)
        Assertions.assertNull(result.resourceModel)
    }

    @Test
    fun handleRequestConflict() {
        val model = ResourceModel().apply {
            id = "${ID}different"
        }

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

        assertThrows<CfnResourceConflictException> {
            DeleteHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DeleteOrganizationRequest::class),
                any<Function<DeleteOrganizationRequest, DeleteOrganizationResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestFailed() {
        val model = ResourceModel().apply {
            id = ID
        }

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
                ofType(DeleteOrganizationRequest::class),
                any<Function<DeleteOrganizationRequest, DeleteOrganizationResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            DeleteHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }

    @Test
    fun handleRequestNull() {
        val request = ResourceHandlerRequest<ResourceModel?>()

        val proxy = initializeProxy(logger, factory, proxyClient)

        assertThrows<IllegalArgumentException> {
            DeleteHandler(factory).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }
}
