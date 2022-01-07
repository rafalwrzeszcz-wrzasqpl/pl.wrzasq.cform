/**
 * This file is part of the pl.wrzasq.cform.
 *
 * @license http://mit-license.org/ The MIT license
 * @copyright 2022 © by Rafał Wrzeszcz - Wrzasq.pl.
 */

package test.pl.wrzasq.cform.resource.aws.edgedeploy.action

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import pl.wrzasq.cform.resource.aws.edgedeploy.action.CreateHandler
import pl.wrzasq.cform.resource.aws.edgedeploy.action.ReadHandler
import pl.wrzasq.cform.resource.aws.edgedeploy.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.edgedeploy.model.ResourceModel
import pl.wrzasq.commons.json.ObjectMapperFactory
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationResponse
import software.amazon.awssdk.services.lambda.model.LastUpdateStatus
import software.amazon.awssdk.services.lambda.model.PublishVersionRequest
import software.amazon.awssdk.services.lambda.model.PublishVersionResponse
import software.amazon.awssdk.services.lambda.model.State
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException
import software.amazon.cloudformation.proxy.LoggerProxy
import software.amazon.cloudformation.proxy.OperationStatus
import software.amazon.cloudformation.proxy.ProgressEvent
import software.amazon.cloudformation.proxy.ProxyClient
import software.amazon.cloudformation.proxy.ResourceHandlerRequest
import software.amazon.cloudformation.proxy.StdCallbackContext
import java.util.function.Function

@ExtendWith(MockKExtension::class)
class CreateHandlerTest {
    private val objectMapper = ObjectMapperFactory.createObjectMapper()

    @MockK
    lateinit var logger: LoggerProxy

    @MockK
    lateinit var factory: ResourcesFactory

    @MockK
    lateinit var readHandler: ReadHandler

    @MockK
    lateinit var proxyClient: ProxyClient<LambdaClient>

    @MockK
    lateinit var proxyS3: ProxyClient<S3Client>

    @Test
    fun handleRequest() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyS3.injectCredentialsAndInvokeV2InputStream(
                ofType(GetObjectRequest::class),
                any<Function<GetObjectRequest, ResponseInputStream<GetObjectResponse>>>()
            )
        } returns ResponseInputStream(
            GetObjectResponse.builder().build(),
            AbortableInputStream.create(PACKAGE_ZIP.byteInputStream())
        )

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateFunctionRequest::class),
                any<Function<CreateFunctionRequest, CreateFunctionResponse>>()
            )
        } returns CreateFunctionResponse.builder()
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionConfigurationRequest::class),
                any<Function<GetFunctionConfigurationRequest, GetFunctionConfigurationResponse>>()
            )
        } returns GetFunctionConfigurationResponse.builder()
            .state(State.ACTIVE)
            .lastUpdateStatus(LastUpdateStatus.SUCCESSFUL)
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(PublishVersionRequest::class),
                any<Function<PublishVersionRequest, PublishVersionResponse>>()
            )
        } returns PublishVersionResponse.builder().build()

        every {
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = CreateHandler(factory, objectMapper, readHandler)
            .handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)
    }

    @Test
    fun handleRequestStabilize() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)
        val callbackContext = StdCallbackContext()

        every {
            proxyS3.injectCredentialsAndInvokeV2InputStream(
                ofType(GetObjectRequest::class),
                any<Function<GetObjectRequest, ResponseInputStream<GetObjectResponse>>>()
            )
        } returns ResponseInputStream(
            GetObjectResponse.builder().build(),
            AbortableInputStream.create(PACKAGE_ZIP.byteInputStream())
        )

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateFunctionRequest::class),
                any<Function<CreateFunctionRequest, CreateFunctionResponse>>()
            )
        } returns CreateFunctionResponse.builder()
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionConfigurationRequest::class),
                any<Function<GetFunctionConfigurationRequest, GetFunctionConfigurationResponse>>()
            )
        } returns GetFunctionConfigurationResponse.builder()
            .state(State.PENDING)
            .build()

        val result = CreateHandler(factory, objectMapper, readHandler)
            .handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.IN_PROGRESS, result.status)

        verify { readHandler wasNot called }
    }

    @Test
    fun handleRequestErrorOnStabilize() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)
        val callbackContext = StdCallbackContext()

        every {
            proxyS3.injectCredentialsAndInvokeV2InputStream(
                ofType(GetObjectRequest::class),
                any<Function<GetObjectRequest, ResponseInputStream<GetObjectResponse>>>()
            )
        } returns ResponseInputStream(
            GetObjectResponse.builder().build(),
            AbortableInputStream.create(PACKAGE_ZIP.byteInputStream())
        )

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateFunctionRequest::class),
                any<Function<CreateFunctionRequest, CreateFunctionResponse>>()
            )
        } returns CreateFunctionResponse.builder()
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionConfigurationRequest::class),
                any<Function<GetFunctionConfigurationRequest, GetFunctionConfigurationResponse>>()
            )
        } returns GetFunctionConfigurationResponse.builder()
            .state(State.FAILED)
            .build()

        assertThrows<CfnNotStabilizedException> {
            CreateHandler(factory, objectMapper, readHandler).handleRequest(proxy, request, callbackContext, logger)
        }

        verify { readHandler wasNot called }
    }

    @Test
    fun handleRequestError() {
        val model = ResourceModel()

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)

        every {
            proxyS3.injectCredentialsAndInvokeV2InputStream(
                ofType(GetObjectRequest::class),
                any<Function<GetObjectRequest, ResponseInputStream<GetObjectResponse>>>()
            )
        } returns ResponseInputStream(
            GetObjectResponse.builder().build(),
            AbortableInputStream.create(PACKAGE_ZIP.byteInputStream())
        )

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(CreateFunctionRequest::class),
                any<Function<CreateFunctionRequest, CreateFunctionResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            CreateHandler(factory, objectMapper, readHandler)
                .handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify { readHandler wasNot called }
    }

    @Test
    fun handleRequestNull() {
        val request = ResourceHandlerRequest<ResourceModel?>()

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)

        assertThrows<IllegalArgumentException> {
            CreateHandler(factory, objectMapper, readHandler)
                .handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }
}
