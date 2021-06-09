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
import pl.wrzasq.cform.resource.aws.organization.action.CreateHandler
import pl.wrzasq.cform.resource.aws.organization.action.ReadHandler
import pl.wrzasq.cform.resource.aws.organization.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.organization.model.ResourceModel
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.awssdk.services.organizations.model.AlreadyInOrganizationException
import software.amazon.awssdk.services.organizations.model.AwsOrganizationsNotInUseException
import software.amazon.awssdk.services.organizations.model.CreateOrganizationRequest
import software.amazon.awssdk.services.organizations.model.CreateOrganizationResponse
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationRequest
import software.amazon.awssdk.services.organizations.model.DescribeOrganizationResponse
import software.amazon.awssdk.services.organizations.model.Organization
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException
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
    lateinit var proxyClient: ProxyClient<OrganizationsClient>

    @Test
    fun handleRequest() {
        val model = ResourceModel().apply {
            id = ID
            arn = ARN
            rootId = ROOT_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeOrganizationRequest::class),
                any<Function<DescribeOrganizationRequest, DescribeOrganizationResponse>>()
            )
        } throws AwsOrganizationsNotInUseException.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateOrganizationRequest::class),
                any<Function<CreateOrganizationRequest, CreateOrganizationResponse>>()
            )
        } returns CreateOrganizationResponse.builder().build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        Assertions.assertEquals(OperationStatus.SUCCESS, result.status)
        Assertions.assertEquals(ID, result.resourceModel?.id)
        Assertions.assertEquals(ARN, result.resourceModel?.arn)
        Assertions.assertEquals(ROOT_ID, result.resourceModel?.rootId)
    }

    @Test
    fun handleRequestExisting() {
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

        assertThrows<CfnAlreadyExistsException> {
            CreateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify { readHandler wasNot called }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateOrganizationRequest::class),
                any<Function<CreateOrganizationRequest, CreateOrganizationResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestErrorOnDescribe() {
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
        } throws exception

        val result = CreateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)

        verify { readHandler wasNot called }

        Assertions.assertEquals(OperationStatus.FAILED, result.status)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateOrganizationRequest::class),
                any<Function<CreateOrganizationRequest, CreateOrganizationResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestAlreadyInOrganization() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(DescribeOrganizationRequest::class),
                any<Function<DescribeOrganizationRequest, DescribeOrganizationResponse>>()
            )
        } throws AwsOrganizationsNotInUseException.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateOrganizationRequest::class),
                any<Function<CreateOrganizationRequest, CreateOrganizationResponse>>()
            )
        } throws AlreadyInOrganizationException.builder().build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        Assertions.assertSame(event, result)
    }

    @Test
    fun handleRequestErrorOnCreate() {
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
        } throws AwsOrganizationsNotInUseException.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateOrganizationRequest::class),
                any<Function<CreateOrganizationRequest, CreateOrganizationResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            CreateHandler(factory, readHandler).handleRequest(proxy, request, StdCallbackContext(), logger)
        }
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
