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
import pl.wrzasq.cform.resource.aws.edgedeploy.action.ReadHandler
import pl.wrzasq.cform.resource.aws.edgedeploy.action.UpdateHandler
import pl.wrzasq.cform.resource.aws.edgedeploy.config.ResourcesFactory
import pl.wrzasq.cform.resource.aws.edgedeploy.model.ResourceModel
import pl.wrzasq.commons.json.ObjectMapperFactory
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.http.AbortableInputStream
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationResponse
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionResponse
import software.amazon.awssdk.services.lambda.model.LastUpdateStatus
import software.amazon.awssdk.services.lambda.model.PublishVersionRequest
import software.amazon.awssdk.services.lambda.model.PublishVersionResponse
import software.amazon.awssdk.services.lambda.model.ResourceNotFoundException
import software.amazon.awssdk.services.lambda.model.State
import software.amazon.awssdk.services.lambda.model.TagResourceRequest
import software.amazon.awssdk.services.lambda.model.TagResourceResponse
import software.amazon.awssdk.services.lambda.model.UntagResourceRequest
import software.amazon.awssdk.services.lambda.model.UntagResourceResponse
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeRequest
import software.amazon.awssdk.services.lambda.model.UpdateFunctionCodeResponse
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.UpdateFunctionConfigurationResponse
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
class UpdateHandlerTest {
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
        val model = ResourceModel().apply {
            name = NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } returns GetFunctionResponse.builder()
            .configuration {
                it
                    .functionName(NAME)
                    .functionArn(ARN)
                    .description(DESCRIPTION)
                    .role(ROLE_ARN)
                    .runtime(RUNTIME)
                    .handler(HANDLER)
                    .memorySize(MEMORY)
                    .timeout(TIMEOUT)
            }
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionConfigurationRequest::class),
                any<Function<UpdateFunctionConfigurationRequest, UpdateFunctionConfigurationResponse>>()
            )
        } returns UpdateFunctionConfigurationResponse.builder()
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
                ofType(UpdateFunctionCodeRequest::class),
                any<Function<UpdateFunctionCodeRequest, UpdateFunctionCodeResponse>>()
            )
        } returns UpdateFunctionCodeResponse.builder()
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(PublishVersionRequest::class),
                any<Function<PublishVersionRequest, PublishVersionResponse>>()
            )
        } returns PublishVersionResponse.builder().build()

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
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = UpdateHandler(factory, objectMapper, readHandler)
            .handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestStabilize() {
        val model = ResourceModel().apply {
            name = NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } returns GetFunctionResponse.builder()
            .configuration {
                it
                    .functionName(NAME)
                    .functionArn(ARN)
                    .description(DESCRIPTION)
                    .role(ROLE_ARN)
                    .runtime(RUNTIME)
                    .handler(HANDLER)
                    .memorySize(MEMORY)
                    .timeout(TIMEOUT)
            }
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionCodeRequest::class),
                any<Function<UpdateFunctionCodeRequest, UpdateFunctionCodeResponse>>()
            )
        } returns UpdateFunctionCodeResponse.builder()
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionConfigurationRequest::class),
                any<Function<GetFunctionConfigurationRequest, GetFunctionConfigurationResponse>>()
            )
        } returns GetFunctionConfigurationResponse.builder()
            .state(State.ACTIVE)
            .lastUpdateStatus(LastUpdateStatus.IN_PROGRESS)
            .build()

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
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = UpdateHandler(factory, objectMapper, readHandler)
            .handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.IN_PROGRESS, result.status)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestErrorOnStabilize() {
        val model = ResourceModel().apply {
            name = NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)
        val callbackContext = StdCallbackContext()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } returns GetFunctionResponse.builder()
            .configuration {
                it
                    .functionName(NAME)
                    .functionArn(ARN)
                    .description(DESCRIPTION)
                    .role(ROLE_ARN)
                    .runtime(RUNTIME)
                    .handler(HANDLER)
                    .memorySize(MEMORY)
                    .timeout(TIMEOUT)
            }
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionCodeRequest::class),
                any<Function<UpdateFunctionCodeRequest, UpdateFunctionCodeResponse>>()
            )
        } returns UpdateFunctionCodeResponse.builder()
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionConfigurationRequest::class),
                any<Function<UpdateFunctionConfigurationRequest, UpdateFunctionConfigurationResponse>>()
            )
        } returns UpdateFunctionConfigurationResponse.builder()
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionConfigurationRequest::class),
                any<Function<GetFunctionConfigurationRequest, GetFunctionConfigurationResponse>>()
            )
        } returns GetFunctionConfigurationResponse.builder()
            .state(State.FAILED)
            .build()

        every {
            proxyS3.injectCredentialsAndInvokeV2InputStream(
                ofType(GetObjectRequest::class),
                any<Function<GetObjectRequest, ResponseInputStream<GetObjectResponse>>>()
            )
        } returns ResponseInputStream(
            GetObjectResponse.builder().build(),
            AbortableInputStream.create(PACKAGE_ZIP.byteInputStream())
        )

        assertThrows<CfnNotStabilizedException> {
            UpdateHandler(factory, objectMapper, readHandler).handleRequest(proxy, request, callbackContext, logger)
        }

        verify { readHandler wasNot called }
    }

    @Test
    fun handleRequestErrorOnDescribe() {
        val model = ResourceModel().apply {
            name = NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } throws exception

        val result = UpdateHandler(factory, objectMapper, readHandler)
            .handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { readHandler wasNot called }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionConfigurationRequest::class),
                any<Function<UpdateFunctionConfigurationRequest, UpdateFunctionConfigurationResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestNotFound() {
        val model = ResourceModel().apply {
            name = NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } throws ResourceNotFoundException.builder().build()

        val result = UpdateHandler(factory, objectMapper, readHandler)
            .handleRequest(proxy, request, StdCallbackContext(), logger)

        assertEquals(OperationStatus.FAILED, result.status)

        verify { readHandler wasNot called }
        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionConfigurationRequest::class),
                any<Function<UpdateFunctionConfigurationRequest, UpdateFunctionConfigurationResponse>>()
            ) wasNot called
        }
    }

    @Test
    fun handleRequestErrorOnUpdate() {
        val model = ResourceModel().apply {
            name = NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } returns GetFunctionResponse.builder()
            .configuration {
                it
                    .functionName(NAME)
                    .functionArn(ARN)
                    .description(DESCRIPTION)
                    .role(ROLE_ARN)
                    .runtime(RUNTIME)
                    .handler(HANDLER)
                    .memorySize(MEMORY)
                    .timeout(TIMEOUT)
            }
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionCodeRequest::class),
                any<Function<UpdateFunctionCodeRequest, UpdateFunctionCodeResponse>>()
            )
        } returns UpdateFunctionCodeResponse.builder()
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
                ofType(UpdateFunctionConfigurationRequest::class),
                any<Function<UpdateFunctionConfigurationRequest, UpdateFunctionConfigurationResponse>>()
            )
        } throws exception

        every {
            proxyS3.injectCredentialsAndInvokeV2InputStream(
                ofType(GetObjectRequest::class),
                any<Function<GetObjectRequest, ResponseInputStream<GetObjectResponse>>>()
            )
        } returns ResponseInputStream(
            GetObjectResponse.builder().build(),
            AbortableInputStream.create(PACKAGE_ZIP.byteInputStream())
        )

        assertThrows<CfnGeneralServiceException> {
            UpdateHandler(factory, objectMapper, readHandler)
                .handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify { readHandler wasNot called }
    }

    @Test
    fun handleRequestChangeTags() {
        val model = ResourceModel().apply {
            name = NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
            previousResourceTags = mapOf(
                TAG_NAME to TAG_VALUE,
                "left" to "right"
            )
            desiredResourceTags = mapOf(
                TAG_NAME to TAG_VALUE,
                "another" to "some"
            )
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)
        val callbackContext = StdCallbackContext()
        val event = ProgressEvent.success(model, callbackContext)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } returns GetFunctionResponse.builder()
            .configuration {
                it
                    .functionName(NAME)
                    .functionArn(ARN)
                    .description(DESCRIPTION)
                    .role(ROLE_ARN)
                    .runtime(RUNTIME)
                    .handler(HANDLER)
                    .memorySize(MEMORY)
                    .timeout(TIMEOUT)
            }
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionConfigurationRequest::class),
                any<Function<UpdateFunctionConfigurationRequest, UpdateFunctionConfigurationResponse>>()
            )
        } returns UpdateFunctionConfigurationResponse.builder()
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
                ofType(UpdateFunctionCodeRequest::class),
                any<Function<UpdateFunctionCodeRequest, UpdateFunctionCodeResponse>>()
            )
        } returns UpdateFunctionCodeResponse.builder()
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(PublishVersionRequest::class),
                any<Function<PublishVersionRequest, PublishVersionResponse>>()
            )
        } returns PublishVersionResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            )
        } returns TagResourceResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UntagResourceRequest::class),
                any<Function<UntagResourceRequest, UntagResourceResponse>>()
            )
        } returns UntagResourceResponse.builder().build()

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
            readHandler.handleRequest(proxy, request, callbackContext, logger)
        } returns event

        val result = UpdateHandler(factory, objectMapper, readHandler)
            .handleRequest(proxy, request, callbackContext, logger)

        assertEquals(OperationStatus.SUCCESS, result.status)

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            )
        }

        verify {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UntagResourceRequest::class),
                any<Function<UntagResourceRequest, UntagResourceResponse>>()
            )
        }
    }

    @Test
    fun handleRequestErrorOnUpdateTags() {
        val model = ResourceModel().apply {
            name = NAME
        }

        val request = ResourceHandlerRequest<ResourceModel?>().apply {
            desiredResourceState = model
            previousResourceTags = mapOf(
                TAG_NAME to TAG_VALUE
            )
            desiredResourceTags = mapOf(
                TAG_NAME to TAG_VALUE,
                "another" to "some"
            )
        }

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(GetFunctionRequest::class),
                any<Function<GetFunctionRequest, GetFunctionResponse>>()
            )
        } returns GetFunctionResponse.builder()
            .configuration {
                it
                    .functionName(NAME)
                    .functionArn(ARN)
                    .description(DESCRIPTION)
                    .role(ROLE_ARN)
                    .runtime(RUNTIME)
                    .handler(HANDLER)
                    .memorySize(MEMORY)
                    .timeout(TIMEOUT)
            }
            .build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionConfigurationRequest::class),
                any<Function<UpdateFunctionConfigurationRequest, UpdateFunctionConfigurationResponse>>()
            )
        } returns UpdateFunctionConfigurationResponse.builder().build()

        every {
            proxyClient.injectCredentialsAndInvokeV2(
                ofType(UpdateFunctionCodeRequest::class),
                any<Function<UpdateFunctionCodeRequest, UpdateFunctionCodeResponse>>()
            )
        } returns UpdateFunctionCodeResponse.builder()
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
                ofType(TagResourceRequest::class),
                any<Function<TagResourceRequest, TagResourceResponse>>()
            )
        } throws exception

        assertThrows<CfnGeneralServiceException> {
            UpdateHandler(factory, objectMapper, readHandler)
                .handleRequest(proxy, request, StdCallbackContext(), logger)
        }

        verify { readHandler wasNot called }
    }

    @Test
    fun handleRequestNull() {
        val request = ResourceHandlerRequest<ResourceModel?>()

        val proxy = initializeProxy(logger, factory, proxyClient, proxyS3)

        assertThrows<IllegalArgumentException> {
            UpdateHandler(factory, objectMapper, readHandler)
                .handleRequest(proxy, request, StdCallbackContext(), logger)
        }
    }
}
