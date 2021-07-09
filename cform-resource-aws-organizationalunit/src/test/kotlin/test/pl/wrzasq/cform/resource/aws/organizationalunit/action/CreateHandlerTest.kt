/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2021 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.organizationalunit.action

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.organizationalunit.action.CreateHandler
import pl.wrzasq.cform.resource.aws.organizationalunit.action.ReadHandler
import pl.wrzasq.cform.resource.aws.organizationalunit.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.organizationalunit.model.ResourceModel
import software.amazon.awssdk.services.organizations.OrganizationsClient
import software.amazon.awssdk.services.organizations.model.CreateOrganizationRequest
import software.amazon.awssdk.services.organizations.model.CreateOrganizationResponse
import software.amazon.awssdk.services.organizations.model.CreateOrganizationalUnitRequest
import software.amazon.awssdk.services.organizations.model.CreateOrganizationalUnitResponse
import software.amazon.awssdk.services.organizations.model.DuplicateOrganizationalUnitException
import software.amazon.awssdk.services.organizations.model.OrganizationalUnit
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
            arn = ARN
            name = NAME
            parentId = PARENT_ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
            desiredResourceTags = emptyMap()
        }

        val proxy = initializeProxy(logger, factory, proxyClient)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateOrganizationalUnitRequest::class),
                any<Function<CreateOrganizationalUnitRequest, CreateOrganizationalUnitResponse>>()
            )
        } returns CreateOrganizationalUnitResponse.builder()
            .organizationalUnit(
                OrganizationalUnit.builder()
                    .id(ID)
                    .build()
            )
            .build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, readHandler).handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
        assertEquals(ID, request.desiredResourceState?.id)
    }

    @Test
    fun handleRequestExisting() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
            desiredResourceTags = emptyMap()
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateOrganizationalUnitRequest::class),
                any<Function<CreateOrganizationalUnitRequest, CreateOrganizationalUnitResponse>>()
            )
        } throws DuplicateOrganizationalUnitException.builder().build()

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
    fun handleRequestErrorOnCreate() {
        val model = ResourceModel().apply {
            id = ID
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
            desiredResourceTags = emptyMap()
        }

        val proxy = initializeProxy(logger, factory, proxyClient)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateOrganizationalUnitRequest::class),
                any<Function<CreateOrganizationalUnitRequest, CreateOrganizationalUnitResponse>>()
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
